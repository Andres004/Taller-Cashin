-- =====================================================================
-- Cashin v6 (Supabase) — 006_desafios_sin_fijos.sql
-- Los retos de límite solo miden lo que se puede recortar: no cuentan los pagos planificados
-- ni las categorías que la persona tenía con importancia 4 o 5 cuando se le asignó el reto.
-- Los desafíos que ya estaban activos terminan con la regla con la que nacieron.
-- Ejecutar DESPUÉS de 005. Se puede volver a ejecutar sin romper nada.
-- =====================================================================

do $$
begin
  if to_regclass('public.categoria_importancia') is null then
    raise exception 'Primero hay que ejecutar 005_importancia_categorias.sql';
  end if;
end $$;

-- 1. Foto de las categorías excluidas ------------------------------------------------
-- NULL = regla anterior (cuenta todo). Se guarda al asignar para que subir una categoría
-- a importancia 5 a mitad de semana no sirva para ganar el reto.
alter table public.desafio add column if not exists categorias_excluidas uuid[];

-- 2. Medición -------------------------------------------------------------------------
-- Igual que en 004; solo cambia "gastado". dias_sin_gasto sigue contando cualquier gasto.
create or replace function public.fn_medir_desafio(d public.desafio)
returns table (gastado numeric, dias_con_registro int, cantidad_registros int, dias_sin_gasto int, ahorros int)
language sql stable security definer set search_path = public as $$
  with hoy as (select (now() at time zone 'America/La_Paz')::date as dia_hoy)
  select
    coalesce((select sum(r.monto) from public.registro r
               where r.usuario_id = d.usuario_id
                 and r.tipo = 'GASTO'
                 and (r.fecha_hora at time zone 'America/La_Paz')::date between d.fecha_inicio and d.fecha_fin
                 and (d.regla <> 'LIMITE_CATEGORIA' or r.categoria_id = d.categoria_id)
                 and (d.categorias_excluidas is null
                      or (r.planificado_id is null and r.categoria_id <> all (d.categorias_excluidas)))), 0),
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

-- 3. Asignación -----------------------------------------------------------------------
-- Igual que en 004; además guarda la foto en los retos de límite (la calcula el servidor).
create or replace function public.asignar_desafios(p_periodo text, p_desafios jsonb)
returns setof public.desafio
language plpgsql security definer set search_path = public as $$
declare
  v_uid       uuid := auth.uid();
  v_rango     record;
  v_maximo    int;
  v_minimo    numeric;
  x           jsonb;
  v_regla     text;
  v_excluidas uuid[];
  v_nuevo     public.desafio;
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

  select coalesce(array_agg(c.id), '{}') into v_excluidas
    from public.categoria c
    left join public.categoria_importancia ci on ci.categoria_id = c.id and ci.usuario_id = v_uid
   where c.tipo = 'GASTO'
     and (c.usuario_id is null or c.usuario_id = v_uid)
     and coalesce(ci.importancia, c.importancia) >= 4;

  for x in select value from jsonb_array_elements(p_desafios) loop
    v_regla := x->>'regla';

    if v_regla in ('LIMITE_CATEGORIA','LIMITE_TOTAL')
       and coalesce((x->>'monto_limite')::numeric, 0) < v_minimo then
      raise exception 'El límite % debe ser de al menos Bs %', lower(p_periodo), v_minimo;
    end if;
    if v_regla = 'LIMITE_CATEGORIA' and (
         not exists (select 1 from public.categoria c
                      where c.id = (x->>'categoria_id')::uuid and c.tipo = 'GASTO'
                        and (c.usuario_id is null or c.usuario_id = v_uid))
         or (x->>'categoria_id')::uuid = any (v_excluidas)) then
      raise exception 'Categoría inválida para el desafío';
    end if;

    insert into public.desafio (usuario_id, titulo, descripcion, periodo, regla, categoria_id,
                                monto_limite, dias_objetivo, fecha_inicio, fecha_fin, xp_recompensa,
                                automatico, plantilla, categorias_excluidas)
    values (v_uid, left(x->>'titulo', 80), left(x->>'descripcion', 300), p_periodo, v_regla,
            case when v_regla = 'LIMITE_CATEGORIA' then (x->>'categoria_id')::uuid end,
            case when v_regla in ('LIMITE_CATEGORIA','LIMITE_TOTAL') then (x->>'monto_limite')::numeric end,
            case when v_regla not in ('LIMITE_CATEGORIA','LIMITE_TOTAL') then (x->>'objetivo')::int end,
            v_rango.fecha_inicio, v_rango.fecha_fin,
            case p_periodo when 'SEMANAL' then 50 else 150 end,
            true, left(x->>'plantilla', 30),
            case when v_regla in ('LIMITE_CATEGORIA','LIMITE_TOTAL') then v_excluidas end)
    on conflict do nothing
    returning * into v_nuevo;

    if found then return next v_nuevo; end if;
  end loop;
end; $$;
