# Decisión · Fase 2 · Reportes

**Fecha:** 11/11/2026
**Elegida:** mezcla de A y B, mejorada (`propuesta.pdf`). Aprobada por el desarrollador.

## Qué pidió el desarrollador
- Combinar A y B y mejorarlas.
- Nada nuevo en Inicio: los reportes viven en **Historial** (pestañas Movimientos | Reportes).
- Gráfico circular más grande y al centro, **fuera** de la tarjeta; debajo, en una tarjeta, la lista de categorías.
- Usar las animaciones de Cashi de Ricardo (`ImagenCashi` + WebP de `res/raw/`) y no repetir siempre la misma imagen.
- Borrar las maquetas que no se usan (`opciones.html` y `opciones.pdf`).

## Qué se implementa
- `HistorialScreen` con pestañas **Movimientos | Reportes**. Movimientos queda igual, con el total del filtro arriba y el total de cada día.
- Reportes con tres vistas, **Gastos | Ingresos | Meses**, y un selector de mes con flechas (no pasa del mes actual).
- Gastos e Ingresos:
  - dona de 232 dp sin tarjeta, con el total y el cambio frente al mes anterior en el centro;
  - lista en una tarjeta con color, nombre, %, monto y ▲ ▼ con el porcentaje de cambio;
  - al tocar una porción o una fila, esa porción se resalta, el centro muestra la categoría y aparece "Ver N movimientos", que abre Movimientos filtrado.
- Meses: barras de ingresos y gastos de 6 meses, lo que sobró en los últimos 3 meses, y la división en fijos, variables y recortable (`GastosFijos.kt`).
- Cashi según el mensaje:

  | Cuándo | Recurso |
  |---|---|
  | El gasto subió | `PREOCUPADO` |
  | El gasto bajó | `CELEBRANDO` |
  | Ingresos | `FELIZ` |
  | Meses | `AHORRO` |
  | Mes sin datos | `PENSANDO` |
  | Error | `PREOCUPADO` |

- Animaciones:
  - la dona se dibuja en 600 ms y el total del centro sube desde Bs 0;
  - las filas de la lista entran escalonadas;
  - la porción tocada crece con `spring` y vibra suave;
  - las barras crecen desde abajo.
- Datos: migración **008** con `v_ingreso_categoria_mes` (la 007 ya es del camino de metas).
- El cálculo va en `prediccion/Reportes.kt`, en Kotlin puro y con pruebas.
- `ReporteRepository` registrado en `AppContainer`.
