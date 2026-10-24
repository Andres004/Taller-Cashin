# Cashin 🐶💸

Libreta digital de finanzas personales para Android: anotas gastos, ingresos, pagos esperados, metas de ahorro y deudas, y **Salchi**, el avatar, te acompaña con consejos y desafíos al estilo Duolingo.

> **Cashin no maneja dinero.** Solo guarda anotaciones: no hay saldos, cuentas ni transferencias.

Proyecto del Taller de Sistemas de Información · Equipo: Dabner · Andy · Carlos.

📘 **Antes de programar, lee [`docs/CASHIN_MAESTRO.md`](docs/CASHIN_MAESTRO.md)** (stack, arquitectura, reglas y base de datos).

---

## Stack

| Capa | Tecnología |
|---|---|
| App | Kotlin · Jetpack Compose (Material 3) · MVVM con ViewModel + StateFlow · Navigation Compose |
| Backend | Supabase: PostgreSQL + RLS, Auth y Edge Functions (no hay servidor propio) |
| Cliente | supabase-kt (auth, postgrest, functions) + Ktor OkHttp · kotlinx.serialization |
| IA (Sprint 3+) | Edge Function `ia-asistente` + API de Gemini |

## Requisitos

- **Android Studio** reciente (con soporte para AGP 9.1) y **JDK 17 o 21** (el que trae Android Studio sirve).
- Un emulador o celular con **Android 8.0 (API 26)** o superior.
- Las claves de Supabase (te las pasa Dabner).

## Primeros pasos

```bash
git clone https://github.com/Andres004/Taller-Cashin.git
cd Taller-Cashin
cp local.properties.example local.properties   # en Windows: copy local.properties.example local.properties
```

1. Abre `local.properties` y pega `SUPABASE_URL` y `SUPABASE_KEY`.
2. Abre la carpeta en Android Studio → espera el **Gradle Sync**.
3. Elige el módulo **app** y presiona ▶ **Run**.

Si falta `local.properties`, la app abre igual y muestra en pantalla qué configurar.

> ¿Primera vez configurando Supabase? Sigue [`docs/SUPABASE.md`](docs/SUPABASE.md).

### Comandos útiles

```bash
./gradlew :app:assembleDebug      # compila el APK de prueba
./gradlew :app:testDebugUnitTest  # pruebas unitarias (formatos, validaciones, errores)
```

## Estructura

```
app/src/main/java/org/ucb/cashin/
├── CashinApp.kt · MainActivity.kt
├── di/            AppContainer (cliente Supabase + repositorios) y fabricaDe { } para ViewModels
├── navigation/    Rutas, CashinRaiz (sesión → login o app), AuthNavHost, PrincipalNavHost (barra inferior)
├── model/         Una data class por tabla y vista de la BD + enums + ErrorApp
├── data/          Repositorios (uno por módulo). Todos usan llamada { } → Result<T>
├── ui/
│   ├── theme/       Colores, tipografía (Poppins), formas y espacios
│   ├── components/  Botones, campos, tarjetas, avatar, estados, selector de categoría...
│   ├── auth/        Login, crear cuenta, recuperar contraseña  ✅ Sprint 1
│   ├── inicio/      Splash e Inicio (bienvenida por ahora)
│   ├── registros/   Anotar e Historial (marcadores → Sprint 2)
│   ├── metas/ · perfil/ · categorias/ · planificados/ · desafios/ · deudas/
└── util/          Formato (Bs, fechas en hora de Bolivia), Validaciones, serializadores
supabase/
├── migrations/    001_esquema.sql · 002_rls.sql  (fuente de verdad de la BD)
├── tests/         Pruebas de RLS para PostgreSQL local
└── functions/     ia-asistente (Sprint 3)
docs/              CASHIN_MAESTRO.md · SUPABASE.md
```

## Qué incluye la base (Sprint 1)

- ✅ Proyecto Android limpio (sin KMP ni iOS), dependencias y claves por `local.properties`.
- ✅ Base de datos completa con RLS, validaciones, XP y racha automáticos, vistas de totales (probada).
- ✅ Crear cuenta, iniciar sesión, recuperar contraseña con código y cerrar sesión.
- ✅ Tema de Cashin, componentes reutilizables, avatar en 4 poses y barra inferior con 5 pestañas.
- ✅ Modelos de todas las tablas, formato de montos `Bs 1.250,50` y manejo de errores en español.
- ⏳ Ingreso con Google → Sprint 2 (HU-02). El botón ya está en el login.

## Cómo trabajamos

1. Toma una tarjeta de Trello y muévela a **En curso**.
2. Crea tu rama desde `main`: `feature/HU-06-anotar-gasto`.
3. Commits en español: `feat(registros): formulario de gasto rápido`.
4. Abre un **pull request** (la plantilla trae el checklist) y pide revisión. Nadie sube directo a `main`.

Detalle completo en el Maestro §10.
