-- =====================================================================
-- Cashin v6 (Supabase) — 003_gamificacion.sql   (Sprint 4)
-- Desafíos (aceptar, descartar, medir, evaluar) y XP por completar metas.
-- Ejecutar DESPUÉS de 001 y 002. Todas las funciones actúan solo sobre auth.uid().
-- =====================================================================

-- Rango [inicio, fin] del periodo actual en hora de Bolivia.
-- SEMANAL: lunes a domingo de esta semana · MENSUAL: primer a último día del mes.
create or replace function public.fn_rango_periodo(p_periodo text)
returns table (fecha_inicio date, fecha_fin date)
language sql stable set search_path = public as $$
  select case p_periodo
           when 'SEMANAL' then date_trunc('week',  (now() at time zone 'America/La_Paz'))::date
           else                date_trunc('month', (now() at time zone 'America/La_Paz'))::date
         end,
         case p_periodo
           when 'SEMANAL' then (date_trunc('week',  (now() at time zone 'America/La_Paz')) + interval '6 days')::date
           else                (date_trunc('month', (now() at time zone 'America/La_Paz')) + interval '1 month - 1 day')::date
         end;
$$;

-- Mide un desafío: cuánto se gastó en su periodo (según su regla) y en cuántos días distintos se anotó.
create or replace function public.fn_medir_desafio(d public.desafio)
returns table (gastado numeric, dias_con_registro int)
language sql stable security definer set search_path = public as $$
  select
    coalesce((select sum(r.monto) from public.registro r
               where r.usuario_id = d.usuario_id
                 and r.tipo = 'GASTO'
                 and (r.fecha_hora at time zone 'America/La_Paz')::date between d.fecha_inicio and d.fecha_fin
                 and (d.regla <> 'LIMITE_CATEGORIA' or r.categoria_id = d.categoria_id)), 0),
    (select count(distinct (r.creado_en at time zone 'America/La_Paz')::date)::int from public.registro r
      where r.usuario_id = d.usuario_id
        and (r.creado_en at time zone 'America/La_Paz')::date between d.fecha_inicio and d.fecha_fin);
$$;

-- HU-22: acepta una de las propuestas. Fechas y XP los pone el servidor, no la app.
create or replace function public.aceptar_desafio(
    p_titulo text, p_descripcion text, p_periodo text, p_regla text,
    p_categoria_id uuid default null, p_monto_limite numeric default null, p_dias_objetivo int default null)
returns public.desafio
language plpgsql security definer set search_path = public as $$
declare
  v_uid uuid := auth.uid();
  v_rango record;
  v_nuevo public.desafio;
begin
  if v_uid is null then raise exception 'Sesión requerida'; end if;
  if p_periodo not in ('SEMANAL','MENSUAL') then raise exception 'Periodo inválido'; end if;

  if exists (select 1 from public.desafio
              where usuario_id = v_uid and periodo = p_periodo and estado = 'ACTIVO') then
    raise exception 'Ya tienes un desafío % activo', lower(p_periodo);
  end if;

  if p_regla = 'LIMITE_CATEGORIA' and not exists (
       select 1 from public.categoria c
        where c.id = p_categoria_id and c.tipo = 'GASTO'
          and (c.usuario_id is null or c.usuario_id = v_uid)) then
    raise exception 'Categoría inválida para el desafío';
  end if;

  select * into v_rango from public.fn_rango_periodo(p_periodo);

  insert into public.desafio (usuario_id, titulo, descripcion, periodo, regla, categoria_id,
                              monto_limite, dias_objetivo, fecha_inicio, fecha_fin, xp_recompensa)
  values (v_uid, left(p_titulo, 80), left(p_descripcion, 300), p_periodo, p_regla,
          case when p_regla = 'LIMITE_CATEGORIA' then p_categoria_id end,
          case when p_regla in ('LIMITE_CATEGORIA','LIMITE_TOTAL') then p_monto_limite end,
          case when p_regla = 'DIAS_REGISTRO' then p_dias_objetivo end,
          v_rango.fecha_inicio, v_rango.fecha_fin,
          case p_periodo when 'SEMANAL' then 50 else 150 end)
  returning * into v_nuevo;
  return v_nuevo;
end; $$;

-- Descarta un desafío activo propio. Devuelve true si se descartó.
create or replace function public.descartar_desafio(p_id uuid)
returns boolean
language plpgsql security definer set search_path = public as $$
begin
  update public.desafio set estado = 'DESCARTADO'
   where id = p_id and usuario_id = auth.uid() and estado = 'ACTIVO';
  return found;
