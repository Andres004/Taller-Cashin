-- =====================================================================================
-- Cashin · 007 · Camino fijo de cada meta
-- El camino (montos y fechas de cada paso) se arma una sola vez y se guarda con la meta.
-- Se vuelve a estimar solo cuando se anota un ahorro o un retiro: con fecha límite cambian los montos,
-- sin fecha cambian los montos y la cantidad de pasos. Los pasos completados no cambian.
-- La política meta_dueno (002) ya deja a cada usuario leer y actualizar solo sus metas.
-- =====================================================================================

alter table public.meta add column if not exists camino jsonb;

comment on column public.meta.camino is
  'Camino de la meta: pasos [{fecha, monto}], ahorrado_inicial, ahorrado_al_estimar, objetivo, fecha_limite, '
  'estado, cadencia, meses_por_paso y ahorro_por_paso. Solo cambia cuando se anota un ahorro o un retiro.';
