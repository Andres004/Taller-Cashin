# CASHIN — Documento Maestro (SDD)

> **Spec-Driven Development.** Este documento es la fuente única de verdad del proyecto.
> Antes de escribir código, cada integrante (y cualquier asistente de IA que usemos) lee la sección que le corresponde.
> Si algo del código contradice este documento, **se corrige el código o se actualiza el documento mediante un pull request**; nunca se deja la contradicción

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
| Versión del documento | 1.3 — Sprints 1–3 cerrados, Sprint 4 en curso (04/11/2026) |

---

## 1. Contexto del producto

Cashin es una **app Android** donde la persona **anota** sus gastos, ingresos, pagos esperados, metas de ahorro y deudas. Un **avatar** (el perrito salchicha de Cashin) la acompaña al estilo Duolingo: da consejos, celebra rachas y propone desafíos.

Lo que diferencia a Cashin es un **asistente de IA predictiva propia** (no generativa, ver §8) que:

1. **Propone desafíos semanales y mensuales** según el historial de la persona (por ejemplo: "Esta semana gasta máximo 150 Bs en comida fuera").
2. **Estima cuánto tiempo falta para una meta** (por ejemplo: "Al ritmo actual juntas los 2.500 Bs del celular en ~5 meses").
3. **Da consejos cortos** a través del avatar.

### 1.1 Principio no negociable

> **Cashin no maneja dinero. Solo guarda anotaciones.**
> No hay cuentas, saldos, transferencias ni conexión con bancos. Una meta o una deuda es solo información para el control personal.

### 1.2 Principios de diseño

- **Registro rápido:** anotar un gasto debe tomar menos de 10 segundos (monto → categoría → guardar).
- **Privacidad por usuario:** cada persona solo ve sus datos. Lo garantiza la base de datos (RLS), no solo la app.
- **El motor calcula, las plantillas explican:** los números (pronósticos, meses restantes, límites de desafíos) salen de modelos estadísticos en Kotlin; el avatar los explica con plantillas de `strings.xml`. No hay IA generativa.
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
| Backend | **Supabase** | Base de datos, login y funciones SQL; no escribimos servidor |
| Cliente Supabase | **supabase-kt** (`auth-kt`, `postgrest-kt`, `functions-kt`) + motor Ktor OkHttp | Hablar con Supabase (`functions-kt` sigue instalado pero no se usa) |
| Serialización | **kotlinx.serialization** | Objetos Kotlin ⇄ JSON |
| Base de datos | **PostgreSQL** (de Supabase) con **RLS** | Datos y privacidad |
| Login con Google (Sprint 3) | **Credential Manager** vía `compose-auth` (repositorio `supabase-kt-plugins`) | Ingreso con Google nativo |
| IA | **Motor de predicción propio** en Kotlin (paquete `prediccion/`), sin APIs externas | Pronósticos, estimación de metas, desafíos, consejos |
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

- **Un solo lenguaje en la app** (Kotlin), incluido el motor de IA; en el servidor solo SQL.
- **Supabase** nos da PostgreSQL real (relacional, el mismo SQL del curso), login con Google y recuperación de contraseña ya resueltos. Eso elimina un backend completo con JWT, BCrypt, sesiones y servidor de correo.
- **RLS** (Row Level Security) hace que la propia base de datos impida ver datos ajenos, aunque la app tuviera un error.
- **IA propia sin APIs externas:** el motor corre en el celular, no tiene costo, no necesita claves secretas y los datos no salen del dispositivo (ver §8).

---

## 5. Arquitectura

```
┌──────────────────────── App Android (Kotlin) ────────────────────────┐
│  Pantalla (Compose) → ViewModel (StateFlow) → Repositorio            │
└───────────────────────────────┬──────────────────────────────────────┘
                                │ HTTPS + token del usuario (automático)
┌───────────────────────────────▼──────────────────────────────────────┐
│                              SUPABASE                                 │
│  Auth (correo, Google)   PostgreSQL + RLS + vistas + funciones SQL    │
└───────────────────────────────────────────────────────────────────────┘
```

La IA no tiene servidor propio ni APIs externas: el motor `prediccion/` corre dentro de la app.

**Flujo de un registro ("gasté 20 Bs en almuerzo con QR"):**
1. La pantalla valida monto > 0 y categoría.
2. El ViewModel llama a `RegistroRepository.crear(...)`.
3. El repositorio hace `insert` en la tabla `registro` con supabase-kt.
4. PostgreSQL verifica RLS, los CHECK y las claves foráneas, y un trigger suma XP y actualiza la racha.
5. La app actualiza el historial y el resumen.

