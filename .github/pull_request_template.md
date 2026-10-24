## Tarjeta

<!-- Ej.: HU-06 Anotar gasto rápido (enlace a Trello) -->

## Qué hace

-

## Cómo probarlo

1.

## Capturas

<!-- Obligatorio si cambia una pantalla -->

## Checklist (Definición de "Hecho", Maestro §10.4)

- [ ] Cumple los criterios de aceptación de la HU
- [ ] Respeta la estructura y las reglas por capa (la pantalla no llama a Supabase)
- [ ] Pantallas con sus 4 estados: cargando, vacío, datos y error
- [ ] Sin colores ni textos sueltos (usa `ui/theme` y `strings.xml`)
- [ ] Montos con `BigDecimal` y `Formato`, nunca `Double`
- [ ] Si cambia la BD: archivo nuevo en `supabase/migrations/` y modelos actualizados
- [ ] Probado en el emulador con un usuario real de Supabase
- [ ] No sube claves ni `local.properties`
