-- =====================================================================================
-- Cashin · 010 · Perfil más completo (HU-05)
-- 1. Día de cobro (opcional): qué día del mes le pagan a la persona.
-- 2. estadisticas_perfil(): los números del perfil, contados en el servidor con el RLS
--    de quien consulta (security invoker): cada uno ve solo lo suyo.
-- Ejecutar después de 001 a 009. No cambia las migraciones anteriores.
-- =====================================================================================

-- 1. Día de cobro ----------------------------------------------------------------------
alter table public.perfil add column if not exists dia_cobro smallint;
do $$ begin
  alter table public.perfil add constraint perfil_dia_cobro_valido check (dia_cobro between 1 and 31);
exception when duplicate_object then null; end $$;

-- La app solo puede cambiar estas columnas (el XP y la racha los maneja la base de datos).
revoke update on public.perfil from anon, authenticated;
grant update (nombre, moneda, avatar, dia_cobro) on public.perfil to authenticated;

-- 2. Estadísticas del perfil -------------------------------------------------------------
create or replace function public.estadisticas_perfil()
returns table (
    dias_usando        int,
    anotaciones        int,
    mejor_racha        int,
    desafios_cumplidos int,
    metas_logradas     int,
    deudas_saldadas    int,
    total_ahorrado     numeric
)
language sql stable security invoker set search_path = public as $$
  select
    ((now() at time zone 'America/La_Paz')::date
       - (p.creado_en at time zone 'America/La_Paz')::date + 1)::int,
    (select count(*) from public.registro r where r.usuario_id = p.id)::int,
    p.racha_maxima,
    (select count(*) from public.desafio d where d.usuario_id = p.id and d.estado = 'CUMPLIDO')::int,
    (select count(*) from public.meta m where m.usuario_id = p.id and m.estado = 'COMPLETADA')::int,
    (select count(*) from public.deuda de where de.usuario_id = p.id and de.estado = 'PAGADA')::int,
    coalesce((select sum(a.total_ahorrado) from public.v_avance_meta a
               where a.usuario_id = p.id and a.estado <> 'CANCELADA'), 0)
  from public.perfil p
  where p.id = auth.uid();
$$;

revoke execute on function public.estadisticas_perfil() from public, anon;
grant execute on function public.estadisticas_perfil() to authenticated;
