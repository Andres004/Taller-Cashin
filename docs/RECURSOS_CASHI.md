# Recursos animados de Cashi

Guía para agregar las animaciones de Cashi sin tocar código. Basta con guardar cada archivo con el nombre exacto de la tabla: la app lo detecta sola. Mientras un archivo no exista, se sigue viendo la imagen fija de la última columna.

## Formato de todos los archivos

| Requisito | Valor |
|---|---|
| Formato | **WebP animado** (`.webp`). No GIF: tiene bordes dentados y pocos colores. |
| Fondo | Transparente |
| Tamaño | Unos 512 × 512 px |
| Duración | 2–3 segundos, en bucle |
| Peso | Menos de 500 KB |
| Carpeta | `app/src/main/res/raw/` |

## Archivos

| Momento | Dónde se ve | Nombre exacto del archivo | Se ve mientras no exista |
|---|---|---|---|
| Bienvenida / splash | Al abrir la app | `cashi_bienvenida.webp` | `logo_cashin.png` |
| Desafío cumplido | Celebración al cumplir un desafío | `cashi_desafio_cumplido.webp` | `avatar_celebrando.webp` |
| Meta completada | Detalle de una meta marcada como completada | `cashi_meta_completada.webp` | `avatar_celebrando.webp` |
| Subida de nivel | Aviso en Inicio al subir de nivel | `cashi_subida_nivel.webp` | `avatar_celebrando.webp` |
| Feliz | Mensajes y estados vacíos | `cashi_feliz.webp` | `avatar_feliz.webp` |
| Pensando | Mensajes neutrales | `cashi_pensando.webp` | `avatar_pensando.webp` |
| Preocupado | Errores y avisos | `cashi_preocupado.webp` | `avatar_pensando.webp` |
| Celebrando | Consejos y logros generales | `cashi_celebrando.webp` | `avatar_celebrando.webp` |
| Cool | Perfil | `cashi_cool.webp` | `avatar_cool.webp` |
| Metas vacío | Metas, cuando aún no hay ninguna | `cashi_metas_vacio.webp` | `avatar_feliz.webp` |
| Meta lograda | Detalle de una meta que llegó al 100 % | `cashi_meta_lograda.webp` | `avatar_celebrando.webp` |

## Reglas para que funcione

- El nombre va en minúsculas, sin espacios ni tildes, y debe coincidir exactamente.
- Si el archivo no tiene animación o está dañado, la app muestra la imagen fija.
- En Android 8 y 8.1 siempre se ve la imagen fija; la animación se ve de Android 9 en adelante.
- Si una animación no debe repetirse sin fin, expórtala con un número fijo de repeticiones.

Después de agregar un archivo, vuelve a instalar la app para verlo.