**Flujo de la IA predictiva:**
1. El ViewModel pide al `ResumenRepository` las vistas `v_totales_mes` y `v_gasto_categoria_mes` (y `v_avance_meta` para metas). RLS asegura que solo llegan los datos del usuario.
2. El **motor** (`prediccion/`, Kotlin puro) calcula en el celular: arma la serie mensual, pronostica, proyecta el mes y estima metas o desafíos.
3. El motor devuelve un `MensajeAvatar` (plantilla + ánimo + datos); la UI lo convierte en texto de `strings.xml` y en la pose de Cashi.
4. Si el usuario acepta un desafío, la app lo guarda con la función SQL `aceptar_desafio(...)` (§7.2).

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
│   ├── tests/                           # pruebas de RLS y gamificación en PostgreSQL local
│   └── seed/demo_historial.sql          # datos de demostración (no es migración)
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

Plantilla: `local.properties.example`. Andy comparte los valores por mensaje privado. La clave pública puede ir en la app porque **RLS** protege los datos; la `service_role` nunca se usa en la app.

---

## 7. Base de datos

**Fuente de verdad:** los archivos de `supabase/migrations/`. Este documento solo los resume; si hay diferencia, manda el SQL.

| Archivo | Contenido | Estado |
|---|---|---|
| `001_esquema.sql` | 9 tablas, índices, validaciones, XP/racha automáticos, 4 vistas, categorías del sistema | ✅ probado |
| `002_rls.sql` | Políticas RLS: cada usuario solo ve y cambia lo suyo | ✅ probado |
| `003_gamificacion.sql` | Funciones de desafíos y XP por completar metas (§7.2) | 🔨 Sprint 4 (pruebas en `tests/pruebas_gamificacion.sql`) |

Datos de demostración (no es migración): `supabase/seed/demo_historial.sql` llena 4 meses de historia y una meta para una cuenta existente, para mostrar el motor de predicción. Se ejecuta **una sola vez** por cuenta.

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

La tabla `desafio` es de **solo lectura** para la app (RLS): se crea y se cambia únicamente con estas funciones. La app las llama con `supabase.postgrest.rpc(...)`, usando **exactamente** los nombres de parámetro (`p_…`).

| Función | Devuelve | Qué hace |
|---|---|---|
| `aceptar_desafio(p_titulo, p_descripcion, p_periodo, p_regla, p_categoria_id, p_monto_limite, p_dias_objetivo)` | `desafio` | HU-22. Inserta el desafío para `auth.uid()`. **El servidor** pone las fechas (semana lunes–domingo o mes calendario, hora Bolivia) y el XP (50 semanal, 150 mensual). Rechaza si ya hay un ACTIVO del mismo periodo ("Ya tienes un desafío … activo") o si la categoría no es de gasto del sistema o propia. Guarda solo los campos que aplican a la regla. |
| `descartar_desafio(p_id)` | `boolean` | Pasa a `DESCARTADO` solo si es del usuario y está ACTIVO. |
| `progreso_desafios()` | `{desafio_id, gastado, dias_con_registro}` | Medición de cada desafío ACTIVO (para las barras de progreso). |
| `evaluar_desafios()` | `setof desafio` | HU-23. Para cada ACTIVO: **LIMITE_CATEGORIA / LIMITE_TOTAL** → si el gasto del periodo supera `monto_limite`, FALLIDO; si `fecha_fin` pasó sin superarlo, CUMPLIDO. **DIAS_REGISTRO** → si llegó a `dias_objetivo` días distintos con registro, CUMPLIDO; si `fecha_fin` pasó sin lograrlo, FALLIDO. Al pasar a CUMPLIDO suma `xp_recompensa` (una sola vez). Devuelve **solo los que cambiaron**. La app la llama al abrir Inicio y Desafíos. |
| `completar_meta(p_meta_id)` | `boolean` | Si la meta está ACTIVA y lo ahorrado (ahorros − retiros) ≥ objetivo: la marca COMPLETADA y suma **+100 XP**. Si no, `false` (no da XP dos veces). |

Funciones internas (no expuestas a la app): `fn_rango_periodo(periodo)` (fechas del periodo actual) y `fn_medir_desafio(desafio)` (gasto y días con registro del periodo).

Todas son `security definer set search_path = public`, filtran siempre por `auth.uid()` y solo las puede ejecutar el rol `authenticated` (no `anon`).

### 7.3 Reglas de XP y nivel

| Acción | XP |
|---|---|
| Anotar un registro | +10 (máx. 5 por día) |
| Cumplir desafío semanal | +50 |
| Cumplir desafío mensual | +150 |
| Completar una meta | +100 |

