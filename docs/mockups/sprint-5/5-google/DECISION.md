# Decisión · Fase 5 · Ingresar con Google

**Fecha:** 12/11/2026
**Elegida:** opción B, con cambios del desarrollador (`opciones.pdf`).

## Qué pidió el desarrollador
- La opción B, pero **sin cambiar Bienvenida** (la primera pantalla de la app).
- El botón de Google **solo en Iniciar sesión**; Crear cuenta tampoco cambia.
- Combinar el «Iniciar sesión» de la opción A y el de la opción B para que se vea bien.

## Qué se implementa
- **Iniciar sesión:**
  - correo, contraseña y "¿Olvidaste tu contraseña?";
  - el botón "Iniciar sesión", la línea "o" y **"Continuar con Google"**, con el logo de Google y el estado de carga;
  - "¿No tienes cuenta? Crea una".
- **Errores:**
  - canceló: no pasa nada;
  - sin internet: aviso abajo;
  - el correo ya tiene cuenta con contraseña: diálogo "Ese correo ya tiene cuenta";
  - falta `GOOGLE_WEB_CLIENT_ID`: aviso claro y la app no se cierra.
- **La primera vez con Google** (de la opción B): una pantalla corta, "¡Hola, Andres!", para confirmar el nombre y elegir la moneda (Bs o $us), con el botón "Empezar".
  - Se muestra una sola vez por cuenta.
  - Se usa cuando el perfil se creó hace menos de 15 minutos, la cuenta es de Google y todavía no se mostró en este teléfono.
- **Código:**
  - `compose-auth` (versión del BOM de Supabase) y `install(ComposeAuth) { googleNativeLogin(...) }`, solo si hay client ID;
  - el perfil se crea con el trigger `fn_crear_perfil`, que ya existe.
- Sin migración.
