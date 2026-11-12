# Decisión · Fase 3 · Deudas

**Fecha:** 11/11/2026
**Elegida:** la opción A, con los cambios que pidió el desarrollador (`propuesta.pdf`).

## Qué pidió el desarrollador
- La opción A: Perfil → Deudas, pestañas **Debo | Me deben** con su total, detalle y hoja para abonar.
- La **línea de tiempo** de la opción B, dentro de cada deuda.
- **Aumentar la misma deuda.** Si le prestaste Bs 40 a Amy y después te pide más, se suma a esa deuda; no se crea un "préstamo 2". Lo mismo si alguien te presta más a ti.
- Modelarlo como deuda, no como meta: sin pasos, sin camino y sin "monto meta".
- Cubrir todos los casos para que nada falle.
- **Solo se puede borrar el último movimiento.** Para corregir uno anterior, se borra de arriba hacia abajo, como deshacer.

## Qué se implementa
- **Movimientos de una deuda:** inicio, aumentos ("Le prestaste más" o "Te prestó más") y abonos ("Te devolvió" o "Le pagaste").
  - Lo que falta = inicio + aumentos − abonos.
  - Cada movimiento muestra cuánto quedaba después.
- **Detalle:**
  - línea de tiempo con lo más nuevo arriba;
  - botones "Prestar más" y "Anotar devolución" (o "Me prestó más" y "Anotar pago" en Debo);
  - el último movimiento tiene el botón de borrar, con confirmación.
- **Prestar más:** se suma a la misma deuda; permite poner o mover la fecha límite y muestra cuánto se va a deber.
- **Nueva deuda con una persona que ya tiene una deuda abierta del mismo tipo:** se pregunta "Sumar a la de X" o "Crear otra". El nombre se compara sin mayúsculas, tildes ni espacios de más.
- **Pagada:**
  - la base de datos la marca sola, con la fecha del último abono;
  - Cashi celebra;
  - pasa a "Pagadas";
  - "Prestarle de nuevo" la reabre.
- **Avisos:**
  - "Vence en N días" (7 días o menos);
  - "Venció el …";
  - "También le debes Bs X a Amy" cuando hay deudas con la misma persona en los dos sentidos.
- **Migración 009:**
  - `deuda.monto_inicial`;
  - la tabla `aumento_deuda` con RLS;
  - `abono_deuda.creado_en`;
  - la función `fn_recalcular_deuda`, que recalcula el total, no deja que el saldo quede negativo en ningún punto, revisa las fechas y marca PAGADA o PENDIENTE;
  - que no se pueda cambiar el tipo si ya hay movimientos;
  - `v_deuda` ampliada al final.
- **Diferencia con la propuesta anterior:** no hay vista `v_movimiento_deuda`. La app junta inicio, aumentos y abonos en `prediccion/Deudas.kt`.
