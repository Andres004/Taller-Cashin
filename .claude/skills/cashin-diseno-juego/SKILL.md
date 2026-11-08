---
name: cashin-diseno-juego
description: Rediseño de UI/UX de Cashin estilo juego (tipo Duolingo) en Jetpack Compose, sin cambiar la paleta ni la tipografía. Úsala siempre que se cree o modifique cualquier pantalla o componente visual de Cashin. Antes de tocar código genera una simulación en PDF y espera aprobación.
---

# Cashin · Diseño estilo juego

Cashin es una app Android (Kotlin + Jetpack Compose, Material 3) de anotaciones de finanzas personales con un avatar (Cashi) y gamificación: XP, nivel, racha, desafíos y metas. Esta skill convierte la interfaz en una experiencia **tipo juego, al estilo Duolingo**: cálida, táctil, con recompensas visibles y progreso como un camino. **Sin cambiar los colores ni la tipografía.**

## 0. Regla de oro: primero se simula, después se programa

Cada vez que esta skill se aplique a una o más pantallas:

1. **No tocar el código de la app todavía.**
2. Generar una **simulación visual** de cómo quedará cada pantalla afectada:
   - Crear `docs/mockups/<fase>/mockup.html`: una maqueta HTML estática con marcos de celular de **360 × 800 dp**, usando **exactamente** los colores de la sección 2 y la fuente Poppins (Google Fonts). Datos de ejemplo realistas en Bs: los de `supabase/seed/demo_historial.sql`.
   - Por cada pantalla mostrar **"Antes"** (resumen del diseño actual leído del código) y **"Después"**, más los estados importantes: vacío, cargando, con datos y celebración, si aplican.
   - Bajo cada pantalla, una lista breve de los cambios y los componentes nuevos o modificados.
   - Exportar a **`docs/mockups/<fase>/mockup.pdf`**. Usar el navegador sin interfaz que haya disponible (por ejemplo `npx playwright` con Chromium, o `chrome --headless --print-to-pdf`). Si no hay forma de exportar, decirlo y pedir al desarrollador que abra el HTML y lo imprima como PDF.
3. **Detenerse** y pedir aprobación: "Revisa `docs/mockups/<fase>/mockup.pdf`. ¿Apruebas, o qué cambio?".
4. Solo con la aprobación explícita, implementar en Compose **lo mismo que se aprobó**. Si al programar algo no se puede hacer igual, avisar antes de desviarse.

La maqueta HTML es solo para revisión: no se usa en la app y no se agrega ninguna librería por ella.

## 1. Qué no se cambia nunca

- **Paleta y tipografía:** colores de `ui/theme/Color.kt` y fuente **Poppins** (`ui/theme/Type.kt`).
- **Colores sueltos:** prohibido `Color(0x…)` en pantallas. Todo color viene de `MaterialTheme.colorScheme`, `CashinColores` o las constantes de `Color.kt`.
- **Tonos nuevos (sombras 3D, bordes):** si hace falta uno, se **deriva** de un color existente (el mismo tono, más oscuro o más claro), se agrega en `Color.kt` con un comentario `// derivado de X` y se muestra en la simulación para aprobarlo.
- **Funcionalidad:** el rediseño **no cambia** lo que hace cada pantalla, sus datos ni su navegación. Solo cómo se ve y cómo se siente.
- **Dependencias:** no se agregan librerías sin preguntar. Todo lo de esta skill se hace con Compose estándar (animaciones, `Canvas`, `LocalHapticFeedback`).
- **Textos:** van en `strings.xml`, en español, con tono cercano y positivo.

## 2. Paleta de Cashin (referencia, no se modifica)

| Rol | Constante | Hex |
|---|---|---|
| Fondo general | `Crema` | `#FCF7EA` |
| Tarjetas | `CremaTarjeta` | `#FFFCF3` |
| Campos | `CremaCampo` | `#FDF9EE` |
| Bordes | `BordeCampo` | `#CCC8BE` |
| Texto y títulos | `VerdeOscuro` | `#2B5333` |
| Encabezados destacados | `VerdeProfundo` | `#075D36` |
| Acción principal | `Verde` | `#6D9A5A` |
| Verde intermedio | `VerdeMedio` | `#56A34D` |
| Progreso / fondos suaves | `VerdeClaro` | `#B4D5AA` |
| Categoría | `VerdeCategoria` | `#8DBE84` |
| Acento: XP, racha, avatar | `Naranja` | `#E08A2C` |
| Acento suave | `NaranjaClaro` | `#FDE7CF` |
| Gasto / error | `RojoGasto` | `#C0473A` |
| Ingreso | `VerdeIngreso` | `#2E7D32` |
| Texto secundario | `GrisTexto` | `#6B6F68` |

**Uso sugerido para el efecto 3D:**
- Botón `Verde` → sombra `VerdeProfundo` o `VerdeOscuro`.
- Botón `Naranja` → sombra derivada (naranja más oscuro, se aprueba en la simulación).
- Tarjeta `CremaTarjeta` → borde y sombra `BordeCampo`.

