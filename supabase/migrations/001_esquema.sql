-- =====================================================================
-- Cashin v6 (Supabase) — 001_esquema.sql
-- Libreta digital: solo anotaciones, sin saldos ni movimientos de dinero.
-- Ejecutar en Supabase: SQL Editor → pegar → Run (o `supabase db push`).
-- Los usuarios viven en auth.users (Supabase Auth); aquí solo su perfil.
-- =====================================================================

-- 1. Perfil -----------------------------------------------------------------
create table public.perfil (
    id              uuid primary key references auth.users(id) on delete cascade,
    nombre          varchar(80) not null,
    moneda          char(3)     not null default 'BOB' check (moneda in ('BOB','USD')),
    avatar          varchar(30) not null default 'salchi',
    xp              int         not null default 0 check (xp >= 0),
    racha_actual    int         not null default 0 check (racha_actual >= 0),
    racha_maxima    int         not null default 0 check (racha_maxima >= 0),
    ultimo_registro date,
    creado_en       timestamptz not null default now()
);

-- Crea el perfil automáticamente cuando alguien se registra (correo o Google).
create function public.fn_crear_perfil() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into public.perfil (id, nombre)
  values (new.id, left(coalesce(nullif(new.raw_user_meta_data->>'nombre', ''),
                                nullif(new.raw_user_meta_data->>'full_name', ''),
                                nullif(new.raw_user_meta_data->>'name', ''),
                                split_part(new.email, '@', 1),
                                'Usuario'), 80));
  return new;
end; $$;

create trigger trg_nuevo_usuario after insert on auth.users
  for each row execute function public.fn_crear_perfil();

-- 2. Categorías ---------------------------------------------------------------
create table public.categoria (
    id         uuid primary key default gen_random_uuid(),
    usuario_id uuid references auth.users(id) on delete cascade,   -- NULL = del sistema
    nombre     varchar(50) not null,
    tipo       varchar(10) not null check (tipo in ('GASTO','INGRESO')),
    icono      varchar(50),
    color      char(7) check (color ~ '^#[0-9A-Fa-f]{6}$'),
    archivada  boolean not null default false,
    unique nulls not distinct (usuario_id, nombre, tipo),
    unique (id, tipo)                       -- habilita la FK compuesta (id, tipo)
);

-- 3. Planificados y registros -------------------------------------------------
create table public.registro_planificado (
    id            uuid primary key default gen_random_uuid(),
    usuario_id    uuid not null default auth.uid() references auth.users(id) on delete cascade,
    categoria_id  uuid not null,
    tipo          varchar(10)   not null check (tipo in ('GASTO','INGRESO')),
    nombre        varchar(60)   not null,
    monto         numeric(14,2) not null check (monto > 0),
    medio_pago    varchar(15)   not null default 'EFECTIVO'
                  check (medio_pago in ('EFECTIVO','QR','TARJETA','TRANSFERENCIA','OTRO')),
    frecuencia    varchar(10)   not null check (frecuencia in ('UNICA','SEMANAL','QUINCENAL','MENSUAL','ANUAL')),
    proxima_fecha date          not null,
    estado        varchar(10)   not null default 'ACTIVO' check (estado in ('ACTIVO','PAUSADO','CANCELADO')),
    creado_en     timestamptz   not null default now(),
    foreign key (categoria_id, tipo) references public.categoria (id, tipo)   -- el tipo debe coincidir
);

create table public.registro (
    id             uuid primary key default gen_random_uuid(),
    usuario_id     uuid not null default auth.uid() references auth.users(id) on delete cascade,
    categoria_id   uuid not null,
    planificado_id uuid references public.registro_planificado(id) on delete set null,
    tipo           varchar(10)   not null check (tipo in ('GASTO','INGRESO')),
    monto          numeric(14,2) not null check (monto > 0),
    medio_pago     varchar(15)   not null default 'EFECTIVO'
                   check (medio_pago in ('EFECTIVO','QR','TARJETA','TRANSFERENCIA','OTRO')),
    fecha_hora     timestamptz   not null default now(),
    nota           varchar(200),
    creado_en      timestamptz   not null default now(),
    foreign key (categoria_id, tipo) references public.categoria (id, tipo)
);

-- 4. Metas --------------------------------------------------------------------
create table public.meta (
    id             uuid primary key default gen_random_uuid(),
    usuario_id     uuid not null default auth.uid() references auth.users(id) on delete cascade,
    nombre         varchar(60)   not null,
    descripcion    varchar(200),
    monto_objetivo numeric(14,2) not null check (monto_objetivo > 0),
    fecha_limite   date,
    estado         varchar(12)   not null default 'ACTIVA' check (estado in ('ACTIVA','COMPLETADA','CANCELADA')),
    creado_en      timestamptz   not null default now()
);

