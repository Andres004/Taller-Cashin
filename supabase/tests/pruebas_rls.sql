-- Pruebas de integridad y RLS. Local: ejecutar después de 00_mock + 001 + 002.
-- Las líneas "ERROR esperado" deben fallar; el resto debe funcionar.
\set ON_ERROR_STOP 0
insert into auth.users(id,email,raw_user_meta_data) values
 ('11111111-1111-1111-1111-111111111111','ana@x.com','{"nombre":"Ana"}'),
 ('22222222-2222-2222-2222-222222222222','beto@x.com','{"full_name":"Beto G"}');
select id, nombre from perfil order by nombre;
set role authenticated;
select set_config('request.jwt.claim.sub','11111111-1111-1111-1111-111111111111',false);
\echo --- Ana ve 11 categorias del sistema
select count(*) from categoria;
insert into categoria(usuario_id,nombre,tipo) values (auth.uid(),'Cafe','GASTO');
\echo --- registro valido
insert into registro(categoria_id,tipo,monto,medio_pago) select id,'GASTO',20,'QR' from categoria where nombre='Alimentación';
insert into registro(categoria_id,tipo,monto) select id,'GASTO',15.5 from categoria where nombre='Cafe';
\echo --- ERROR esperado: tipo incoherente
insert into registro(categoria_id,tipo,monto) select id,'INGRESO',20 from categoria where nombre='Alimentación';
\echo --- ERROR esperado: monto 0
insert into registro(categoria_id,tipo,monto) select id,'GASTO',0 from categoria where nombre='Alimentación';
select xp, racha_actual, racha_maxima, ultimo_registro is not null as hoy from perfil;
\echo --- ERROR esperado: no puede editar xp
update perfil set xp = 9999;
update perfil set nombre='Ana María' returning nombre;
select * from v_totales_mes;
select nombre,total from v_gasto_categoria_mes order by total desc;
insert into meta(nombre,monto_objetivo) values ('Celular',2500);
insert into aporte_meta(meta_id,tipo,monto) select id,'AHORRO',300 from meta;
\echo --- ERROR esperado: retiro supera ahorro
insert into aporte_meta(meta_id,tipo,monto) select id,'RETIRO',500 from meta;
select nombre,total_ahorrado from v_avance_meta;
insert into deuda(tipo,contraparte,monto_total) values ('DEBO','Juan',100);
insert into abono_deuda(deuda_id,monto) select id,60 from deuda;
\echo --- ERROR esperado: abono supera deuda
insert into abono_deuda(deuda_id,monto) select id,50 from deuda;
select contraparte,pendiente from v_deuda;
\echo --- ERROR esperado: la app no inserta desafios
insert into desafio(titulo,periodo,regla,monto_limite,fecha_inicio,fecha_fin) values ('x','SEMANAL','LIMITE_TOTAL',100,current_date,current_date+6);
\echo --- Beto
select set_config('request.jwt.claim.sub','22222222-2222-2222-2222-222222222222',false);
\echo --- Beto ve 0 registros, 11 categorias, 0 metas, solo su perfil
select (select count(*) from registro) reg,(select count(*) from categoria) cat,(select count(*) from meta) metas,(select count(*) from v_totales_mes) tot,(select string_agg(nombre,',') from perfil) perfiles;
\echo --- ERROR esperado: usar categoria de Ana
insert into registro(usuario_id,categoria_id,tipo,monto) values (auth.uid(),(select id from categoria where nombre='Cafe' limit 1),'GASTO',5);
select count(*) as cat_ana_visible from categoria where nombre='Cafe';
\echo --- ERROR esperado: insertar registro a nombre de Ana
insert into registro(usuario_id,categoria_id,tipo,monto) select '11111111-1111-1111-1111-111111111111',id,'GASTO',5 from categoria where nombre='Transporte';
\echo --- 0 filas: borrar registros de Ana
delete from registro;
reset role;
select count(*) as registros_ana_intactos from registro;
\echo --- racha: simular ayer
update perfil set ultimo_registro = (now() at time zone 'America/La_Paz')::date - 1, racha_actual=3 where nombre like 'Ana%';
set role authenticated;
select set_config('request.jwt.claim.sub','11111111-1111-1111-1111-111111111111',false);
insert into registro(categoria_id,tipo,monto) select id,'INGRESO',3000 from categoria where nombre='Sueldo';
select xp, racha_actual, racha_maxima from perfil;
