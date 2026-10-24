-- =====================================================================
-- Cashin v6 (Supabase) — 002_rls.sql
-- Row Level Security: cada usuario solo ve y modifica sus propias filas.
-- Ejecutar DESPUÉS de 001_esquema.sql.
-- =====================================================================

alter table public.perfil               enable row level security;
alter table public.categoria            enable row level security;
alter table public.registro_planificado enable row level security;
alter table public.registro             enable row level security;
alter table public.meta                 enable row level security;
alter table public.aporte_meta          enable row level security;
alter table public.deuda                enable row level security;
alter table public.abono_deuda          enable row level security;
alter table public.desafio              enable row level security;

-- Perfil: solo el propio. La app solo puede editar nombre, moneda y avatar;
-- xp y racha los cambian los triggers y funciones del servidor.
create policy perfil_select on public.perfil for select to authenticated
  using (id = auth.uid());
create policy perfil_update on public.perfil for update to authenticated
  using (id = auth.uid()) with check (id = auth.uid());
revoke insert, update, delete on public.perfil from anon, authenticated;
grant update (nombre, moneda, avatar) on public.perfil to authenticated;

-- Categorías: ve las del sistema y las suyas; crea y modifica solo las suyas.
create policy cat_select on public.categoria for select to authenticated
  using (usuario_id is null or usuario_id = auth.uid());
create policy cat_insert on public.categoria for insert to authenticated
  with check (usuario_id = auth.uid());
create policy cat_update on public.categoria for update to authenticated
  using (usuario_id = auth.uid()) with check (usuario_id = auth.uid());
create policy cat_delete on public.categoria for delete to authenticated
  using (usuario_id = auth.uid());

-- Registros: dueño + la categoría (y el planificado, si hay) deben ser suyos o del sistema.
create policy reg_dueno on public.registro for all to authenticated
  using (usuario_id = auth.uid())
  with check (
    usuario_id = auth.uid()
    and exists (select 1 from public.categoria c
                 where c.id = categoria_id and (c.usuario_id is null or c.usuario_id = auth.uid()))
    and (planificado_id is null or exists (select 1 from public.registro_planificado p
                 where p.id = planificado_id and p.usuario_id = auth.uid()))
  );

create policy plan_dueno on public.registro_planificado for all to authenticated
  using (usuario_id = auth.uid())
  with check (
    usuario_id = auth.uid()
    and exists (select 1 from public.categoria c
                 where c.id = categoria_id and (c.usuario_id is null or c.usuario_id = auth.uid()))
  );

create policy meta_dueno on public.meta for all to authenticated
  using (usuario_id = auth.uid()) with check (usuario_id = auth.uid());

create policy deuda_dueno on public.deuda for all to authenticated
  using (usuario_id = auth.uid()) with check (usuario_id = auth.uid());

-- Desafíos: la app solo los lee. Se crean, descartan y evalúan con funciones
-- del servidor (003_gamificacion.sql, Sprint 4).
create policy desafio_select on public.desafio for select to authenticated
  using (usuario_id = auth.uid());

-- Tablas hijas: dueño a través de la tabla padre.
create policy aporte_dueno on public.aporte_meta for all to authenticated
  using (exists (select 1 from public.meta m where m.id = meta_id and m.usuario_id = auth.uid()))
  with check (exists (select 1 from public.meta m where m.id = meta_id and m.usuario_id = auth.uid()));

create policy abono_dueno on public.abono_deuda for all to authenticated
  using (exists (select 1 from public.deuda d where d.id = deuda_id and d.usuario_id = auth.uid()))
  with check (exists (select 1 from public.deuda d where d.id = deuda_id and d.usuario_id = auth.uid()));
