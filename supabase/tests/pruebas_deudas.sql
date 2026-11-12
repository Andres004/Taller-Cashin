-- Pruebas de 009_deudas.sql. Local: ejecutar después de 00_mock + 001 a 009.
-- Las líneas "ERROR esperado" deben fallar; el resto debe funcionar.
\set ON_ERROR_STOP 0
insert into auth.users(id,email,raw_user_meta_data) values
 ('55555555-5555-5555-5555-555555555555','eva@x.com','{"nombre":"Eva"}'),
 ('66666666-6666-6666-6666-666666666666','fer@x.com','{"nombre":"Fer"}');
set role authenticated;
select set_config('request.jwt.claim.sub','55555555-5555-5555-5555-555555555555',false);

\echo --- Le presto 40 a Amy hace 10 días (la app envía monto_total; la BD llena monto_inicial)
insert into deuda(tipo,contraparte,monto_total,fecha_inicio)
values ('ME_DEBEN','Amy',40,(now() at time zone 'America/La_Paz')::date - 10);
select contraparte, monto_inicial, monto_total, estado from deuda;

\echo --- Le presto 60 más hace 6 días → total 100, pendiente 100
insert into aumento_deuda(deuda_id,monto,fecha) select id,60,(now() at time zone 'America/La_Paz')::date - 6 from deuda;
select monto_total, pendiente, aumentado, cantidad_aumentos from v_deuda;

\echo --- ERROR esperado: devolución de 50 fechada hace 8 días (ese día solo debía 40)
insert into abono_deuda(deuda_id,monto,fecha) select id,50,(now() at time zone 'America/La_Paz')::date - 8 from deuda;

\echo --- ERROR esperado: abono mayor a lo pendiente (120 de 100)
insert into abono_deuda(deuda_id,monto,fecha) select id,120,(now() at time zone 'America/La_Paz')::date from deuda;

\echo --- ERROR esperado: fecha futura y fecha antes del inicio
insert into abono_deuda(deuda_id,monto,fecha) select id,10,(now() at time zone 'America/La_Paz')::date + 1 from deuda;
insert into aumento_deuda(deuda_id,monto,fecha) select id,10,(now() at time zone 'America/La_Paz')::date - 30 from deuda;

\echo --- Devuelve 30 y luego 70 → PAGADA con la fecha del último abono
insert into abono_deuda(deuda_id,monto,fecha) select id,30,(now() at time zone 'America/La_Paz')::date - 3 from deuda;
insert into abono_deuda(deuda_id,monto,fecha) select id,70,(now() at time zone 'America/La_Paz')::date from deuda;
select estado, pendiente, fecha_cancelacion = (now() at time zone 'America/La_Paz')::date as fecha_ok from v_deuda;

\echo --- ERROR esperado: la app no puede escribir el total ni el estado
update deuda set monto_total = 5;
update deuda set estado = 'PENDIENTE';

\echo --- ERROR esperado: borrar un movimiento que no es el último (el aumento)
delete from aumento_deuda;

\echo --- Borrar el último abono (70) → vuelve a PENDIENTE y sin fecha de pago
delete from abono_deuda where monto = 70;
select estado, pendiente, fecha_cancelacion is null as sin_fecha from v_deuda;

\echo --- Prestar de nuevo después de pagada: pagar 70, luego aumentar 20 → reabre
insert into abono_deuda(deuda_id,monto,fecha) select id,70,(now() at time zone 'America/La_Paz')::date from deuda;
select estado from deuda;
insert into aumento_deuda(deuda_id,monto,fecha) select id,20,(now() at time zone 'America/La_Paz')::date from deuda;
select estado, pendiente from v_deuda;

\echo --- ERROR esperado: bajar el monto inicial por debajo de lo que la historia permite (10)
update deuda set monto_inicial = 10;

\echo --- ERROR esperado: cambiar Me deben → Debo con movimientos
update deuda set tipo = 'DEBO';

\echo --- Editar nombre, descripción y fecha límite sí se puede
update deuda set contraparte = 'Amy R.', descripcion = 'Almuerzo y pasaje', fecha_limite = (now() at time zone 'America/La_Paz')::date + 15;
select contraparte, descripcion, fecha_limite is not null as con_limite from deuda;

\echo --- Fer no ve ni toca las deudas de Eva (esperado: 0 | 0 | 0)
select set_config('request.jwt.claim.sub','66666666-6666-6666-6666-666666666666',false);
select (select count(*) from deuda) deudas, (select count(*) from aumento_deuda) aumentos, (select count(*) from v_deuda) vista;
\echo --- ERROR esperado (o 0 filas): Fer intenta agregar un aumento a la deuda de Eva
insert into aumento_deuda(deuda_id,monto) select id,10 from deuda;

\echo --- Borrar la deuda entera borra sus movimientos (cascada, sin chocar con «solo el último»)
select set_config('request.jwt.claim.sub','55555555-5555-5555-5555-555555555555',false);
delete from deuda;
select (select count(*) from deuda) deudas, (select count(*) from aumento_deuda) aumentos, (select count(*) from abono_deuda) abonos;
reset role;
