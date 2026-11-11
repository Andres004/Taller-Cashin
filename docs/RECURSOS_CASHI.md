# Guía de las imágenes y animaciones de Cashi

Cada momento de la app tiene su archivo en **`app/src/main/res/raw/`**. Para cambiar un dibujo **no hace falta tocar código**: reemplaza el archivo con el mismo nombre y vuelve a instalar la app.

Si borras un archivo, la app muestra la imagen fija de la columna "Respaldo" (está en `res/drawable-nodpi/`) y no se rompe.

## Cómo debe ser cada archivo

| Requisito | Valor |
|---|---|
| Formato | **WebP** (`.webp`), animado o fijo |
| Fondo | Transparente. La app tiene fondo crema (`#FCF7EA`) |
| Tamaño | Unos **432 × 432 px**. Los chicos, según la columna "Tamaño" |
| Duración | Si es animada, 2 a 3 segundos, en bucle |
| Peso | Unos **500 KB** por archivo como máximo |
| Estilo | El mismo Cashi de siempre: mismo trazo y colores |

No uses GIF: tiene bordes dentados y pocos colores. Si solo tienes un GIF, conviértelo con `gif2webp`:

```bash
gif2webp -q 80 -m 6 cashi_feliz.gif -o cashi_feliz.webp
```

## Todos los archivos

El tamaño en pantalla está en dp, la medida que usa Android.

| Momento | Dónde se ve | Nombre exacto | Tamaño en pantalla | Respaldo |
|---|---|---|---|---|
| Splash | Al abrir la app | `cashi_bienvenida.webp` | 220 | `logo_bienvenida.webp` |
| Pantalla de bienvenida | Primera pantalla sin sesión | `cashi_pantalla_bienvenida.webp` | 240 | `logo_bienvenida.webp` |
| Iniciar sesión | Arriba del formulario | `cashi_inicio_sesion.webp` | 170 × 145 | `pose_medalla.webp` |
| Crear cuenta | Arriba del formulario | `cashi_crear_cuenta.webp` | 170 × 145 | `pose_mochila.webp` |
| Racha | Al anotar por primera vez en el día, Tu racha y Perfil | `cashi_racha.webp` | 56 a 220 | `pose_llama.webp` |
| XP | Premio al anotar, Anotar, Tu racha y Perfil | `cashi_xp.webp` | 56 a 64 | `pose_estrella.webp` |
| Ahorro | Al anotar un ingreso o un ahorro en una meta | `cashi_ahorro.webp` | 58 a 64 | `pose_cofre.webp` |
| Tu camino | Paso actual del camino de una meta | `cashi_camino.webp` | 58 | `pose_mochila.webp` |
| Desafío cumplido | Celebración al cumplir un reto | `cashi_desafio_cumplido.webp` | 128 | `pose_trofeo.webp` |
| Meta completada | Detalle de una meta ya marcada | `cashi_meta_completada.webp` | 88 | `pose_cofre.webp` |
| Meta lograda | Final del camino, **en círculo** | `cashi_meta_lograda.webp` | 52 a 86 | `pose_trofeo_cerca.webp` |
| Subida de nivel | Inicio, Tu racha y Perfil | `cashi_subida_nivel.webp` | 56 a 70 | `pose_medalla.webp` |
| Metas vacío | Metas, cuando no hay ninguna | `cashi_metas_vacio.webp` | 160 | `pose_mochila.webp` |
| Feliz | Consejos y estados vacíos | `cashi_feliz.webp` | 60 a 160 | `avatar_feliz.webp` |
| Celebrando | Consejos y logros | `cashi_celebrando.webp` | 58 a 88 | `avatar_celebrando.webp` |
| Pensando | Mensajes neutrales | `cashi_pensando.webp` | 60 a 160 | `avatar_pensando.webp` |
| Preocupado | Errores y avisos | `cashi_preocupado.webp` | 70 a 190 | `avatar_preocupado.webp` |
| Cool | Foto del Perfil, **en círculo** | `cashi_cool.webp` | 56 | `avatar_cool.webp` |

Las que van **en círculo** pierden las esquinas: deja a Cashi centrado y con aire alrededor.

## Reglas de los nombres

- Solo minúsculas, números y guion bajo (`_`). Sin espacios, tildes ni guiones (`-`).
- Debe coincidir **exactamente** con la tabla, con la extensión `.webp`.
- No guardes dos archivos con el mismo nombre y distinta extensión.
- Un nombre con mayúsculas o guiones hace que la app **no compile**.

## Si algo no se ve bien

| Qué pasa | Qué revisar |
|---|---|
| Sigue saliendo la imagen anterior | El nombre exacto, que esté en `res/raw/` y que hayas vuelto a instalar la app |
| La app no compila | Mayúsculas, espacios, tildes o guiones en el nombre, o dos archivos con el mismo nombre |
| Aparece un cuadro de fondo | El WebP no se exportó con transparencia |
| Se mueve lento o tarda en aparecer | Baja el peso: menos cuadros o menos calidad |
| No se anima | La animación se ve desde Android 9. En Android 8 y 8.1 sale el respaldo |