**Nivel** = `xp / 200 + 1` (división entera), calculado en la app. **Racha** = días consecutivos con al menos un registro (hora Bolivia). Si hoy aún no anotó, la racha se muestra "en riesgo".

---

## 8. IA predictiva

Cashin usa un **motor de predicción propio**, escrito en Kotlin puro en el paquete `prediccion/` (sin `android.*`, `androidx.*` ni Supabase). Recibe los datos de las vistas, calcula en el celular y devuelve números y mensajes de plantilla. **No es IA generativa:** no se llama a Gemini, OpenAI ni ninguna API externa.

### 8.1 Modelos

| Pieza | Archivo | Modelo |
|---|---|---|
| Serie mensual | `SerieMensual.kt` (`construirSerie`) | Convierte `v_totales_mes` en una serie de meses **completos** (sin el actual), rellenando con 0 los meses sin registros. |
| Pronóstico del siguiente mes | `Pronostico.kt` (`pronosticarSiguienteMes`) | 1 mes → ese valor (margen 25 %). 2 meses → promedio. 3 o más → **regresión lineal por mínimos cuadrados** (últimos hasta 6 meses) combinada **50/50 con un promedio móvil ponderado** (el mes reciente pesa más). Margen = **desviación estándar de los residuos** (mínimo 10 %). Confianza: BAJA (1–2 meses), MEDIA (3–5), ALTA (6+). |
| Proyección del mes actual | `Pronostico.kt` (`proyectarMesActual`, `pronosticarProximoMes`) | Ritmo = gastado ÷ día del mes × días del mes, **mezclado** con el pronóstico según el avance del mes (al inicio manda la historia, al final manda el ritmo real). |
| Estimación de metas (HU-20) | `EstimadorMeta.kt` (`estimarMeta`) | Ritmo de ahorro = promedio de **aportes netos** de los últimos 3 meses con aportes (si hay 2+ meses); si no, **capacidad de ahorro** = ingresos esperados − gastos esperados. Meses = ⌈faltante ÷ ritmo⌉; se compara con la fecha límite. |
| Mensajes del avatar | `MensajesAvatar.kt` | Decide la **plantilla** (`TipoMensaje`), el ánimo y los datos; la UI la traduce con `MensajePrediccion.kt` a `strings.xml` y a la pose de Cashi. |
| Desafíos y consejo del día (Sprint 4) | `GeneradorDesafios.kt`, `Consejos.kt` | Propuestas de desafío por reglas (límite por categoría = 85 % del promedio, límite total = 90 % del pronóstico, días de registro) y consejo del día por prioridad (racha en riesgo, desafío en riesgo, categoría que sube, gasto más rápido, meta cerca, racha buena). |

### 8.2 Por qué predictiva y no generativa

- **Números exactos y explicables:** cada cifra sale de una fórmula que se puede mostrar y defender; un modelo generativo puede inventar cifras.
- **Sin costo ni límites:** no hay cuotas de API, claves secretas ni dependencia de un servicio externo.
- **Privacidad:** los datos no salen del celular más allá de Supabase (protegido con RLS).
- **Funciona siempre:** si no hay datos, el motor responde un mensaje de "sin datos" en vez de fallar.

### 8.3 Evaluación

- **Error medio absoluto walk-forward** (`errorMedioAbsoluto`): para cada mes desde el 3.º se pronostica solo con los meses anteriores y se compara con lo real (necesita 4+ meses).
- **Pruebas unitarias** en `app/src/test/java/org/ucb/cashin/prediccion/` (serie, pronóstico, estimación de metas, mensajes; y en el Sprint 4, generador de desafíos y consejos).
- Para la demo: `supabase/seed/demo_historial.sql` carga 4 meses con tendencia creciente.

### 8.4 Límites

- Necesita **1 a 3 meses de historia** para pronósticos útiles; con menos, la confianza es BAJA o SIN_DATOS y el avatar pide anotar más.
- Supone que el comportamiento reciente se mantiene: no anticipa gastos extraordinarios (un viaje, una emergencia).
- Solo ve lo que el usuario anota: si no anota, el modelo no lo sabe.

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
| S2 | **mar 27/10 · 17:30** | **Carlos** (revisa Andy) | Anotar (HU-06/07), historial (HU-09), editar/eliminar (HU-08), Inicio con totales (HU-13), perfil (HU-05) | ✅ Hecho |
| S3 | mar 03/11 · 17:30 | por definir | Filtros (HU-10), categorías (HU-11/12), planificados (HU-15/16), metas (HU-17–19), motor de predicción y estimación de metas (HU-20) | ✅ Hecho (Google HU-02 pasa a S4 opcional / S5) |
| S4 | mar 10/11 · 17:30 | por definir | Consejo del día (HU-21); desafíos (HU-22/23); XP, racha y nivel (HU-24); `003_gamificacion.sql`; Google (HU-02) si sobra tiempo | 🔨 En curso |
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
| El motor de predicción tiene poca historia y pronostica mal | Confianza BAJA / SIN_DATOS con mensajes que piden anotar más; para la demo, `seed/demo_historial.sql` |
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
- **Función SQL (rpc):** función de PostgreSQL que la app llama por nombre; la usamos para desafíos y XP, que la app no puede escribir directo.
- **IA predictiva:** modelos estadísticos que pronostican a partir del historial (no redactan texto como la IA generativa).
- **XP / racha:** puntos de experiencia y días seguidos anotando.

