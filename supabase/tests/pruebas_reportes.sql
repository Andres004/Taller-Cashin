-- Pruebas de la migración 008 (reportes). Local: ejecutar después de 00_mock + 001 a 008.
-- Cada bloque dice qué resultado se espera.
\set ON_ERROR_STOP 0
insert into auth.users(id,email,raw_user_meta_data) values
 ('33333333-3333-3333-3333-333333333333','caro@x.com','{"nombre":"Caro"}'),
 ('44444444-4444-4444-4444-444444444444','dani@x.com','{"nombre":"Dani"}');

\echo --- Colores: cada tipo con un color distinto por categoría (esperado: GASTO 10 | 10, INGRESO 10 | 10)
select tipo, count(*) as categorias, count(distinct color) as colores
from categoria where usuario_id is null group by tipo order by tipo;

\echo --- Ingresos nuevos (esperado: 6 filas, sin Beca ni Reembolsos)
select nombre, icono, color from categoria
where usuario_id is null and tipo = 'INGRESO'
  and nombre in ('Trabajo extra','Bonos y aguinaldo','Apoyo familiar','Alquiler','Intereses','Devoluciones')
order by nombre;

\echo --- Colores: Alimentación #E08A2C, Transporte #1F6E8C, Vivienda #2E7D32
select nombre, color from categoria
where usuario_id is null and nombre in ('Alimentación','Transporte','Vivienda') order by nombre;

set role authenticated;
select set_config('request.jwt.claim.sub','33333333-3333-3333-3333-333333333333',false);
insert into registro(categoria_id,tipo,monto) select id,'INGRESO',3000 from categoria where nombre='Sueldo';
insert into registro(categoria_id,tipo,monto) select id,'INGRESO',150 from categoria where nombre='Venta';
insert into registro(categoria_id,tipo,monto) select id,'INGRESO',50 from categoria where nombre='Venta';
insert into registro(categoria_id,tipo,monto) select id,'GASTO',70 from categoria where nombre='Alimentación';

\echo --- Caro: ingresos por categoría del mes (esperado: Sueldo 3000, Venta 200; sin Alimentación)
select nombre, total from v_ingreso_categoria_mes order by total desc;

\echo --- Caro: el gasto sigue en su propia vista (esperado: Alimentación 70)
select nombre, total from v_gasto_categoria_mes;

\echo --- Dani no ve los ingresos de Caro (esperado: 0)
select set_config('request.jwt.claim.sub','44444444-4444-4444-4444-444444444444',false);
select count(*) as ingresos_ajenos from v_ingreso_categoria_mes;

\echo --- ERROR esperado (o 0 filas): un usuario no cambia el color de una categoría del sistema
update categoria set color = '#000000' where usuario_id is null and nombre = 'Sueldo';
reset role;
select color as sueldo_sigue_igual from categoria where usuario_id is null and nombre = 'Sueldo';
