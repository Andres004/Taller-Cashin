# CASHIN — Documento Maestro (SDD)

> **Spec-Driven Development.** Este documento es la fuente única de verdad del proyecto.
> Antes de escribir código, cada integrante (y cualquier asistente de IA que usemos) lee la sección que le corresponde.
> Si algo del código contradice este documento, **se corrige el código o se actualiza el documento mediante un pull request**; nunca se deja la contradicción.

| Campo | Valor |
|---|---|
| Proyecto | Cashin — libreta digital de finanzas personales con asistente de IA |
| Materia | Taller de Sistemas de Información |
| Equipo | Dabner · Andy · Carlos |
| Duración | 5 sprints de 1 semana; cada uno se entrega **el martes a las 17:30** (S2: 27/10 → S5: 17/11/2026) |
| Repositorio | https://github.com/Andres004/Taller-Cashin |
| Diseño | Figma "Cashin-Mobile" (fuente visual de todas las pantallas) |
| Tablero | Trello: [Cashin · Taller de Sistemas de Información](https://trello.com/b/8wOuJi2k/cashin-taller-de-sistemas-de-informaci%C3%B3n) |
| Backend | Proyecto Supabase `cashin` (São Paulo) — ✅ configurado |
| Paquete Android | `org.ucb.cashin` (módulo `app`) |
| Versión del documento | 1.2 — Sprint 1 cerrado, Sprint 2 en curso (24/10/2026) |

---

## 1. Contexto del producto

Cashin es una **app Android** donde la persona **anota** sus gastos, ingresos, pagos esperados, metas de ahorro y deudas. Un **avatar** (el perrito salchicha de Cashin) la acompaña al estilo Duolingo: da consejos, celebra rachas y propone desafíos.

Lo que diferencia a Cashin es un **asistente de IA** que:

1. **Propone desafíos semanales y mensuales** según el historial de la persona (por ejemplo: "Esta semana gasta máximo 150 Bs en comida fuera").
2. **Estima cuánto tiempo falta para una meta** (por ejemplo: "Al ritmo actual juntas los 2.500 Bs del celular en ~5 meses").
3. **Da consejos cortos** a través del avatar.

### 1.1 Principio no negociable

> **Cashin no maneja dinero. Solo guarda anotaciones.**
> No hay cuentas, saldos, transferencias ni conexión con bancos. Una meta o una deuda es solo información para el control personal.

### 1.2 Principios de diseño

- **Registro rápido:** anotar un gasto debe tomar menos de 10 segundos (monto → categoría → guardar).
- **Privacidad por usuario:** cada persona solo ve sus datos. Lo garantiza la base de datos (RLS), no solo la app.
- **La IA redacta, el código calcula:** los números (promedios, meses restantes, cumplimiento de desafíos) se calculan con fórmulas. La IA solo los explica y propone ideas.
- **Hora de Bolivia:** todo cálculo por día o por mes usa `America/La_Paz`.
- **Archivar en lugar de borrar** categorías, para no romper el historial.

---

## 2. Alcance (módulos)

| # | Módulo | Prioridad | Incluye |
|---|---|---|---|
| M1 | Cuenta | Alta | Registro e ingreso con correo o Google, recuperar contraseña, cerrar sesión, perfil |
| M2 | Registros | Alta | Anotar gasto o ingreso, editar, eliminar, historial con filtros |
| M3 | Categorías | Media | Categorías del sistema + propias, crear, archivar |
| M4 | Resumen | Alta | Totales del mes, gasto por categoría |
| M5 | Planificados | Media | Gastos e ingresos esperados (alquiler, sueldo) y convertirlos en registro |
| M6 | Metas | Alta | Crear meta, anotar ahorro o retiro, ver avance |
| M7 | Asistente IA | Alta | Estimación de metas, consejos, generación de desafíos |
| M8 | Gamificación | Alta | XP, nivel, racha, desafíos, avatar |
| M9 | Deudas | Baja | "Debo" y "Me deben", abonos, pendiente |

**Fuera de alcance:** iOS, versión web, exportar a Excel, notificaciones push, varias monedas con conversión, compartir datos entre usuarios.

---

## 3. Historias de usuario

Formato de ID: `HU-XX`. Cada HU es una tarjeta en Trello y una rama en GitHub.

| ID | Módulo | Historia | Criterios de aceptación |
|---|---|---|---|
| HU-01 | M1 | Como usuario quiero registrarme con correo y contraseña | Correo válido, contraseña ≥ 8 caracteres, se crea mi perfil automáticamente |
| HU-02 | M1 | Quiero ingresar con mi cuenta de Google | Un toque abre el selector de cuentas; al volver estoy dentro |
| HU-03 | M1 | Quiero recuperar mi contraseña | Recibo un código numérico por correo y con él pongo una nueva contraseña dentro de la app |
| HU-04 | M1 | Quiero cerrar sesión | Vuelvo a la pantalla de ingreso y no puedo ver datos |
| HU-05 | M1 | Quiero ver y editar mi perfil | Puedo cambiar nombre y moneda; veo XP, nivel y racha |
| HU-06 | M2 | Quiero anotar un gasto rápido | Monto > 0, categoría obligatoria, medio de pago opcional (por defecto Efectivo), fecha por defecto ahora |
| HU-07 | M2 | Quiero anotar un ingreso | Igual que HU-06 con categorías de ingreso |
| HU-08 | M2 | Quiero editar o eliminar una anotación | Cambios visibles en historial y resumen |
| HU-09 | M2 | Quiero ver mi historial | Lista por fecha descendente, agrupada por día |
| HU-10 | M2 | Quiero filtrar mi historial | Por tipo, categoría y rango de fechas |
| HU-11 | M3 | Quiero crear mis propias categorías | Nombre, tipo, ícono y color; no se repite el nombre |
| HU-12 | M3 | Quiero archivar una categoría | Desaparece del selector pero sigue en el historial |
| HU-13 | M4 | Quiero ver los totales del mes | Gastos, ingresos y diferencia del mes actual |
| HU-14 | M4 | Quiero ver en qué categoría gasto más | Gráfico o lista con porcentaje por categoría |
| HU-15 | M5 | Quiero anotar un pago o ingreso esperado | Nombre, monto, frecuencia, próxima fecha |
| HU-16 | M5 | Quiero convertir un planificado en registro | Un toque crea el registro y mueve la próxima fecha |
| HU-17 | M6 | Quiero crear una meta de ahorro | Nombre, monto objetivo, fecha límite opcional |
| HU-18 | M6 | Quiero anotar un ahorro o retiro en la meta | El retiro no supera lo ahorrado |
| HU-19 | M6 | Quiero ver el avance de la meta | Barra de progreso con monto y porcentaje |
| HU-20 | M7 | Quiero saber cuánto me falta para mi meta | Estimación en meses + explicación del avatar |
| HU-21 | M7 | Quiero recibir consejos del avatar | Consejo corto basado en mis datos del mes en la pantalla de inicio |
| HU-22 | M8 | Quiero recibir desafíos semanales y mensuales | La IA propone 3 opciones y yo acepto una |
| HU-23 | M8 | Quiero ver si cumplí un desafío | Al terminar el periodo se marca cumplido o fallido y gano XP |
| HU-24 | M8 | Quiero ganar XP y mantener una racha | XP por anotar; racha por días consecutivos anotando |
| HU-25 | M9 | Quiero anotar una deuda (debo / me deben) | Contraparte, monto total, fecha límite opcional |
| HU-26 | M9 | Quiero anotar abonos a una deuda | El abono no supera lo pendiente; al llegar a 0 queda pagada |

---

## 4. Stack tecnológico

| Capa | Tecnología | Para qué |
|---|---|---|
| Lenguaje | **Kotlin** | Toda la app |
| UI | **Jetpack Compose** + Material 3 | Pantallas declarativas |
| Navegación | **Navigation Compose** | Moverse entre pantallas |
| Estado | **ViewModel + StateFlow + Coroutines** | Patrón MVVM |
| Backend | **Supabase** | Base de datos, login y función de IA; no escribimos servidor |
| Cliente Supabase | **supabase-kt** (`auth-kt`, `postgrest-kt`, `functions-kt`) + motor Ktor OkHttp | Hablar con Supabase |
| Serialización | **kotlinx.serialization** | Objetos Kotlin ⇄ JSON |
| Base de datos | **PostgreSQL** (de Supabase) con **RLS** | Datos y privacidad |
| Login con Google (Sprint 3) | **Credential Manager** vía `compose-auth` (repositorio `supabase-kt-plugins`) | Ingreso con Google nativo |
| IA | **Supabase Edge Function** (TypeScript) + **API de Gemini** | Desafíos, consejos, estimación |
| Avatar | **Lottie Compose** (animaciones) o PNG | Avatar animado |
| Gráficos | Compose puro (Canvas) o **Vico** | Gasto por categoría |
| Control de versiones | Git + GitHub (pull requests) | Trabajo en equipo |
| Tablero | Trello | Tareas y plazos |

**Versiones** (todas en `gradle/libs.versions.toml`; se cambian solo en un PR que compile):

| Pieza | Versión |
|---|---|
| Android Gradle Plugin | 9.1.1 (trae Kotlin integrado) |
| Kotlin | 2.4.20 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 26 |
| Compose BOM | 2025.08.00 (Material 3) |
| Navigation Compose | 2.9.3 (rutas con tipos) |
| Lifecycle (ViewModel) | 2.9.2 |
| supabase-kt | 3.8.0 (BOM) |
| Ktor | 3.4.1 |
| kotlinx.serialization | 1.9.0 |

**Inyección de dependencias:** sin Hilt ni Koin. Un `AppContainer` manual + `fabricaDe { }` (ver §6.3), que es más simple de entender y de defender.

**No se usa:** Kotlin Multiplatform, iOS, Spring Boot, Koin, Room. Agregar una librería nueva requiere avisar en el grupo y actualizar esta tabla.

### 4.1 Por qué este stack (para defender en la presentación)

- **Un solo lenguaje en la app** (Kotlin) y una función pequeña en TypeScript.
- **Supabase** nos da PostgreSQL real (relacional, el mismo SQL del curso), login con Google y recuperación de contraseña ya resueltos. Eso elimina un backend completo con JWT, BCrypt, sesiones y servidor de correo.
- **RLS** (Row Level Security) hace que la propia base de datos impida ver datos ajenos, aunque la app tuviera un error.
- **La clave de la IA nunca va en el celular:** vive como secreto en la Edge Function.

---

## 5. Arquitectura

```
┌──────────────────────── App Android (Kotlin) ────────────────────────┐
│  Pantalla (Compose) → ViewModel (StateFlow) → Repositorio            │
└───────────────────────────────┬──────────────────────────────────────┘
                                │ HTTPS + token del usuario (automático)
┌───────────────────────────────▼──────────────────────────────────────┐
│                              SUPABASE                                 │
│  Auth (correo, Google)   PostgreSQL + RLS + vistas   Edge Function    │
│                                                      "ia-asistente"   │
└──────────────────────────────────────────────────────┬───────────────┘
                                                       │ HTTPS (clave secreta)
                                                  ┌────▼────┐
                                                  │ Gemini  │
                                                  └─────────┘
```

**Flujo de un registro ("gasté 20 Bs en almuerzo con QR"):**
1. La pantalla valida monto > 0 y categoría.
2. El ViewModel llama a `RegistroRepository.crear(...)`.
3. El repositorio hace `insert` en la tabla `registro` con supabase-kt.
4. PostgreSQL verifica RLS, los CHECK y las claves foráneas, y un trigger suma XP y actualiza la racha.
5. La app actualiza el historial y el resumen.

**Flujo de la IA:**
1. La app llama `supabase.functions.invoke("ia-asistente", { accion, ... })`.
2. La función lee los datos **del usuario que la llamó** (con su token, así que RLS sigue aplicando).
3. La función **calcula** los números con código.
4. La función envía a Gemini solo un **resumen** (nunca correo ni nombre real) y pide respuesta en JSON.
5. Valida el JSON y lo devuelve a la app.

---

## 6. Estructura del repositorio

Paquete base: **`org.ucb.cashin`** · módulo único **`app`** · código en `app/src/main/java/org/ucb/cashin/`.

```
Taller-Cashin/
├── app/
│   ├── build.gradle.kts                 # dependencias + claves de local.properties → BuildConfig
│   └── src/
│       ├── main/java/org/ucb/cashin/
│       │   ├── CashinApp.kt             # Application: crea el AppContainer (si hay claves)
│       │   ├── MainActivity.kt          # tema + CashinRaiz (o aviso de configuración)
│       │   ├── di/
│       │   │   ├── AppContainer.kt      # cliente Supabase + un repositorio por módulo
│       │   │   └── FabricaViewModel.kt  # fabricaDe { } para crear ViewModels
│       │   ├── navigation/
│       │   │   ├── Rutas.kt             # rutas con tipos (Rutas.Login, Rutas.Inicio…)
│       │   │   ├── CashinRaiz.kt        # sesión → Splash / AuthNavHost / PrincipalNavHost
│       │   │   ├── AuthNavHost.kt       # login, crear cuenta, recuperar
│       │   │   └── PrincipalNavHost.kt  # barra inferior + pantallas con sesión
│       │   ├── model/
│       │   │   ├── Tablas.kt            # una data class por tabla (+ Nuevo* para insertar)
│       │   │   ├── Vistas.kt            # una data class por vista (totales)
│       │   │   ├── Enums.kt             # valores de los CHECK (GASTO/INGRESO, QR…)
│       │   │   └── ErrorApp.kt          # errores que la app sabe explicar
│       │   ├── data/
│       │   │   ├── Errores.kt           # llamada { } → Result<T> y traducción de errores
│       │   │   ├── AuthRepository.kt    # ✅ Sprint 1
│       │   │   └── …Repository.kt       # uno por módulo (Registro, Categoria, Meta…)
│       │   ├── ui/
│       │   │   ├── theme/               # Color.kt, Type.kt (Poppins), Theme.kt (formas, Espacio)
│       │   │   ├── components/          # piezas reutilizables (ver §9)
│       │   │   ├── auth/                # ✅ Login, CrearCuenta, Recuperar (+ ViewModels)
│       │   │   ├── inicio/              # Splash ✅, Inicio (bienvenida → HU-13)
│       │   │   ├── registros/           # Anotar, Historial (marcadores → Sprint 2)
│       │   │   ├── metas/ · perfil/     # marcador / cerrar sesión ✅
│       │   │   └── categorias/ · planificados/ · desafios/ · deudas/   (vacías)
│       │   └── util/                    # Formato, Validaciones, Serializadores
│       ├── main/res/
│       │   ├── drawable-nodpi/          # logo y avatar: feliz, pensando, celebrando, cool
│       │   ├── font/                    # Poppins regular, medium, bold
│       │   ├── raw/                     # animaciones Lottie (Sprint 5)
│       │   └── values/strings.xml       # TODOS los textos
│       └── test/java/org/ucb/cashin/    # pruebas unitarias (JUnit)
├── supabase/
│   ├── migrations/                      # 001_esquema.sql, 002_rls.sql, 003_…
│   ├── tests/                           # pruebas de RLS en PostgreSQL local
│   └── functions/ia-asistente/          # Edge Function (Sprint 3)
├── docs/                                # este documento, SUPABASE.md
├── .github/pull_request_template.md     # checklist de "Hecho" en cada PR
├── local.properties.example
└── gradle/libs.versions.toml            # versiones de todas las librerías
```

### 6.1 Reglas por capa

| Capa | Puede | No puede |
|---|---|---|
| `ui/*Screen.kt` | Dibujar, leer `uiState`, enviar eventos al ViewModel | Llamar a Supabase o a un repositorio |
| `ui/*ViewModel.kt` | Validar, llamar repositorios, exponer `StateFlow<UiState>`; guardar ids `@StringRes` y `ErrorApp` | Usar `Context`, `@Composable` o textos escritos a mano |
| `data/*Repository.kt` | Llamar a Supabase dentro de `llamada { }` y devolver `Result<T>` | Mantener estado de pantalla |
| `model/` | Data classes `@Serializable` y cálculos simples (`nivel`, `progreso`) | Llamar a red |

### 6.2 Patrón de una pantalla (copiar de `ui/auth/LoginScreen.kt`)

Cada pantalla tiene **3 piezas**: `XUiState` + `XViewModel` + `XScreen`/`XContenido`.

```kotlin
// 1. Estado: todo lo que la pantalla necesita para dibujarse
data class NuevoRegistroUiState(
    val monto: String = "",
    val categoriaId: String? = null,
    @StringRes val errorMonto: Int? = null,     // texto de validación (strings.xml)
    val error: ErrorApp? = null,                // error del servidor
    val cargando: Boolean = false,
    val guardado: Boolean = false,
)

// 2. ViewModel: valida y llama al repositorio
class NuevoRegistroViewModel(private val repo: RegistroRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(NuevoRegistroUiState())
    val uiState: StateFlow<NuevoRegistroUiState> = _uiState.asStateFlow()

    fun onMontoCambio(v: String) = _uiState.update { it.copy(monto = v, errorMonto = null) }

    fun guardar() {
        val monto = Formato.parsearMonto(_uiState.value.monto)
        if (monto == null || monto <= BigDecimal.ZERO) {
            _uiState.update { it.copy(errorMonto = R.string.validacion_monto) }; return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            repo.crear(NuevoRegistro(categoriaId = …, tipo = TipoRegistro.GASTO, monto = monto))
                .onSuccess { _uiState.update { it.copy(cargando = false, guardado = true) } }
                .onFailure { e -> _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) } }
        }
    }

    companion object {
        val Factory = fabricaDe { NuevoRegistroViewModel(it.registroRepository) }
    }
}

// 3. Pantalla: XScreen conecta el ViewModel; XContenido solo dibuja (y tiene @Preview)
@Composable
fun NuevoRegistroScreen(viewModel: NuevoRegistroViewModel = viewModel(factory = NuevoRegistroViewModel.Factory)) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    NuevoRegistroContenido(estado = estado, onMontoCambio = viewModel::onMontoCambio, onGuardar = viewModel::guardar)
}
```

**Repositorio** (copiar de `data/AuthRepository.kt`):

```kotlin
class RegistroRepository(private val supabase: SupabaseClient) {
    suspend fun crear(nuevo: NuevoRegistro): Result<Registro> = llamada {
        supabase.from("registro").insert(nuevo) { select() }.decodeSingle<Registro>()
    }
    suspend fun listar(): Result<List<Registro>> = llamada {
        supabase.from("registro").select { order("fecha_hora", Order.DESCENDING) }.decodeList<Registro>()
    }
}
```

Reglas:
- Repositorios **siempre** con `llamada { }` → devuelven `Result<T>` y convierten errores en `ErrorApp`.
- Montos en **`BigDecimal`** (el serializador ya está en los modelos). Nunca `Float`/`Double` para dinero.
- Texto que escribe el usuario → `Formato.parsearMonto(...)`; texto que se muestra → `Formato.monto(...)`.
- Fechas por día o mes: `Formato.hoy()`, `Formato.fechaDe(instante)` (hora de Bolivia).
- No hace falta enviar `usuario_id` al insertar: la base usa `auth.uid()` por defecto. **Excepción:** `categoria` (las del sistema tienen `usuario_id` NULL), donde se envía `authRepository.usuarioIdActual()`.

### 6.3 AppContainer

`di/AppContainer.kt` crea **un solo** cliente de Supabase (`Auth`, `Postgrest`, `Functions`, JSON que ignora columnas desconocidas). Para un módulo nuevo:

```kotlin
// en AppContainer
val registroRepository by lazy { RegistroRepository(supabase) }
```

La sesión se guarda sola en el celular (supabase-kt) y se renueva automáticamente. `CashinRaiz` escucha `AuthRepository.estadoSesion`: al iniciar o cerrar sesión la app cambia de flujo **sin navegar a mano**.

### 6.4 Secretos y configuración

| Valor | Dónde vive | ¿Se sube a GitHub? |
|---|---|---|
| `SUPABASE_URL`, `SUPABASE_KEY` (clave pública anon/publishable) | `local.properties` → `BuildConfig` | **No** |
| `GOOGLE_WEB_CLIENT_ID` | `local.properties` → `BuildConfig` | No |
| `GEMINI_API_KEY`, `GEMINI_MODEL` | Secretos de Supabase (`supabase secrets set`) | **Nunca** |

Plantilla: `local.properties.example`. Andy comparte los valores por mensaje privado. La clave pública puede ir en la app porque **RLS** protege los datos; la `service_role` nunca se usa en la app.

---

## 7. Base de datos

**Fuente de verdad:** los archivos de `supabase/migrations/`. Este documento solo los resume; si hay diferencia, manda el SQL.

| Archivo | Contenido | Estado |
|---|---|---|
| `001_esquema.sql` | 9 tablas, índices, validaciones, XP/racha automáticos, 4 vistas, categorías del sistema | ✅ probado |
| `002_rls.sql` | Políticas RLS: cada usuario solo ve y cambia lo suyo | ✅ probado |
| `003_gamificacion.sql` | Funciones de desafíos y metas (§7.2) | Sprint 4 |

Cómo ejecutarlos y probarlos: `docs/SUPABASE.md`.

**Cambios respecto al modelo v5:**
- `usuario`, `sesion` y `token_usuario` → los reemplaza **Supabase Auth** (`auth.users`) + tabla `perfil` (se crea sola con un trigger al registrarse).
- `medio_pago` (tabla) → **columna** `medio_pago` con CHECK en `registro` y `registro_planificado`.
- `etiqueta` y `registro_etiqueta` → eliminadas (las categorías bastan).
- `anotacion_meta` → se llama `aporte_meta`.
- Triggers de propietario → reemplazados por **políticas RLS**.
- **Nuevo:** XP, nivel y racha en `perfil`; tabla `desafio`; vista `v_gasto_categoria_mes`.
- Se mantienen: FK compuesta `(categoria_id, tipo)`, validación de abonos y retiros, vistas en hora de Bolivia.

### Tablas

| Tabla | Para qué | Modelo Kotlin |
|---|---|---|
| `perfil` | Nombre, moneda, avatar, XP, racha (1 por usuario) | `Perfil`, `PerfilEditable` |
| `categoria` | Del sistema (`usuario_id` NULL) o propias | `Categoria`, `NuevaCategoria` |
| `registro` | Gasto o ingreso anotado | `Registro`, `NuevoRegistro` |
| `registro_planificado` | Pago o ingreso esperado | `RegistroPlanificado`, `NuevoPlanificado` |
| `meta` / `aporte_meta` | Meta de ahorro y sus ahorros/retiros | `Meta`, `AporteMeta` (+ `Nuevo*`) |
| `deuda` / `abono_deuda` | "Debo" / "Me deben" y sus abonos | `Deuda`, `AbonoDeuda` (+ `Nuevo*`) |
| `desafio` | Desafíos semanales/mensuales (solo lectura en la app) | `Desafio` |

| Vista | Devuelve | Modelo |
|---|---|---|
| `v_totales_mes` | Gastos, ingresos y cantidad por mes | `TotalesMes` |
| `v_gasto_categoria_mes` | Gasto por categoría y mes | `GastoCategoriaMes` |
| `v_avance_meta` | Total ahorrado por meta | `AvanceMeta` |
| `v_deuda` | Pendiente por deuda | `ResumenDeuda` |

### Reglas que garantiza la base de datos (aunque la app falle)

- Montos `NUMERIC(14,2)` y siempre > 0.
- El tipo de un registro (GASTO/INGRESO) coincide con el de su categoría (FK compuesta).
- Un retiro no supera lo ahorrado en la meta; un abono no supera lo pendiente de la deuda.
- Cada registro suma **+10 XP** (solo los 5 primeros del día) y actualiza la **racha** (hora Bolivia).
- La app **no puede** editar `xp` ni la racha, ni crear desafíos: lo hacen triggers y funciones del servidor.

### 7.1 Seguridad (RLS)

- Cada tabla con `usuario_id` solo deja ver y modificar filas donde `usuario_id = auth.uid()`.
- `categoria`: se ven las del sistema y las propias; solo se modifican las propias.
- `registro` y `registro_planificado`: además, la categoría usada debe ser del sistema o propia.
- `aporte_meta` y `abono_deuda`: el dueño se verifica por la tabla padre.
- Las vistas usan `security_invoker`, así que también respetan RLS.
- Probado en `supabase/tests/pruebas_rls.sql`: un usuario no ve, no usa ni borra datos de otro.

### 7.2 Funciones SQL de gamificación (Sprint 4) — `003_gamificacion.sql`

Contratos a implementar en el Sprint 4 (responsable por definir); la app los llama con `supabase.postgrest.rpc(...)`:

| Función | Qué hace |
|---|---|
| `aceptar_desafio(p_titulo, p_descripcion, p_periodo, p_regla, p_categoria_id, p_monto_limite, p_dias_objetivo)` | Inserta el desafío para `auth.uid()` con fechas del periodo actual (semana lunes–domingo o mes calendario, hora Bolivia). Rechaza si ya hay un desafío ACTIVO del mismo periodo. XP: 50 semanal, 150 mensual. |
| `descartar_desafio(p_id)` | Cambia a `DESCARTADO` solo si es del usuario y está ACTIVO. |
| `evaluar_desafios()` | Para cada desafío ACTIVO de `auth.uid()`: **LIMITE_CATEGORIA / LIMITE_TOTAL** → si el gasto del periodo ya supera `monto_limite`, pasa a FALLIDO; si `fecha_fin` ya pasó y no lo superó, pasa a CUMPLIDO. **DIAS_REGISTRO** → si llegó a `dias_objetivo` días distintos con registro, pasa a CUMPLIDO; si `fecha_fin` pasó sin lograrlo, FALLIDO. Al pasar a CUMPLIDO suma `xp_recompensa` al perfil. Devuelve los desafíos que cambiaron. La app la llama al abrir Inicio. |
| `completar_meta(p_meta_id)` | Si el total ahorrado ≥ objetivo, marca COMPLETADA y suma 100 XP (una sola vez). |

Todas `security definer set search_path = public` y siempre filtran por `auth.uid()`.

### 7.3 Reglas de XP y nivel

| Acción | XP |
|---|---|
| Anotar un registro | +10 (máx. 5 por día) |
| Cumplir desafío semanal | +50 |
| Cumplir desafío mensual | +150 |
| Completar una meta | +100 |

**Nivel** = `xp / 200 + 1` (división entera), calculado en la app. **Racha** = días consecutivos con al menos un registro (hora Bolivia). Si hoy aún no anotó, la racha se muestra "en riesgo".

---

## 8. Asistente de IA — Edge Function `ia-asistente`

Archivo: `supabase/functions/ia-asistente/index.ts`. Una sola función con un campo `accion`.

### 8.1 Seguridad

- Crea el cliente de Supabase **con el header `Authorization` de la petición**, para leer como el usuario (RLS aplica).
- A Gemini se le envía **solo** el resumen numérico: nunca correo, nombre, contrapartes de deudas ni notas libres.
- Límite: máximo **10 llamadas por usuario por día** (contar en memoria o en una tabla simple `uso_ia`, opcional).
- Si Gemini falla o devuelve JSON inválido, la función responde un **mensaje de respaldo** fijo (la app nunca se rompe por la IA).

### 8.2 Resumen que se calcula antes de llamar a la IA

```json
{
  "moneda": "BOB",
  "ingreso_promedio_mensual": 3200.00,
  "gasto_promedio_mensual": 2850.00,
  "ahorro_promedio_mensual": 350.00,
  "meses_con_datos": 3,
  "top_categorias_gasto": [
    {"categoria_id": "…", "nombre": "Alimentación", "promedio_mensual": 900.00, "porcentaje": 31.6}
  ],
  "gasto_fin_de_semana_pct": 42.0,
  "racha_actual": 4,
  "desafios_ultimo_mes": {"cumplidos": 1, "fallidos": 2}
}
```

Ventana de cálculo: últimos 3 meses completos + mes actual. Si hay menos de 2 semanas de datos, no se llama a la IA y se responde "Anota unos días más para que pueda conocerte".

### 8.3 Acciones

**`accion: "estimar_meta"`** — entrada `{ meta_id }`

1. Código: `faltante = monto_objetivo − total_ahorrado`; `ahorro_mensual = max(ahorro_promedio_mensual, promedio mensual de aportes a metas)`.
2. Si `ahorro_mensual ≤ 0` → `alcanzable = false`.
3. Si no → `meses = ceil(faltante / ahorro_mensual)`; si hay `fecha_limite`, comparar.
4. IA: recibe `{faltante, ahorro_mensual, meses, fecha_limite, top_categorias_gasto}` y devuelve:

```json
{ "mensaje": "≤ 220 caracteres, tono del avatar",
  "sugerencia": "≤ 160 caracteres: dónde recortar",
  "ahorro_extra_sugerido": 150.00 }
```

Respuesta final a la app: `{ meses, alcanzable, ahorro_mensual, mensaje, sugerencia, ahorro_extra_sugerido }`. **`meses` siempre sale del código, nunca de la IA.**

**`accion: "consejo"`** — sin entrada. IA devuelve `{ "consejo": "≤ 200 caracteres", "emocion": "feliz|neutral|preocupado|celebrando" }`. La `emocion` decide la animación del avatar. Cachear 1 consejo por día en la app.

**`accion: "proponer_desafios"`** — entrada `{ periodo: "SEMANAL" | "MENSUAL" }`. IA devuelve exactamente 3:

```json
{ "desafios": [
  { "titulo": "≤ 60 caracteres",
    "descripcion": "≤ 200 caracteres",
    "regla": "LIMITE_CATEGORIA | LIMITE_TOTAL | DIAS_REGISTRO",
    "categoria_id": "uuid o null",
    "monto_limite": 600.00,
    "dias_objetivo": null }
]}
```

La función **valida** cada desafío: `regla` permitida; `categoria_id` debe ser de `top_categorias_gasto`; `monto_limite` entre 60 % y 95 % del promedio de esa categoría (o del total) para el periodo; `dias_objetivo` entre 3 y 7 (semanal) o 10 y 25 (mensual). Lo que no cumple se corrige o se descarta. La app muestra las 3 opciones y el usuario acepta una con `aceptar_desafio(...)`.

### 8.4 Prompt base (system)

```
Eres "Salchi", el avatar de Cashin, una app boliviana de anotaciones de finanzas personales.
Hablas en español de Bolivia, cercano, positivo y breve, como un amigo que anima (estilo Duolingo).
Cashin NO maneja dinero: nunca sugieras transferir, invertir, pedir préstamos ni productos financieros.
Usa solo los números que te doy; no inventes cifras. Montos en Bs.
Responde ÚNICAMENTE con JSON válido que siga el esquema indicado, sin texto adicional.
```

Se usa el modo de respuesta JSON de Gemini (`responseMimeType: "application/json"` con esquema) para reducir errores.

---

## 9. Diseño (UI)

- **Fuente visual:** Figma "Cashin-Mobile". Si una pantalla no existe en Figma, se sigue el estilo de las existentes y se avisa en el PR.
- **Colores** (en `ui/theme/Color.kt`, tomados de los prototipos): crema `#FCF7EA` (fondo), verde oscuro `#2B5333` (textos), verde `#6D9A5A` (botones), verde claro `#B4D5AA` (progreso), naranja `#E08A2C` (avatar, XP, racha), rojo `#C0473A` (gastos y errores), verde `#2E7D32` (ingresos). En pantallas se usa `MaterialTheme.colorScheme.*` o `CashinColores.*`; **prohibido** escribir `Color(0x…)` en una pantalla.
- **Tipografía:** Poppins (regular, medium, bold) en `ui/theme/Type.kt`. Montos grandes con `displaySmall/Medium`, títulos con `headlineMedium`.
- **Espacios:** `Espacio.s/m/l…` y margen lateral `Espacio.pantalla` (24 dp).
- **Componentes compartidos** (`ui/components/`, ya implementados):

| Componente | Uso |
|---|---|
| `CashinBoton` | Principal, secundario (borde) o texto; con `cargando` e `icono` |
| `CashinCampoTexto` | Etiqueta arriba, error abajo, modo contraseña con ojo |
| `CashinCampoMonto` | Monto grande "Bs 20,50" que solo acepta 2 decimales |
| `CashinTarjeta` | Contenedor con borde suave; tocable con `onClick` |
| `SelectorCategoria` | Grilla de categorías con ícono y color (`iconoCategoria`, `colorCategoria`) |
| `AvatarImagen` / `AvatarBurbuja` | Salchi con emoción `FELIZ, NEUTRAL, PREOCUPADO, CELEBRANDO, COOL` + globo de texto |
| `BarraProgreso` | Metas, desafíos y XP |
| `EstadoCargando` / `EstadoVacio` / `EstadoError` | Los estados obligatorios de cada pantalla |
| `CashinTopBar` | Título + botón atrás + acciones |
| `PantallaEnConstruccion` | Marcador temporal; se borra cuando la pantalla real existe |
| `ErrorApp.mensaje()` | Texto en español para cualquier error del repositorio |

- **Navegación inferior:** Inicio · Historial · (+) Anotar · Metas · Perfil. Desafíos y Deudas se abren desde Inicio y Perfil.
- **Cada pantalla tiene 4 estados:** cargando, vacío, con datos, error.
- **Textos en español** en `res/values/strings.xml`.
- **Montos:** formato `Bs 1.250,50` (helper en `util/Formato.kt`).

---

## 10. Forma de trabajo

### 10.1 Sprints

Cada sprint dura una semana y se **entrega el martes a las 17:30** (hora Bolivia).

| Sprint | Entrega | Responsable | Objetivo | Estado |
|---|---|---|---|---|
| S1 | sáb 24/10 | Andy | Base: repo, Supabase, esquema + RLS, login, tema, componentes, navegación | ✅ Hecho |
| S2 | **mar 27/10 · 17:30** | **Carlos** (revisa Andy) | Anotar (HU-06/07), historial (HU-09), editar/eliminar (HU-08), Inicio con totales (HU-13), perfil (HU-05) | 🔨 En curso |
| S3 | mar 03/11 · 17:30 | por definir | Google (HU-02), filtros (HU-10), categorías (HU-11/12), planificados (HU-15/16), metas (HU-17–19), Edge Function base (T-11) | Backlog |
| S4 | mar 10/11 · 17:30 | por definir | IA: estimación (HU-20) y consejos (HU-21); desafíos (HU-22/23); XP y racha (HU-24); `003_gamificacion.sql` | Backlog |
| S5 | mar 17/11 · 17:30 | por definir | Deudas (HU-25/26), gasto por categoría (HU-14), avatar animado, pruebas, APK y presentación | Backlog |

El responsable de cada sprint se decide en su planificación. Las fechas de S3 a S5 son tentativas.

**Ritmo de cada semana:**
- **Miércoles:** planificación (20 min). Se eligen las tarjetas del backlog, se asigna responsable y se pasan a la lista del sprint.
- **Todos los días:** daily por el grupo de WhatsApp **antes de las 22:00**, 3 líneas: *ayer hice / hoy haré / me bloquea*.
- **Martes 15:30:** pull request del sprint abierto.
- **Martes 17:00:** revisión y aprobación del PR por otro integrante.
- **Martes 17:30:** entrega (todo unido a `main`) y retro de 10 minutos.

Cada sprint tiene su prompt de trabajo en `docs/sprints/` (por ejemplo `PROMPT_SPRINT2_CARLOS.md`), que se pega en el asistente de IA para hacer las tareas en orden sin romper la base.

### 10.2 Trello

Tablero: [Cashin · Taller de Sistemas de Información](https://trello.com/b/8wOuJi2k/cashin-taller-de-sistemas-de-informaci%C3%B3n).

| Lista | Para qué |
|---|---|
| 📌 Cómo trabajamos | Maestro, calendario, flujo de trabajo y Definición de "Hecho" |
| 📋 Historias de usuario (backlog) | Historias y tareas de los próximos sprints, marcadas `[S3]`, `[S4]`, `[S5]` |
| 🏃 Sprint N · Por hacer | Tarjetas del sprint actual con responsable y fecha límite |
| 🔨 En curso | Máximo **2 por persona** |
| 👀 En revisión (PR abierto) | Con el enlace del PR |
| ✅ Hecho | PR aprobado y unido a `main` |
| 🚧 Bloqueado | Con un comentario de qué falta |

**Formato de tarjeta:** `[Responsable] HU-XX · título` (o `[S3] HU-XX · título` si aún no tiene responsable). Cada tarjeta lleva criterios de aceptación, archivos a tocar, cómo probarla y **fecha límite**. Colores: naranja = Carlos, azul = Andy, verde = meta del sprint.

### 10.3 Git y GitHub

- `main` siempre compila y funciona. **Protegida:** nadie hace push directo.
- Una rama por tarjeta: `feature/HU-06-anotar-gasto`, `fix/HU-09-orden-historial`, `chore/T-01-setup`.
- Commits (Conventional Commits en español):
  `feat(registros): formulario de gasto rápido` · `fix(auth): error al volver de Google` · `docs: actualizar maestro` · `chore: dependencias`.
- Antes de abrir PR: `git pull origin main`, resolver conflictos, compilar y probar en el emulador.
- **PR:** título = título de la tarjeta; descripción con *qué hace*, *cómo probarlo* y **captura de pantalla** si hay UI.
- **Revisión:** 1 aprobación obligatoria de otro integrante. Revisar en menos de 24 h.
- Se une con **"Squash and merge"** y se borra la rama.
- Cambios en la base de datos: **siempre** como nuevo archivo en `supabase/migrations/` (`004_...sql`); nunca editar a mano en el panel sin dejar el SQL en el repo.

### 10.4 Definición de "Hecho"

Una tarjeta pasa a **Hecho** solo si:
- [ ] Cumple todos los criterios de aceptación de la HU.
- [ ] Sigue la estructura y las reglas por capa de este documento.
- [ ] Tiene los 4 estados (cargando, vacío, datos, error) si es pantalla.
- [ ] Se probó en el emulador con un usuario real de Supabase.
- [ ] No tiene colores ni textos sueltos (usa theme y strings).
- [ ] No sube claves ni `local.properties`.
- [ ] El PR fue aprobado y unido a `main`.

### 10.5 Cómo pedir ayuda

Si llevas **más de 2 horas trabado**, avisa en el grupo y mueve la tarjeta a `Bloqueado` con una nota. Pedir ayuda temprano es parte del trabajo.

---

## 11. Instrucciones para asistentes de IA (Claude, Copilot, Cursor, etc.)

Cuando usen una IA para programar, **pegar al inicio esta sección + la sección del módulo** en el que trabajan:

```
Contexto: Proyecto Cashin (ver CASHIN_MAESTRO.md). App Android en Kotlin + Jetpack Compose,
MVVM con ViewModel y StateFlow, Supabase con supabase-kt 3.x (Auth, Postgrest, Functions),
kotlinx.serialization, sin Hilt ni Koin (AppContainer + fabricaDe). Paquete base org.ucb.cashin.
Copia el patrón de ui/auth/LoginScreen.kt, LoginViewModel.kt y data/AuthRepository.kt.
Reglas:
- Respeta la estructura de carpetas y las reglas por capa (UI no llama a Supabase).
- Repositorios devuelven Result<T> usando llamada { }. Errores como ErrorApp. Montos en BigDecimal, nunca Double.
- Usa los modelos de model/Tablas.kt y model/Vistas.kt y los componentes de ui/components.
- Colores y tipografía solo desde ui/theme; textos en strings.xml (español).
- Fechas por día/mes en zona America/La_Paz.
- La app solo guarda anotaciones: no hay saldos ni transferencias.
- No inventes tablas ni columnas: usa exactamente el esquema de la sección 7.
- No agregues librerías que no estén en la sección 4 sin preguntarlo.
Tarea: [describir la HU y sus criterios de aceptación]
```

Toda salida de la IA **se revisa y se entiende** antes de subirla. En la defensa, cada integrante debe poder explicar su código.

---

## 12. Riesgos y planes de respaldo

| Riesgo | Plan |
|---|---|
| Login con Google no funciona a tiempo (SHA-1, client ID) | El correo y la contraseña son el camino principal; Google se terminó de mover al S3 para no frenar el S2 |
| La IA responde mal o se cae | Mensajes de respaldo fijos + validación de JSON; los números nunca dependen de la IA |
| Se agota el nivel gratuito de Gemini | Límite de 10 llamadas por usuario por día; cachear consejo diario |
| El proyecto gratuito de Supabase se pausa por inactividad | Entrar al panel al menos una vez por semana; antes de la presentación, abrir la app el día anterior |
| Atraso en un sprint | Deudas (M9) y gráfico de categorías (HU-14) son lo primero que se recorta |
| Alguien no puede trabajar una semana | Sus tarjetas vuelven a `Por hacer` el miércoles y las toma quien tenga menos carga |

---

## 13. Glosario

- **Registro / anotación:** un gasto o ingreso anotado. No mueve dinero.
- **Planificado:** un gasto o ingreso esperado que se repite.
- **Meta:** objetivo de ahorro que la persona controla anotando aportes.
- **Desafío:** reto semanal o mensual con una regla verificable que otorga XP.
- **RLS:** Row Level Security, regla de PostgreSQL que filtra filas por usuario.
- **Edge Function:** pequeña función en la nube de Supabase; la usamos para hablar con la IA.
- **XP / racha:** puntos de experiencia y días seguidos anotando.

---

## 14. Estado actual (24/10/2026)

### Sprint 1 — ✅ cerrado (Andy)
- Repositorio `Andres004/Taller-Cashin`: proyecto Android de un solo módulo (`app`), sin KMP ni iOS.
- Supabase configurado: `001_esquema.sql` y `002_rls.sql` ejecutados, SMTP propio (Gmail con contraseña de aplicación) y plantilla **Reset Password** con `{{ .Token }}`. *Confirm email* desactivado durante el desarrollo.
- Login, crear cuenta, recuperar contraseña con código (la app acepta 6 a 10 dígitos; Supabase envía 8 por defecto) y cerrar sesión.
- `CashinRaiz`: splash → login o app según la sesión. Barra inferior con 5 pestañas.
- Tema, componentes, avatar (4 poses), modelos de todas las tablas y vistas, `Formato`, `Validaciones`, pruebas unitarias.
- Trello creado con backlog completo y el Sprint 2 planificado.

### Sprint 2 — 🔨 en curso (Carlos, entrega mar 27/10 · 17:30)
Guía paso a paso: `docs/sprints/PROMPT_SPRINT2_CARLOS.md`. Rama: `feature/sprint-2-registros`.

| Paso | Historia | Límite |
|---|---|---|
| 1 | Repositorios (`Categoria`, `Registro`, `Perfil`, `Resumen`) | dom 25 · 18:00 |
| 2 | HU-06/07 Anotar gasto o ingreso | dom 25 · 22:00 |
| 3 | HU-09 Historial agrupado por día | lun 26 · 13:00 |
| 4 | HU-08 Editar o eliminar | lun 26 · 20:00 |
| 5 | HU-13 Inicio con totales del mes | mar 27 · 11:00 |
| 6 | HU-05 Perfil con nivel, XP y racha | mar 27 · 14:00 |
| 7 | Prueba final y PR · revisión de Andy 17:00 | mar 27 · 15:30 |

### Pendientes generales
- Proteger `main` en GitHub (exigir PR y 1 aprobación) e invitar a Carlos y Dabner al repo y al tablero.
- Asignar responsables de los sprints 3 a 5 en sus planificaciones.
- iOS queda fuera de alcance; se evaluará al final solo si sobra tiempo y hay una Mac disponible.