end; $$;

-- Progreso de los desafíos ACTIVOS del usuario (para las barras de la app).
create or replace function public.progreso_desafios()
returns table (desafio_id uuid, gastado numeric, dias_con_registro int)
language sql stable security definer set search_path = public as $$
  select d.id, m.gastado, m.dias_con_registro
    from public.desafio d
    cross join lateral public.fn_medir_desafio(d) m
   where d.usuario_id = auth.uid() and d.estado = 'ACTIVO';
$$;

-- HU-23: evalúa los desafíos activos y devuelve los que cambiaron de estado.
-- LIMITE_*: si ya se pasó del límite → FALLIDO; si terminó el periodo sin pasarse → CUMPLIDO.
-- DIAS_REGISTRO: si llegó a los días → CUMPLIDO; si terminó el periodo sin llegar → FALLIDO.
-- Al pasar a CUMPLIDO suma xp_recompensa al perfil (una sola vez: el estado deja de ser ACTIVO).
create or replace function public.evaluar_desafios()
returns setof public.desafio
language plpgsql security definer set search_path = public as $$
declare
  v_uid uuid := auth.uid();
  v_hoy date := (now() at time zone 'America/La_Paz')::date;
  d public.desafio;
  m record;
  v_estado text;
begin
  if v_uid is null then raise exception 'Sesión requerida'; end if;

  for d in select * from public.desafio
            where usuario_id = v_uid and estado = 'ACTIVO' for update loop
    select * into m from public.fn_medir_desafio(d);
    v_estado := null;

    if d.regla in ('LIMITE_CATEGORIA','LIMITE_TOTAL') then
      if m.gastado > d.monto_limite then v_estado := 'FALLIDO';
      elsif v_hoy > d.fecha_fin then v_estado := 'CUMPLIDO';
      end if;
    else
      if m.dias_con_registro >= d.dias_objetivo then v_estado := 'CUMPLIDO';
      elsif v_hoy > d.fecha_fin then v_estado := 'FALLIDO';
      end if;
    end if;

    if v_estado is not null then
      update public.desafio set estado = v_estado where id = d.id returning * into d;
      if v_estado = 'CUMPLIDO' then
        update public.perfil set xp = xp + d.xp_recompensa where id = v_uid;
      end if;
      return next d;
    end if;
  end loop;
end; $$;

-- Marca una meta como COMPLETADA y da +100 XP, solo si ya llegó al objetivo y seguía ACTIVA.
create or replace function public.completar_meta(p_meta_id uuid)
returns boolean
language plpgsql security definer set search_path = public as $$
declare
  v_uid uuid := auth.uid();
  v_objetivo numeric;
  v_ahorrado numeric;
begin
  select m.monto_objetivo into v_objetivo from public.meta m
   where m.id = p_meta_id and m.usuario_id = v_uid and m.estado = 'ACTIVA' for update;
  if not found then return false; end if;

  select coalesce(sum(case when tipo = 'AHORRO' then monto else -monto end), 0) into v_ahorrado
    from public.aporte_meta where meta_id = p_meta_id;
  if v_ahorrado < v_objetivo then return false; end if;

  update public.meta set estado = 'COMPLETADA' where id = p_meta_id;
  update public.perfil set xp = xp + 100 where id = v_uid;
  return true;
end; $$;

-- Permisos: solo usuarios con sesión. Las funciones internas no se exponen.
revoke all on function public.fn_rango_periodo(text)                                        from public, anon, authenticated;
revoke all on function public.fn_medir_desafio(public.desafio)                              from public, anon, authenticated;
revoke all on function public.aceptar_desafio(text, text, text, text, uuid, numeric, int)   from public, anon;
revoke all on function public.descartar_desafio(uuid)                                      from public, anon;
revoke all on function public.progreso_desafios()                                          from public, anon;
revoke all on function public.evaluar_desafios()                                           from public, anon;
revoke all on function public.completar_meta(uuid)                                         from public, anon;
grant execute on function public.aceptar_desafio(text, text, text, text, uuid, numeric, int) to authenticated;
grant execute on function public.descartar_desafio(uuid)                                    to authenticated;
grant execute on function public.progreso_desafios()                                        to authenticated;
grant execute on function public.evaluar_desafios()                                         to authenticated;
grant execute on function public.completar_meta(uuid)                                       to authenticated;
