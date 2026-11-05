-- =====================================================================
-- Cashin v6 (Supabase) — 004_desafios_automaticos.sql   (Sprint 4)
-- Los desafíos llegan solos: cada lunes 3 semanales y cada día 1 un mensual, elegidos por la app
-- de un catálogo (12 semanales + 5 mensuales) con un sorteo propio de cada cuenta.
-- - Varios desafíos activos a la vez (pero nunca el mismo reto dos veces en un periodo).
-- - Reglas nuevas que no necesitan historia: CANTIDAD_REGISTROS, DIAS_SIN_GASTO, AHORRO_META.
-- - asignar_desafios(): guarda los del periodo UNA sola vez por cuenta (no se re-sortean al volver a entrar).
-- Ejecutar DESPUÉS de 001, 002 y 003. Se puede volver a ejecutar sin romper nada.
-- =====================================================================

-- 1. Columnas nuevas ---------------------------------------------------------------
-- automatico: lo asignó el sistema (no lo aceptó la persona) · plantilla: qué reto del catálogo es.
alter table public.desafio add column if not exists automatico boolean not null default false;
alter table public.desafio add column if not exists plantilla  varchar(30);

-- 2. Reglas nuevas ------------------------------------------------------------------
-- Se reemplazan las validaciones de 001 que nombran a "regla" (sus nombres los puso PostgreSQL).
do $$
declare c record;
begin
  for c in select conname from pg_constraint
            where conrelid = 'public.desafio'::regclass and contype = 'c'
              and pg_get_constraintdef(oid) ilike '%regla%' loop
    execute format('alter table public.desafio drop constraint %I', c.conname);
  end loop;
end $$;

-- dias_objetivo pasa a ser "el objetivo" de las reglas que cuentan algo (días, movimientos o ahorros).
alter table public.desafio add constraint desafio_regla_check check (regla in (
  'LIMITE_CATEGORIA','LIMITE_TOTAL','DIAS_REGISTRO','CANTIDAD_REGISTROS','DIAS_SIN_GASTO','AHORRO_META'));
alter table public.desafio add constraint desafio_campos_regla_check check (
  (regla = 'LIMITE_CATEGORIA' and categoria_id is not null and monto_limite is not null) or
  (regla = 'LIMITE_TOTAL'     and monto_limite is not null) or
  (regla in ('DIAS_REGISTRO','CANTIDAD_REGISTROS','DIAS_SIN_GASTO','AHORRO_META') and dias_objetivo is not null));

-- Nunca el mismo reto automático dos veces en el mismo periodo.
create unique index if not exists ux_desafio_automatico on public.desafio (
  usuario_id, periodo, fecha_inicio, regla,
  coalesce(categoria_id, '00000000-0000-0000-0000-000000000000'::uuid)) where automatico;

-- 3. Medición con las reglas nuevas ------------------------------------------------
-- Cambia lo que devuelve, así que se borra y se vuelve a crear (con progreso_desafios, que la usa).
drop function if exists public.progreso_desafios();
drop function if exists public.fn_medir_desafio(public.desafio);

-- gastado: gasto del periodo (de su categoría si es LIMITE_CATEGORIA)
-- dias_con_registro / cantidad_registros: por creado_en (no se puede "anotar en el pasado")
-- dias_sin_gasto: días YA TERMINADOS del periodo sin ningún gasto · ahorros: ahorros anotados en metas
create function public.fn_medir_desafio(d public.desafio)
returns table (gastado numeric, dias_con_registro int, cantidad_registros int, dias_sin_gasto int, ahorros int)
language sql stable security definer set search_path = public as $$
  with hoy as (select (now() at time zone 'America/La_Paz')::date as dia_hoy)
  select
    coalesce((select sum(r.monto) from public.registro r
               where r.usuario_id = d.usuario_id
                 and r.tipo = 'GASTO'
                 and (r.fecha_hora at time zone 'America/La_Paz')::date between d.fecha_inicio and d.fecha_fin
                 and (d.regla <> 'LIMITE_CATEGORIA' or r.categoria_id = d.categoria_id)), 0),
    (select count(distinct (r.creado_en at time zone 'America/La_Paz')::date)::int from public.registro r
      where r.usuario_id = d.usuario_id
        and (r.creado_en at time zone 'America/La_Paz')::date between d.fecha_inicio and d.fecha_fin),
    (select count(*)::int from public.registro r
      where r.usuario_id = d.usuario_id
        and (r.creado_en at time zone 'America/La_Paz')::date between d.fecha_inicio and d.fecha_fin),
    (select count(*)::int
       from generate_series(d.fecha_inicio, least(d.fecha_fin, (select dia_hoy from hoy) - 1), interval '1 day') g(dia)
      where not exists (select 1 from public.registro r
                         where r.usuario_id = d.usuario_id and r.tipo = 'GASTO'
                           and (r.fecha_hora at time zone 'America/La_Paz')::date = g.dia::date)),
    (select count(*)::int from public.aporte_meta a join public.meta m on m.id = a.meta_id
      where m.usuario_id = d.usuario_id and a.tipo = 'AHORRO'
        and a.fecha between d.fecha_inicio and d.fecha_fin);
$$;

create function public.progreso_desafios()
returns table (desafio_id uuid, gastado numeric, dias_con_registro int,
               cantidad_registros int, dias_sin_gasto int, ahorros int)