## 3. Principios del estilo juego (inspirado en Duolingo)

1. **Táctil y "gordito":**
   - Botones grandes (alto ≥ 52 dp), esquinas muy redondeadas (16–24 dp) y **efecto 3D**: una base de 4 dp en el tono oscuro bajo el botón.
   - Al presionar, el botón baja esos 4 dp (animación de 80–120 ms) y vibra suave (`HapticFeedbackType.TextHandleMove` o `LongPress`).
2. **Progreso como camino:**
   - Metas y avances se ven como un **sendero vertical de nodos circulares** (60–72 dp) que zigzaguean: completado (verde, ✓), actual (más grande, anillo naranja pulsante), pendiente (gris claro, bloqueado) y meta final (cofre o trofeo con Cashi).
   - Los nodos se unen con un trazo punteado dibujado en `Canvas`.
3. **Recompensas visibles:**
   - XP, racha 🔥 y nivel siempre a la vista en una **barra superior de estadísticas** con chips: ícono + número en `Naranja`.
   - Cada logro (anotar, cumplir un desafío, completar una meta, subir de nivel) tiene un momento de **celebración**: Cashi celebrando, número de XP que sube con animación y confeti simple en `Canvas` con colores de la paleta.
4. **Cashi como guía:**
   - El avatar aparece en momentos clave con globo de diálogo: consejos, estados vacíos y logros.
   - Su expresión acompaña el mensaje (`Emocion`).
   - Usa las imágenes o GIF del catálogo de recursos (`RecursosCashi`) cuando existan.
5. **Una acción principal por pantalla:** un solo botón destacado. Lo secundario va como texto o íconos.
6. **Tarjetas "de juego":**
   - Fondo `CremaTarjeta`, borde de 2 dp `BordeCampo` y base 3D de 3–4 dp, esquinas de 20 dp.
   - Íconos grandes de categoría dentro de círculos de color suave.
   - Barras de progreso gruesas (12–16 dp) con extremos redondeados y un brillo superior sutil.
7. **Jerarquía clara:**
   - Títulos Poppins Bold 22–28 sp, números importantes Bold 28–40 sp, textos 14–16 sp.
   - Máximo 3 niveles de jerarquía por tarjeta.
8. **Movimiento con intención:**
   - Animaciones cortas (150–400 ms) con `spring` y entradas con `AnimatedVisibility` (desliza + aparece).
   - Nada que distraiga ni se repita sin fin, salvo el pulso del nodo actual.
9. **Números amables:**
   - Mostrar la conclusión antes que el dato: "¡Vas bien! 🎯", y debajo "Bs 250 este mes".
   - Nada de rangos o términos técnicos a la vista; si se necesitan, van en un "¿Cómo lo calculé?" plegable.
10. **Accesible:**
    - Contraste suficiente (texto `VerdeOscuro` sobre `Crema`).
    - Áreas táctiles ≥ 48 dp y `contentDescription` en íconos.
    - La información nunca depende solo del color: agregar ícono o texto.

## 4. Componentes a crear o mejorar (en `ui/components/`)

Al implementar, preferir **componentes reutilizables** y ampliar los existentes sin romper sus llamadas actuales (agregar parámetros con valor por defecto):

| Componente | Qué es |
|---|---|
| `BotonJuego` | Botón 3D (principal, secundario y peligro), con presión animada y háptica. Puede reemplazar internamente el estilo de `CashinBoton`. |
| `TarjetaJuego` | Tarjeta con borde y base 3D. Evolución de `CashinTarjeta`. |
| `BarraEstadisticas` | Chips de racha 🔥, XP y nivel para la parte superior. |
| `NodoCamino` + `CaminoMeta` | Sendero de nodos para metas (estados completado, actual, pendiente y final). |
| `BarraProgresoJuego` | Barra gruesa con brillo y animación de llenado. |
| `Celebracion` | Capa de celebración: Cashi, XP que sube y confeti en `Canvas`. |
| `ChipXp` | "+10 XP" que aparece y sube. |
| `AvatarBurbuja` | Mejorar el globo de diálogo (cola del globo, entrada animada) sin cambiar su firma. |

Cada componente lleva un `@Preview` con datos de ejemplo.

## 5. Checklist antes de dar por terminada una pantalla

- [ ] Se aprobó su simulación en PDF y lo implementado coincide con ella.
- [ ] Solo colores de la paleta (o derivados aprobados) y solo Poppins.
- [ ] Una acción principal, botones 3D con presión y háptica.
- [ ] XP, racha y nivel visibles donde corresponde; logros con celebración.
- [ ] Estados vacío / cargando / error con Cashi y un mensaje amable.
- [ ] Números con conclusión en lenguaje simple; nada técnico a la vista.
- [ ] Animaciones cortas; nada parpadea sin motivo.
- [ ] Áreas táctiles ≥ 48 dp, `contentDescription`, buen contraste.
- [ ] La funcionalidad y la navegación siguen exactamente igual.
- [ ] Compila y pasan las pruebas.
