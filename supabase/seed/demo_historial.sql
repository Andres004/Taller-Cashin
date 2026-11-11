-- =====================================================================
-- Cashin · Datos de DEMOSTRACIÓN (NO es una migración)
-- Llena 4 meses anteriores + el mes actual hasta ayer, y una meta con aportes, para que
-- el motor de predicción tenga historia que mostrar en la presentación.
--
-- Uso: Supabase → SQL Editor → cambiar el correo de abajo → Run.
-- Ejecutar UNA sola vez por cuenta (si se repite, duplica los datos).
-- La cuenta debe existir (crearla antes desde la app) y la migración 008 ya debe estar ejecutada.
-- =====================================================================
do $$
declare
  v_correo  text := 'demo@cashin.app';   -- ← CAMBIAR por el correo de la cuenta demo
  v_uid     uuid;
  v_mes     date;
  v_meta    uuid;
  i         int;
  d         int;
  -- factor de tendencia: el gasto sube un poco cada mes (para que la regresión tenga algo que aprender)
  f         numeric;
begin
  select id into v_uid from auth.users where email = v_correo;
  if v_uid is null then
    raise exception 'No existe una cuenta con el correo %', v_correo;
  end if;

  -- 4 meses completos anteriores al actual (i = 4 es el más antiguo)
  for i in reverse 4..1 loop
    v_mes := (date_trunc('month', (now() at time zone 'America/La_Paz')) - make_interval(months => i))::date;
    f := 1 + (4 - i) * 0.06;   -- +6 % por mes

    -- Ingreso: sueldo el día 1
    insert into public.registro (usuario_id, categoria_id, tipo, monto, medio_pago, fecha_hora, nota)
    select v_uid, c.id, 'INGRESO', 3200, 'TRANSFERENCIA', (v_mes + time '09:00') at time zone 'America/La_Paz', 'Sueldo'
    from public.categoria c where c.usuario_id is null and c.nombre = 'Sueldo';

    -- Venta ocasional a mitad de mes
    insert into public.registro (usuario_id, categoria_id, tipo, monto, medio_pago, fecha_hora, nota)
    select v_uid, c.id, 'INGRESO', 150 + i * 40, 'QR', (v_mes + 14 + time '18:00') at time zone 'America/La_Paz', 'Venta de ropa'
    from public.categoria c where c.usuario_id is null and c.nombre = 'Venta';

    -- Intereses de la caja de ahorro a fin de mes (categoría de la migración 008)
    insert into public.registro (usuario_id, categoria_id, tipo, monto, medio_pago, fecha_hora, nota)
    select v_uid, c.id, 'INGRESO', 25 + i * 2, 'TRANSFERENCIA', (v_mes + 27 + time '09:00') at time zone 'America/La_Paz', 'Intereses DPF'
    from public.categoria c where c.usuario_id is null and c.nombre = 'Intereses';

    -- Vivienda: alquiler el día 5
    insert into public.registro (usuario_id, categoria_id, tipo, monto, medio_pago, fecha_hora, nota)
    select v_uid, c.id, 'GASTO', 900, 'TRANSFERENCIA', (v_mes + 4 + time '10:00') at time zone 'America/La_Paz', 'Alquiler'
    from public.categoria c where c.usuario_id is null and c.nombre = 'Vivienda';

    -- Alimentación y transporte: casi todos los días (con variación)
    for d in 0..27 loop
      insert into public.registro (usuario_id, categoria_id, tipo, monto, medio_pago, fecha_hora, nota)
      select v_uid, c.id, 'GASTO', round((18 + (d % 5) * 4) * f, 2), 'QR',
             (v_mes + d + time '13:00') at time zone 'America/La_Paz', 'Almuerzo'
      from public.categoria c where c.usuario_id is null and c.nombre = 'Alimentación';

      if d % 2 = 0 then
        insert into public.registro (usuario_id, categoria_id, tipo, monto, medio_pago, fecha_hora, nota)
        select v_uid, c.id, 'GASTO', round(6 * f, 2), 'EFECTIVO',
               (v_mes + d + time '08:00') at time zone 'America/La_Paz', 'Micro'
        from public.categoria c where c.usuario_id is null and c.nombre = 'Transporte';
      end if;
    end loop;

    -- Entretenimiento los fines de mes
    insert into public.registro (usuario_id, categoria_id, tipo, monto, medio_pago, fecha_hora, nota)
    select v_uid, c.id, 'GASTO', round(120 * f, 2), 'TARJETA', (v_mes + 24 + time '21:00') at time zone 'America/La_Paz', 'Cine y salida'
    from public.categoria c where c.usuario_id is null and c.nombre = 'Entretenimiento';
  end loop;

  -- Mes actual: sueldo y gastos diarios hasta AYER, para que la proyección del mes tenga ritmo real
  v_mes := date_trunc('month', (now() at time zone 'America/La_Paz'))::date;
  f := 1.24;
  insert into public.registro (usuario_id, categoria_id, tipo, monto, medio_pago, fecha_hora, nota)
  select v_uid, c.id, 'INGRESO', 3200, 'TRANSFERENCIA', (v_mes + time '09:00') at time zone 'America/La_Paz', 'Sueldo'
  from public.categoria c where c.usuario_id is null and c.nombre = 'Sueldo';
  insert into public.registro (usuario_id, categoria_id, tipo, monto, medio_pago, fecha_hora, nota)
  select v_uid, c.id, 'GASTO', 900, 'TRANSFERENCIA', (v_mes + 4 + time '10:00') at time zone 'America/La_Paz', 'Alquiler'
  from public.categoria c where c.usuario_id is null and c.nombre = 'Vivienda';
  -- Un trabajo extra el día 1 del mes actual (categoría de la migración 008)
  insert into public.registro (usuario_id, categoria_id, tipo, monto, medio_pago, fecha_hora, nota)
  select v_uid, c.id, 'INGRESO', 250, 'QR', (v_mes + time '19:00') at time zone 'America/La_Paz', 'Clases particulares'
  from public.categoria c where c.usuario_id is null and c.nombre = 'Trabajo extra';
  for d in 0..((now() at time zone 'America/La_Paz')::date - v_mes - 1) loop
    insert into public.registro (usuario_id, categoria_id, tipo, monto, medio_pago, fecha_hora, nota)
    select v_uid, c.id, 'GASTO', round((18 + (d % 5) * 4) * f, 2), 'QR',
           (v_mes + d + time '13:00') at time zone 'America/La_Paz', 'Almuerzo'
    from public.categoria c where c.usuario_id is null and c.nombre = 'Alimentación';
  end loop;

  -- Meta "Celular" con 3 aportes en meses distintos
  insert into public.meta (usuario_id, nombre, descripcion, monto_objetivo, fecha_limite)
  values (v_uid, 'Celular nuevo', 'Para cambiar mi celular', 2500,
          ((now() at time zone 'America/La_Paz')::date + interval '8 months')::date)
  returning id into v_meta;

  for i in reverse 3..1 loop
    insert into public.aporte_meta (meta_id, tipo, monto, fecha, nota)
    values (v_meta, 'AHORRO', 250,
            (date_trunc('month', (now() at time zone 'America/La_Paz')) - make_interval(months => i))::date + 2,
            'Ahorro del mes');
  end loop;

  raise notice 'Listo: 4 meses de historia y la meta "Celular nuevo" para %', v_correo;
end $$;
