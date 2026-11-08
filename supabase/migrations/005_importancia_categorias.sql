-- =====================================================================
-- Cashin v6 (Supabase) — 005_importancia_categorias.sql
-- Importancia del 1 al 5 por categoría de gasto: qué tanto se puede recortar.
-- - categoria.importancia: valor inicial automático (la persona no tiene que llenar nada).
-- - categoria_importancia: el valor que cada persona eligió, si quiso cambiarlo.
-- - Tres categorías nuevas del sistema para separar los gastos que más se pueden recortar.
-- Ejecutar DESPUÉS de 001, 002, 003 y 004. Se puede volver a ejecutar sin romper nada.
-- =====================================================================

-- 1. Valor inicial por categoría ------------------------------------------------------
alter table public.categoria
  add column if not exists importancia smallint not null default 3
  check (importancia between 1 and 5);

update public.categoria
   set importancia = case nombre
         when 'Vivienda'        then 4
         when 'Salud'           then 4
         when 'Alimentación'    then 3
         when 'Educación'       then 3
         when 'Transporte'      then 3
         when 'Entretenimiento' then 2
         when 'Otros gastos'    then 2
       end
 where usuario_id is null
   and tipo = 'GASTO'
   and nombre in ('Vivienda','Salud','Alimentación','Educación','Transporte','Entretenimiento','Otros gastos');

-- 2. Categorías nuevas del sistema ------------------------------------------------------
insert into public.categoria (usuario_id, nombre, tipo, icono, color, importancia) values
  (null, 'Ropa y accesorios', 'GASTO', 'gift',  '#1F6E8C', 2),
  (null, 'Suscripciones',     'GASTO', 'movie', '#C0473A', 2),
  (null, 'Salidas y fiestas', 'GASTO', 'movie', '#8E5BA8', 1)
on conflict (usuario_id, nombre, tipo) do nothing;

-- 3. Lo que cada persona eligió ---------------------------------------------------------
-- Tabla aparte porque las categorías del sistema son de todos y RLS no deja modificarlas.
create table if not exists public.categoria_importancia (
    usuario_id   uuid     not null default auth.uid() references auth.users(id) on delete cascade,
    categoria_id uuid     not null references public.categoria(id) on delete cascade,
    importancia  smallint not null check (importancia between 1 and 5),
    primary key (usuario_id, categoria_id)
);

alter table public.categoria_importancia enable row level security;

drop policy if exists cat_imp_select on public.categoria_importancia;
create policy cat_imp_select on public.categoria_importancia for select to authenticated
  using (usuario_id = auth.uid());

drop policy if exists cat_imp_insert on public.categoria_importancia;
create policy cat_imp_insert on public.categoria_importancia for insert to authenticated
  with check (
    usuario_id = auth.uid()
    and exists (select 1 from public.categoria c
                 where c.id = categoria_id and c.tipo = 'GASTO'
                   and (c.usuario_id is null or c.usuario_id = auth.uid()))
  );

drop policy if exists cat_imp_update on public.categoria_importancia;
create policy cat_imp_update on public.categoria_importancia for update to authenticated
  using (usuario_id = auth.uid())
  with check (
    usuario_id = auth.uid()
    and exists (select 1 from public.categoria c
                 where c.id = categoria_id and c.tipo = 'GASTO'
                   and (c.usuario_id is null or c.usuario_id = auth.uid()))
  );