create table public.aporte_meta (
    id      uuid primary key default gen_random_uuid(),
    meta_id uuid not null references public.meta(id) on delete cascade,
    tipo    varchar(8)    not null check (tipo in ('AHORRO','RETIRO')),
    monto   numeric(14,2) not null check (monto > 0),
    fecha   date          not null default (now() at time zone 'America/La_Paz')::date,
    nota    varchar(200)
);

-- 5. Deudas -------------------------------------------------------------------
create table public.deuda (
    id                uuid primary key default gen_random_uuid(),
    usuario_id        uuid not null default auth.uid() references auth.users(id) on delete cascade,
    tipo              varchar(10)   not null check (tipo in ('DEBO','ME_DEBEN')),
    contraparte       varchar(80)   not null,
    descripcion       varchar(200),
    monto_total       numeric(14,2) not null check (monto_total > 0),
    fecha_inicio      date          not null default (now() at time zone 'America/La_Paz')::date,
    fecha_limite      date,
    estado            varchar(10)   not null default 'PENDIENTE' check (estado in ('PENDIENTE','PAGADA')),
    fecha_cancelacion date,
    check ((estado = 'PAGADA') = (fecha_cancelacion is not null))
);

create table public.abono_deuda (
    id       uuid primary key default gen_random_uuid(),
    deuda_id uuid not null references public.deuda(id) on delete cascade,
    monto    numeric(14,2) not null check (monto > 0),
    fecha    date          not null default (now() at time zone 'America/La_Paz')::date,
    nota     varchar(200)
);

-- 6. Desafíos (gamificación) ----------------------------------------------------
create table public.desafio (
    id            uuid primary key default gen_random_uuid(),
    usuario_id    uuid not null default auth.uid() references auth.users(id) on delete cascade,
    titulo        varchar(80)  not null,
    descripcion   varchar(300),
    periodo       varchar(10)  not null check (periodo in ('SEMANAL','MENSUAL')),
    regla         varchar(20)  not null check (regla in ('LIMITE_CATEGORIA','LIMITE_TOTAL','DIAS_REGISTRO')),
    categoria_id  uuid references public.categoria(id),
    monto_limite  numeric(14,2) check (monto_limite > 0),
    dias_objetivo int check (dias_objetivo between 1 and 31),
    fecha_inicio  date not null,
    fecha_fin     date not null,
    xp_recompensa int  not null default 50 check (xp_recompensa between 0 and 300),
    estado        varchar(10) not null default 'ACTIVO' check (estado in ('ACTIVO','CUMPLIDO','FALLIDO','DESCARTADO')),
    creado_en     timestamptz not null default now(),
    check (fecha_fin > fecha_inicio),
    check (
      (regla = 'LIMITE_CATEGORIA' and categoria_id is not null and monto_limite is not null) or
      (regla = 'LIMITE_TOTAL'     and monto_limite is not null) or
      (regla = 'DIAS_REGISTRO'    and dias_objetivo is not null)
    )
);

-- 7. Índices ------------------------------------------------------------------
create index ix_categoria_usuario      on public.categoria (usuario_id);
create index ix_registro_usuario_fecha on public.registro (usuario_id, fecha_hora desc);
create index ix_registro_categoria     on public.registro (categoria_id);
create index ix_registro_planificado   on public.registro (planificado_id);
create index ix_plan_usuario           on public.registro_planificado (usuario_id);
create index ix_plan_categoria         on public.registro_planificado (categoria_id);
create index ix_meta_usuario           on public.meta (usuario_id);
create index ix_aporte_meta            on public.aporte_meta (meta_id);
create index ix_deuda_usuario          on public.deuda (usuario_id);
create index ix_abono_deuda            on public.abono_deuda (deuda_id);
create index ix_desafio_usuario        on public.desafio (usuario_id, estado);
create index ix_desafio_categoria      on public.desafio (categoria_id);

-- 8. Validaciones (del modelo v5) -----------------------------------------------
-- Un abono no puede superar lo pendiente de la deuda.
create function public.fn_validar_abono() returns trigger
language plpgsql set search_path = public as $$
declare v_total numeric; v_abonado numeric;
begin
  select monto_total into v_total from public.deuda where id = new.deuda_id for update;
  select coalesce(sum(monto), 0) into v_abonado from public.abono_deuda
   where deuda_id = new.deuda_id and (tg_op = 'INSERT' or id <> new.id);
  if v_abonado + new.monto > v_total then
    raise exception 'Los abonos superan el monto de la deuda';
  end if;
  return new;
end; $$;
create trigger trg_abono before insert or update on public.abono_deuda
  for each row execute function public.fn_validar_abono();

