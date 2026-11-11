-- =====================================================================================
-- Cashin · 007 · Camino fijo de cada meta
-- El camino (montos y fechas de cada paso) se arma una sola vez y se guarda con la meta.
-- Los ahorros lo van llenando en orden; ya no se recalcula cada vez que se abre la meta.
-- La política meta_dueno (002) ya deja a cada usuario leer y actualizar solo sus metas.
-- =====================================================================================

alter table public.meta add column if not exists camino jsonb;

comment on column public.meta.camino is
  'Camino fijo de la meta: paso, cadencia, meses_por_paso, primer_corte, ahorrado_inicial, objetivo, '
  'fecha_limite, estado, reinicio_indice y reinicio_corte. Null hasta que la app lo arma.';
