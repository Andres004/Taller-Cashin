-- Pruebas de 003_gamificacion.sql en PostgreSQL local (después de 00_mock + 001 + 002 + 003).
-- Las líneas "ERROR esperado" deben fallar; el resto debe funcionar.
\set ON_ERROR_STOP 0
insert into auth.users(id,email,raw_user_meta_data) values
 ('11111111-1111-1111-1111-111111111111','ana@x.com','{"nombre":"Ana"}'),
 ('22222222-2222-2222-2222-222222222222','beto@x.com','{"nombre":"Beto"}');
set role authenticated;
select set_config('request.jwt.claim.sub','11111111-1111-1111-1111-111111111111',false);
\echo --- aceptar semanal de categoría (límite 100) y mensual de días (2 días)
select periodo, regla, xp_recompensa, fecha_fin - fecha_inicio as dias from aceptar_desafio('Comida bajo control','x','SEMANAL','LIMITE_CATEGORIA',(select id from categoria where nombre='Alimentación'),100,null);
select periodo, regla, xp_recompensa from aceptar_desafio('Anota 2 días','x','MENSUAL','DIAS_REGISTRO',null,null,2);
\echo --- ERROR esperado: segundo semanal activo
select aceptar_desafio('otro','x','SEMANAL','LIMITE_TOTAL',null,500,null);
\echo --- ERROR esperado: la app no puede insertar ni cambiar desafíos directamente
insert into desafio(titulo,periodo,regla,monto_limite,fecha_inicio,fecha_fin) values ('x','SEMANAL','LIMITE_TOTAL',1,current_date,current_date+1);
update desafio set estado='CUMPLIDO';
select count(*) as cumplidos_a_mano from desafio where estado='CUMPLIDO';
\echo --- anotar 60 Bs de comida hoy → progreso
insert into registro(categoria_id,tipo,monto) select id,'GASTO',60 from categoria where nombre='Alimentación';
select d.regla, p.gastado, p.dias_con_registro from progreso_desafios() p join desafio d on d.id=p.desafio_id order by d.regla;
\echo --- evaluar: nada cambia aún
select count(*) as cambiados from evaluar_desafios();
\echo --- 50 Bs más (total 110 > 100) → semanal FALLIDO
insert into registro(categoria_id,tipo,monto) select id,'GASTO',50 from categoria where nombre='Alimentación';
select regla, estado from evaluar_desafios();
\echo --- simular un registro creado ayer → mensual llega a 2 días → CUMPLIDO +150 XP
reset role;
update registro set creado_en = creado_en - interval '1 day' where monto=60;
set role authenticated;
select xp as xp_antes from perfil;
select regla, estado from evaluar_desafios();
select xp as xp_despues from perfil;
\echo --- evaluar otra vez no vuelve a dar XP
select count(*) as cambiados from evaluar_desafios();
select xp from perfil;
\echo --- descartar: no hay activos → false
select descartar_desafio(id) from desafio limit 1;
\echo --- completar_meta: no llega → false; llega → true y +100 XP; segunda vez → false
insert into meta(nombre,monto_objetivo) values ('Celular',100);
insert into aporte_meta(meta_id,tipo,monto) select id,'AHORRO',60 from meta;
select completar_meta(id) as aun_no from meta;
insert into aporte_meta(meta_id,tipo,monto) select id,'AHORRO',40 from meta;
select completar_meta(id) as completada from meta;
select xp, (select estado from meta) from perfil;
select completar_meta(id) as segunda_vez from meta;
\echo --- Beto no ve ni toca lo de Ana
select set_config('request.jwt.claim.sub','22222222-2222-2222-2222-222222222222',false);
select (select count(*) from desafio) desafios, (select count(*) from progreso_desafios()) progreso,
       (select completar_meta((select id from meta limit 1))) meta_ajena;
reset role;
select count(*) as metas_de_ana_completadas from meta where estado='COMPLETADA';
\echo --- anon no puede ejecutar
set role anon;
select evaluar_desafios();
reset role;
