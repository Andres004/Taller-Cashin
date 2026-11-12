# Decisión · Fase 4 · Perfil

**Fecha:** 12/11/2026
**Elegida:** opción A, más el recordatorio diario de la opción C (`opciones.pdf`, páginas 2 y 4).

## Qué pidió el desarrollador
- La opción A tal como está: perfil con los números y la pantalla «Editar perfil».
- Los logros de la opción B quedan para más adelante.
- Agregar las notificaciones de la opción C: el recordatorio diario.
- La foto propia de la opción C no entra.

## Qué se implementa
- **Perfil:**
  - avatar (una pose de Cashi), nombre, correo, racha y nivel, con su barra de XP;
  - "Tus números": días usando Cashin, anotaciones, mejor racha, desafíos cumplidos, metas logradas y deudas saldadas;
  - total ahorrado en metas;
  - menú con Mis categorías, Pagos esperados, Deudas, Desafíos, **Reportes** y **Recordatorio diario**;
  - cerrar sesión, con confirmación.
- **Editar perfil:**
  - avatar elegido entre 6 poses de Cashi (en una hoja desde abajo), nombre, moneda y día de cobro (opcional);
  - "Cambiar contraseña", solo para cuentas con correo.
- **Cambiar contraseña:** pide la nueva dos veces (mínimo 8 caracteres) y usa `supabase.auth.updateUser`.
- **Recordatorio diario** (de la opción C):
  - se elige la hora y "Todos los días" o "Lun–Vie";
  - solo avisa si ese día todavía no anotaste, y el aviso nombra tu racha;
  - en Android 13 o superior pide permiso para notificar;
  - se vuelve a programar al reiniciar el teléfono.
- **Migración 010:**
  - `perfil.dia_cobro` (1 a 31), con su permiso para editarlo;
  - la función `estadisticas_perfil()`, que cuenta todo en el servidor.

## Cambios frente al PDF (avisados)
- **El recordatorio se guarda en el teléfono, no en la tabla `perfil`.** La alarma vive en cada teléfono, así que guardarlo en el servidor no sirve para nada, y así no hacen falta columnas nuevas. Sin librerías: usa AlarmManager y el permiso de notificaciones.
- **El día de cobro se guarda y se muestra, pero todavía no cambia la predicción.** Usarlo en "este mes te alcanza" toca el motor de predicción, y queda para después.
