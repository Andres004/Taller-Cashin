-- Pruebas de 010_perfil.sql. Local: ejecutar después de 00_mock + 001 a 010.
-- Las líneas "ERROR esperado" deben fallar; el resto debe funcionar.
\set ON_ERROR_STOP 0
insert into auth.users(id,email,raw_user_meta_data) values
 ('77777777-7777-7777-7777-777777777777','gabi@x.com','{"nombre":"Gabi"}'),
 ('88888888-8888-8888-8888-888888888888','hugo@x.com','{"nombre":"Hugo"}');
set role authenticated;
select set_config('request.jwt.claim.sub','77777777-7777-7777-7777-777777777777',false);

\echo --- Sin datos: 1 día usando Cashin y todo lo demás en 0
select * from estadisticas_perfil();

\echo --- Gabi anota 2 gastos, ahorra 300 en una meta y salda una deuda
insert into registro(categoria_id,tipo,monto) select id,'GASTO',20 from categoria where nombre='Alimentación';
insert into registro(categoria_id,tipo,monto) select id,'GASTO',15 from categoria where nombre='Transporte';
insert into meta(nombre,monto_objetivo) values ('Bici',300);
insert into aporte_meta(meta_id,tipo,monto) select id,'AHORRO',300 from meta;
update meta set estado = 'COMPLETADA';
insert into deuda(tipo,contraparte,monto_total) values ('DEBO','Juan',50);
insert into abono_deuda(deuda_id,monto) select id,50 from deuda;
\echo --- esperado: anotaciones 2, metas_logradas 1, deudas_saldadas 1, total_ahorrado 300
select anotaciones, metas_logradas, deudas_saldadas, total_ahorrado from estadisticas_perfil();

\echo --- Día de cobro: 15 se guarda; 0 y 32 no
update perfil set dia_cobro = 15 returning dia_cobro;
\echo --- ERROR esperado: día de cobro fuera de rango
update perfil set dia_cobro = 32;
update perfil set dia_cobro = 0;

\echo --- ERROR esperado: la app no puede escribir XP ni racha
update perfil set xp = 9999;
update perfil set racha_maxima = 99;

\echo --- Hugo solo ve sus números (esperado: anotaciones 0)
select set_config('request.jwt.claim.sub','88888888-8888-8888-8888-888888888888',false);
select anotaciones, metas_logradas, deudas_saldadas from estadisticas_perfil();
reset role;