-- Un retiro no puede superar lo ahorrado en la meta.
create function public.fn_validar_retiro() returns trigger
language plpgsql set search_path = public as $$
declare v_ahorrado numeric;
begin
  if new.tipo = 'RETIRO' then
    perform 1 from public.meta where id = new.meta_id for update;
    select coalesce(sum(case when tipo = 'AHORRO' then monto else -monto end), 0) into v_ahorrado
      from public.aporte_meta where meta_id = new.meta_id and (tg_op = 'INSERT' or id <> new.id);
    if new.monto > v_ahorrado then
      raise exception 'El retiro supera lo ahorrado en la meta';
    end if;
  end if;
  return new;
end; $$;
create trigger trg_retiro before insert or update on public.aporte_meta
  for each row execute function public.fn_validar_retiro();

-- 9. XP y racha automáticos al anotar ---------------------------------------------
-- +10 XP por registro (solo los 5 primeros del día dan XP) y racha de días seguidos.
create function public.fn_actividad_registro() returns trigger
language plpgsql security definer set search_path = public as $$
declare
  v_hoy   date := (now() at time zone 'America/La_Paz')::date;
  v_ult   date;
  v_racha int;
  v_cant  int;
begin
  select ultimo_registro, racha_actual into v_ult, v_racha
    from public.perfil where id = new.usuario_id for update;

  select count(*) into v_cant from public.registro
   where usuario_id = new.usuario_id
     and (creado_en at time zone 'America/La_Paz')::date = v_hoy;

  v_racha := case when v_ult = v_hoy     then v_racha
                  when v_ult = v_hoy - 1 then v_racha + 1
                  else 1 end;

  update public.perfil set
    xp              = xp + case when v_cant <= 5 then 10 else 0 end,
    racha_actual    = v_racha,
    racha_maxima    = greatest(racha_maxima, v_racha),
    ultimo_registro = v_hoy
  where id = new.usuario_id;
  return new;
end; $$;
create trigger trg_actividad_registro after insert on public.registro
  for each row execute function public.fn_actividad_registro();

-- 10. Vistas (security_invoker: respetan RLS del usuario que consulta) ----------------
create view public.v_totales_mes with (security_invoker = true) as
select usuario_id,
       date_trunc('month', fecha_hora at time zone 'America/La_Paz')::date as mes,
       coalesce(sum(monto) filter (where tipo = 'GASTO'), 0)   as total_gastos,
       coalesce(sum(monto) filter (where tipo = 'INGRESO'), 0) as total_ingresos,
       count(*) as cantidad_registros
from public.registro
group by usuario_id, date_trunc('month', fecha_hora at time zone 'America/La_Paz');

create view public.v_gasto_categoria_mes with (security_invoker = true) as
select r.usuario_id,
       date_trunc('month', r.fecha_hora at time zone 'America/La_Paz')::date as mes,
       c.id as categoria_id, c.nombre, c.icono, c.color,
       sum(r.monto) as total
from public.registro r
join public.categoria c on c.id = r.categoria_id
where r.tipo = 'GASTO'
group by r.usuario_id, date_trunc('month', r.fecha_hora at time zone 'America/La_Paz'), c.id;

create view public.v_avance_meta with (security_invoker = true) as
select m.id as meta_id, m.usuario_id, m.nombre, m.monto_objetivo, m.fecha_limite, m.estado,
       coalesce(sum(case when a.tipo = 'AHORRO' then a.monto else -a.monto end), 0) as total_ahorrado
from public.meta m
left join public.aporte_meta a on a.meta_id = m.id
group by m.id;

create view public.v_deuda with (security_invoker = true) as
select d.id as deuda_id, d.usuario_id, d.tipo, d.contraparte, d.monto_total,
       d.fecha_limite, d.estado,
       d.monto_total - coalesce(sum(a.monto), 0) as pendiente
from public.deuda d
left join public.abono_deuda a on a.deuda_id = d.id
group by d.id;

-- 11. Datos iniciales: categorías del sistema ------------------------------------------
insert into public.categoria (usuario_id, nombre, tipo, icono, color) values
  (null, 'Alimentación',    'GASTO',   'restaurant', '#4CAF50'),
  (null, 'Transporte',      'GASTO',   'bus',        '#2E7D32'),
  (null, 'Vivienda',        'GASTO',   'home',       '#81C784'),
  (null, 'Salud',           'GASTO',   'health',     '#FF8C32'),
  (null, 'Educación',       'GASTO',   'school',     '#1F6E8C'),
  (null, 'Entretenimiento', 'GASTO',   'movie',      '#6A4FB0'),
  (null, 'Otros gastos',    'GASTO',   'more',       '#9E9E9E'),
  (null, 'Sueldo',          'INGRESO', 'work',       '#2E7D32'),
  (null, 'Venta',           'INGRESO', 'store',      '#4CAF50'),
  (null, 'Regalo',          'INGRESO', 'gift',       '#FF8C32'),
  (null, 'Otros ingresos',  'INGRESO', 'more',       '#9E9E9E');
