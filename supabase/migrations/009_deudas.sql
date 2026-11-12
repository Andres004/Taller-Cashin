-- =====================================================================================
-- Cashin · 009 · Deudas con línea de tiempo (HU-25, HU-26)
-- Una deuda es una historia de movimientos con una persona:
--   inicio (monto_inicial) + aumentos (le prestaste más / te prestó más) − abonos.
-- La base de datos recalcula sola el total, lo pendiente y el estado (PAGADA / PENDIENTE),
-- y no deja que el saldo quede negativo en ningún punto de la historia.
-- Solo se puede borrar el último movimiento (para corregir, se deshace de arriba hacia abajo).
-- Ejecutar después de 001 a 008. No cambia las migraciones anteriores.
-- =====================================================================================

-- 1. Monto inicial -------------------------------------------------------------------
-- monto_total pasa a ser monto_inicial + aumentos y lo calcula la base de datos.
alter table public.deuda add column if not exists monto_inicial numeric(14,2);
update public.deuda set monto_inicial = monto_total where monto_inicial is null;
alter table public.deuda alter column monto_inicial set not null;
do $$ begin
  alter table public.deuda add constraint deuda_monto_inicial_positivo check (monto_inicial > 0);
exception when duplicate_object then null; end $$;

-- 2. Orden de los movimientos del mismo día ------------------------------------------
alter table public.abono_deuda add column if not exists creado_en timestamptz not null default now();

-- 3. Aumentos ------------------------------------------------------------------------
create table if not exists public.aumento_deuda (
    id        uuid primary key default gen_random_uuid(),
    deuda_id  uuid          not null references public.deuda(id) on delete cascade,
    monto     numeric(14,2) not null check (monto > 0),
    fecha     date          not null default (now() at time zone 'America/La_Paz')::date,
    nota      varchar(200),
    creado_en timestamptz   not null default now()
);
create index if not exists ix_aumento_deuda on public.aumento_deuda (deuda_id);

alter table public.aumento_deuda enable row level security;
drop policy if exists aumento_dueno on public.aumento_deuda;
create policy aumento_dueno on public.aumento_deuda for all to authenticated
  using (exists (select 1 from public.deuda d where d.id = deuda_id and d.usuario_id = auth.uid()))
  with check (exists (select 1 from public.deuda d where d.id = deuda_id and d.usuario_id = auth.uid()));

-- 4. La app no escribe el total ni el estado ------------------------------------------
revoke update on public.deuda from anon, authenticated;
grant update (tipo, contraparte, descripcion, monto_inicial, fecha_inicio, fecha_limite)
  on public.deuda to authenticated;

-- 5. Recalcular una deuda ------------------------------------------------------------
-- Errores con un código al inicio del mensaje para que la app muestre un texto amable:
--   DEUDA_SALDO_NEGATIVO · DEUDA_FECHA_INVALIDA · DEUDA_TIPO_CON_MOVIMIENTOS · DEUDA_SOLO_ULTIMO
create or replace function public.fn_recalcular_deuda(p_deuda uuid) returns void
language plpgsql security definer set search_path = public as $$
declare
  d          public.deuda%rowtype;
  v_hoy      date := (now() at time zone 'America/La_Paz')::date;
  v_aumentos numeric;
  v_abonado  numeric;
  v_total    numeric;
  v_minimo   numeric;
  v_ultimo   date;
begin
  select * into d from public.deuda where id = p_deuda for update;
  if not found then
    return; -- la deuda se está borrando (cascada)
  end if;

  if exists (select 1 from public.aumento_deuda where deuda_id = p_deuda and (fecha < d.fecha_inicio or fecha > v_hoy))
     or exists (select 1 from public.abono_deuda where deuda_id = p_deuda and (fecha < d.fecha_inicio or fecha > v_hoy)) then
    raise exception 'DEUDA_FECHA_INVALIDA: un movimiento queda antes del inicio de la deuda o en el futuro';
  end if;

  -- Saldo después de cada movimiento, en el orden de la línea de tiempo:
  -- por fecha; el mismo día, primero el inicio, luego los aumentos y al final los abonos.
  select min(saldo) into v_minimo from (
    select sum(delta) over (order by fecha, orden, creado_en, id rows unbounded preceding) as saldo
    from (
      select d.fecha_inicio as fecha, 0 as orden, '-infinity'::timestamptz as creado_en, p_deuda as id, d.monto_inicial as delta
      union all
      select fecha, 1, creado_en, id, monto from public.aumento_deuda where deuda_id = p_deuda
      union all
      select fecha, 2, creado_en, id, -monto from public.abono_deuda where deuda_id = p_deuda
    ) movimientos
  ) saldos;
  if v_minimo < 0 then
    raise exception 'DEUDA_SALDO_NEGATIVO: los abonos superan lo que se debía en ese momento';
  end if;

  select coalesce(sum(monto), 0) into v_aumentos from public.aumento_deuda where deuda_id = p_deuda;
  select coalesce(sum(monto), 0), max(fecha) into v_abonado, v_ultimo from public.abono_deuda where deuda_id = p_deuda;
  v_total := d.monto_inicial + v_aumentos;

  update public.deuda set
    monto_total       = v_total,
    estado            = case when v_total - v_abonado = 0 then 'PAGADA' else 'PENDIENTE' end,
    fecha_cancelacion = case when v_total - v_abonado = 0 then v_ultimo end
  where id = p_deuda
    and (monto_total is distinct from v_total
         or estado is distinct from (case when v_total - v_abonado = 0 then 'PAGADA' else 'PENDIENTE' end)
         or fecha_cancelacion is distinct from (case when v_total - v_abonado = 0 then v_ultimo end));
