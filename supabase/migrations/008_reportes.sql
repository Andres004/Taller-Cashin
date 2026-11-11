-- =====================================================================================
-- Cashin · 008 · Reportes de gastos e ingresos (HU-14)
-- 1. Vista de ingresos por categoría y mes, gemela de v_gasto_categoria_mes.
-- 2. Colores distintos para las categorías del sistema: antes Alimentación, Transporte y
--    Vivienda eran tres verdes y no se distinguían en el gráfico de reportes.
--    Todos los colores salen de ColoresCategoria (ui/theme/Color.kt; la 008 le agrega 4).
--    Solo cambian las categorías del sistema (usuario_id is null); las propias no se tocan.
-- 3. Seis categorías de ingreso nuevas del sistema. Las 10 de ingreso y las 10 de gasto
--    tienen, cada grupo, un color distinto por categoría.
-- =====================================================================================

-- 1. Ingresos por categoría y mes (security_invoker: respeta el RLS de quien consulta) -----
create or replace view public.v_ingreso_categoria_mes with (security_invoker = true) as
select r.usuario_id,
       date_trunc('month', r.fecha_hora at time zone 'America/La_Paz')::date as mes,
       c.id as categoria_id, c.nombre, c.icono, c.color,
       sum(r.monto) as total
from public.registro r
join public.categoria c on c.id = r.categoria_id
where r.tipo = 'INGRESO'
group by r.usuario_id, date_trunc('month', r.fecha_hora at time zone 'America/La_Paz'), c.id;

comment on view public.v_ingreso_categoria_mes is
  'Total de ingresos por categoría y mes (hora Bolivia). Reportes de Historial (HU-14).';

-- 2. Colores de las categorías del sistema ----------------------------------------------
update public.categoria as c
set color = v.color
from (values
  ('Vivienda',        'GASTO',   '#2E7D32'),
  ('Alimentación',    'GASTO',   '#E08A2C'),
  ('Transporte',      'GASTO',   '#1F6E8C'),
  ('Entretenimiento', 'GASTO',   '#8E5BA8'),
  ('Salud',           'GASTO',   '#D4829C'),
  ('Educación',       'GASTO',   '#4CAF50'),
  ('Otros gastos',    'GASTO',   '#6B6F68'),
  -- las tres de la 005: antes repetían el azul de Transporte y el morado de Entretenimiento
  ('Ropa y accesorios', 'GASTO', '#2A9D8F'),
  ('Suscripciones',     'GASTO', '#5C6BC0'),
  ('Salidas y fiestas', 'GASTO', '#C9A227'),
  ('Sueldo',          'INGRESO', '#2E7D32'),
  ('Venta',           'INGRESO', '#E08A2C'),
  ('Regalo',          'INGRESO', '#8E5BA8'),
  ('Otros ingresos',  'INGRESO', '#6B6F68')
) as v(nombre, tipo, color)
where c.usuario_id is null
  and c.nombre = v.nombre
  and c.tipo = v.tipo;

-- 3. Categorías de ingreso nuevas -------------------------------------------------------
-- on conflict: si la migración se ejecuta dos veces, no duplica ni falla.
insert into public.categoria (usuario_id, nombre, tipo, icono, color) values
  (null, 'Trabajo extra',     'INGRESO', 'laptop',   '#4CAF50'),  -- freelance, clases, trabajos por día
  (null, 'Bonos y aguinaldo', 'INGRESO', 'star',     '#C9A227'),  -- aguinaldo, segundo aguinaldo, bonos
  (null, 'Apoyo familiar',    'INGRESO', 'family',   '#D4829C'),  -- mesada, ayuda de familia, envíos del exterior
  (null, 'Alquiler',          'INGRESO', 'key',      '#8D6E63'),  -- cobro de alquiler de un cuarto, casa o garaje
  (null, 'Intereses',         'INGRESO', 'trending', '#2A9D8F'),  -- intereses del banco, DPF o caja de ahorro
  (null, 'Devoluciones',      'INGRESO', 'refund',   '#5C6BC0')   -- te devolvieron plata o te pagaron lo prestado
on conflict do nothing;