language sql stable security definer set search_path = public as $$
  select d.id, m.gastado, m.dias_con_registro, m.cantidad_registros, m.dias_sin_gasto, m.ahorros
    from public.desafio d
    cross join lateral public.fn_medir_desafio(d) m
   where d.usuario_id = auth.uid() and d.estado = 'ACTIVO';
$$;

-- 4. Evaluación con las reglas nuevas ----------------------------------------------
-- LIMITE_*: si ya se pasó → FALLIDO; si terminó el periodo sin pasarse → CUMPLIDO.
-- Las que cuentan algo: si llegó al objetivo → CUMPLIDO; si terminó el periodo sin llegar → FALLIDO.
create or replace function public.evaluar_desafios()
returns setof public.desafio
language plpgsql security definer set search_path = public as $$
declare
  v_uid uuid := auth.uid();
  v_hoy date := (now() at time zone 'America/La_Paz')::date;
  d public.desafio;
  m record;
  v_logrado int;
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
      v_logrado := case d.regla
                     when 'DIAS_REGISTRO'      then m.dias_con_registro
                     when 'CANTIDAD_REGISTROS' then m.cantidad_registros
                     when 'DIAS_SIN_GASTO'     then m.dias_sin_gasto
                     when 'AHORRO_META'        then m.ahorros
                   end;
      if v_logrado >= d.dias_objetivo then v_estado := 'CUMPLIDO';
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

-- 5. Asignación automática ---------------------------------------------------------
-- La app elige los retos del periodo (3 semanales o 1 mensual) y los manda aquí.
-- Solo se guardan si en este periodo todavía NO hubo asignación automática: así, aunque la persona
-- salga y vuelva a entrar o use otro celular, sus desafíos de la semana/mes no cambian.
-- p_desafios: [{titulo, descripcion, regla, categoria_id, monto_limite, objetivo, plantilla}, ...]
-- Devuelve los desafíos nuevos (vacío si ya estaban asignados).
create or replace function public.asignar_desafios(p_periodo text, p_desafios jsonb)
returns setof public.desafio
language plpgsql security definer set search_path = public as $$
declare
  v_uid    uuid := auth.uid();
  v_rango  record;
  v_maximo int;
  v_minimo numeric;
  x        jsonb;
  v_regla  text;
  v_nuevo  public.desafio;
begin
  if v_uid is null then raise exception 'Sesión requerida'; end if;
  if p_periodo not in ('SEMANAL','MENSUAL') then raise exception 'Periodo inválido'; end if;

  v_maximo := case p_periodo when 'SEMANAL' then 3 else 1 end;
  v_minimo := case p_periodo when 'SEMANAL' then 50 else 200 end;   -- límites realistas
  if jsonb_typeof(p_desafios) <> 'array' or jsonb_array_length(p_desafios) > v_maximo then
    raise exception 'Se pueden asignar como máximo % desafíos %', v_maximo, lower(p_periodo);
  end if;

  -- Dos celulares a la vez no deben asignar dos veces.
  perform pg_advisory_xact_lock(hashtext(v_uid::text || p_periodo));

  select * into v_rango from public.fn_rango_periodo(p_periodo);
  if exists (select 1 from public.desafio
              where usuario_id = v_uid and periodo = p_periodo
                and fecha_inicio = v_rango.fecha_inicio and automatico) then
    return;   -- ya se asignaron los de este periodo
  end if;

  for x in select value from jsonb_array_elements(p_desafios) loop
    v_regla := x->>'regla';

    if v_regla in ('LIMITE_CATEGORIA','LIMITE_TOTAL')
       and coalesce((x->>'monto_limite')::numeric, 0) < v_minimo then
      raise exception 'El límite % debe ser de al menos Bs %', lower(p_periodo), v_minimo;
    end if;
    if v_regla = 'LIMITE_CATEGORIA' and not exists (
         select 1 from public.categoria c
          where c.id = (x->>'categoria_id')::uuid and c.tipo = 'GASTO'
            and (c.usuario_id is null or c.usuario_id = v_uid)) then
      raise exception 'Categoría inválida para el desafío';
    end if;

    insert into public.desafio (usuario_id, titulo, descripcion, periodo, regla, categoria_id,
                                monto_limite, dias_objetivo, fecha_inicio, fecha_fin, xp_recompensa,
                                automatico, plantilla)
    values (v_uid, left(x->>'titulo', 80), left(x->>'descripcion', 300), p_periodo, v_regla,
            case when v_regla = 'LIMITE_CATEGORIA' then (x->>'categoria_id')::uuid end,
            case when v_regla in ('LIMITE_CATEGORIA','LIMITE_TOTAL') then (x->>'monto_limite')::numeric end,
            case when v_regla not in ('LIMITE_CATEGORIA','LIMITE_TOTAL') then (x->>'objetivo')::int end,
            v_rango.fecha_inicio, v_rango.fecha_fin,
            case p_periodo when 'SEMANAL' then 50 else 150 end,
            true, left(x->>'plantilla', 30))
    on conflict do nothing
    returning * into v_nuevo;

    if found then return next v_nuevo; end if;
  end loop;
end; $$;

-- 6. Permisos ----------------------------------------------------------------------
revoke all on function public.fn_medir_desafio(public.desafio)     from public, anon, authenticated;
revoke all on function public.progreso_desafios()                  from public, anon;
revoke all on function public.evaluar_desafios()                   from public, anon;
revoke all on function public.asignar_desafios(text, jsonb)        from public, anon;
grant execute on function public.progreso_desafios()               to authenticated;
grant execute on function public.evaluar_desafios()                to authenticated;
grant execute on function public.asignar_desafios(text, jsonb)     to authenticated;