end; $$;

-- Solo la usan los triggers; nadie la llama desde la app.
revoke execute on function public.fn_recalcular_deuda(uuid) from public, anon, authenticated;

-- 6. Triggers --------------------------------------------------------------------------
-- Al crear: monto_inicial sale de monto_total (lo que envía la app) y la deuda empieza pendiente.
create or replace function public.fn_deuda_al_crear() returns trigger
language plpgsql set search_path = public as $$
begin
  new.monto_inicial     := coalesce(new.monto_inicial, new.monto_total);
  new.monto_total       := new.monto_inicial;
  new.estado            := 'PENDIENTE';
  new.fecha_cancelacion := null;
  return new;
end; $$;
drop trigger if exists trg_deuda_al_crear on public.deuda;
create trigger trg_deuda_al_crear before insert on public.deuda
  for each row execute function public.fn_deuda_al_crear();

-- El tipo (Debo / Me deben) no cambia si ya hay movimientos.
create or replace function public.fn_deuda_tipo() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if new.tipo <> old.tipo and (
       exists (select 1 from public.aumento_deuda where deuda_id = old.id)
       or exists (select 1 from public.abono_deuda where deuda_id = old.id)) then
    raise exception 'DEUDA_TIPO_CON_MOVIMIENTOS: no se puede cambiar el tipo de una deuda con movimientos';
  end if;
  return new;
end; $$;
drop trigger if exists trg_deuda_tipo on public.deuda;
create trigger trg_deuda_tipo before update of tipo on public.deuda
  for each row execute function public.fn_deuda_tipo();

-- Editar el monto inicial o la fecha de inicio vuelve a revisar toda la historia.
create or replace function public.fn_deuda_editada() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  perform public.fn_recalcular_deuda(new.id);
  return null;
end; $$;
drop trigger if exists trg_deuda_editada on public.deuda;
create trigger trg_deuda_editada after update of monto_inicial, fecha_inicio on public.deuda
  for each row execute function public.fn_deuda_editada();

-- Después de anotar o borrar un abono o un aumento, se recalcula la deuda.
create or replace function public.fn_movimiento_deuda() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  perform public.fn_recalcular_deuda(coalesce(new.deuda_id, old.deuda_id));
  if tg_op = 'UPDATE' and new.deuda_id <> old.deuda_id then
    perform public.fn_recalcular_deuda(old.deuda_id);
  end if;
  return null;
end; $$;
drop trigger if exists trg_abono_recalcular on public.abono_deuda;
create trigger trg_abono_recalcular after insert or update or delete on public.abono_deuda
  for each row execute function public.fn_movimiento_deuda();
drop trigger if exists trg_aumento_recalcular on public.aumento_deuda;
create trigger trg_aumento_recalcular after insert or update or delete on public.aumento_deuda
  for each row execute function public.fn_movimiento_deuda();

-- Solo se borra el último movimiento de la línea de tiempo (mismo orden que el saldo).
-- Si se está borrando la deuda entera (cascada), no se revisa.
create or replace function public.fn_borrar_solo_ultimo() returns trigger
language plpgsql security definer set search_path = public as $$
declare
  v_orden int := case when tg_table_name = 'aumento_deuda' then 1 else 2 end;
begin
  if not exists (select 1 from public.deuda where id = old.deuda_id) then
    return old;
  end if;
  if exists (
    select 1 from (
      select fecha, 1 as orden, creado_en, id from public.aumento_deuda where deuda_id = old.deuda_id
      union all
      select fecha, 2, creado_en, id from public.abono_deuda where deuda_id = old.deuda_id
    ) m
    where (m.fecha, m.orden, m.creado_en, m.id) > (old.fecha, v_orden, old.creado_en, old.id)
  ) then
    raise exception 'DEUDA_SOLO_ULTIMO: solo se puede borrar el último movimiento';
  end if;
  return old;
end; $$;
drop trigger if exists trg_abono_solo_ultimo on public.abono_deuda;
create trigger trg_abono_solo_ultimo before delete on public.abono_deuda
  for each row execute function public.fn_borrar_solo_ultimo();
drop trigger if exists trg_aumento_solo_ultimo on public.aumento_deuda;
create trigger trg_aumento_solo_ultimo before delete on public.aumento_deuda
  for each row execute function public.fn_borrar_solo_ultimo();

-- 7. Vista ampliada (las columnas nuevas van al final para no romper lo que ya la usa) ----
create or replace view public.v_deuda with (security_invoker = true) as
select d.id as deuda_id, d.usuario_id, d.tipo, d.contraparte, d.monto_total,
       d.fecha_limite, d.estado,
       d.monto_total - coalesce(ab.total, 0) as pendiente,
       coalesce(ab.total, 0)                 as abonado,
       d.descripcion, d.fecha_inicio, d.fecha_cancelacion, d.monto_inicial,
       coalesce(au.total, 0)                 as aumentado,
       coalesce(au.cantidad, 0)::int         as cantidad_aumentos,
       coalesce(ab.cantidad, 0)::int         as cantidad_abonos
from public.deuda d
left join (select deuda_id, sum(monto) as total, count(*) as cantidad
             from public.abono_deuda group by deuda_id) ab on ab.deuda_id = d.id
left join (select deuda_id, sum(monto) as total, count(*) as cantidad
             from public.aumento_deuda group by deuda_id) au on au.deuda_id = d.id;

-- 8. Deudas que ya existían: total, estado y fecha de pago al día ---------------------
select public.fn_recalcular_deuda(id) from public.deuda;