---

## 14. Estado actual (04/11/2026)

### Sprint 1 — ✅ cerrado (Andy)
- Repositorio `Andres004/Taller-Cashin`: proyecto Android de un solo módulo (`app`), sin KMP ni iOS.
- Supabase configurado: `001_esquema.sql` y `002_rls.sql` ejecutados, SMTP propio (Gmail con contraseña de aplicación) y plantilla **Reset Password** con `{{ .Token }}`. *Confirm email* desactivado durante el desarrollo.
- Login, crear cuenta, recuperar contraseña con código (la app acepta 6 a 10 dígitos; Supabase envía 8 por defecto) y cerrar sesión.
- `CashinRaiz`: splash → login o app según la sesión. Barra inferior con 5 pestañas.
- Tema, componentes, avatar (4 poses), modelos de todas las tablas y vistas, `Formato`, `Validaciones`, pruebas unitarias.
- Trello creado con backlog completo y el Sprint 2 planificado.

### Sprint 2 — ✅ cerrado (Carlos)
- Repositorios `Categoria`, `Registro`, `Perfil` y `Resumen`.
- Anotar gasto o ingreso (HU-06/07), historial agrupado por día (HU-09), editar y eliminar (HU-08).
- Inicio con totales del mes (HU-13) y perfil con nombre, moneda, nivel, XP y racha (HU-05).

### Sprint 3 — ✅ cerrado
- Filtros del historial por tipo, categoría y fechas (HU-10).
- Mis categorías: crear y archivar (HU-11/12).
- Pagos planificados: crear y marcar como pagado, una vez por fecha (HU-15/16).
- Metas: crear, editar, ahorros y retiros, detalle con movimientos, completadas en su propia sección (HU-17–19).
- **Motor de predicción** `prediccion/` con pruebas: serie mensual, pronóstico (regresión + promedio ponderado), proyección del mes, estimación de metas (HU-20) y mensajes del avatar. Inicio muestra la proyección del mes y el pronóstico del próximo.
- Se descartó la IA generativa (Gemini + Edge Function `ia-asistente`); ver §8.
- Marcadores "próximamente" para Desafíos (S4) y Deudas (S5), abiertos desde el Perfil.
- Datos de demostración: `supabase/seed/demo_historial.sql`.
- Pendiente que pasa al S4 (opcional) / S5: ingreso con Google (HU-02).

### Sprint 4 — 🔨 en curso (entrega mar 10/11 · 17:30)
| Fase | Tarea | Límite |
|---|---|---|
| 1 | Cierre del Sprint 3: Maestro, datos de demo | jue 05/11 · 22:00 |
| 2 | `003_gamificacion.sql` + pruebas (§7.2) | vie 06/11 · 13:00 |
| 3 | Capa de datos: `DesafioRepository`, `completar_meta`, gasto por categoría | vie 06/11 · 22:00 |
| 4 | Motor: generador de desafíos y consejo del día, con pruebas | sáb 07/11 · 22:00 |
| 5 | Pantalla de Desafíos (HU-22/23) | dom 08/11 · 22:00 |
| 6 | Inicio: consejo del día, desafío activo, racha en riesgo, subida de nivel (HU-21/24) | lun 09/11 · 13:00 |
| 7 | Metas: +100 XP al completar | lun 09/11 · 20:00 |
| 8 | Verificación final y PR | mar 10/11 · 15:00 |

### Pendientes generales
- Proteger `main` en GitHub (exigir PR y 1 aprobación) e invitar a Carlos y Dabner al repo y al tablero.
- Asignar responsables de los sprints 4 y 5 en sus planificaciones.
- Quitar la dependencia `functions-kt` y `install(Functions)` cuando se confirme que no se usará (ya no hay Edge Functions).
- iOS queda fuera de alcance; se evaluará al final solo si sobra tiempo y hay una Mac disponible.
