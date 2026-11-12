package org.ucb.cashin.navigation

import kotlinx.serialization.Serializable

/**
 * Rutas con tipos (Navigation Compose). Para una pantalla nueva:
 *  1. Agregarla aquí:  @Serializable data object MisCategorias
 *     (con argumentos:  @Serializable data class DetalleMeta(val metaId: String))
 *  2. Registrarla en el NavHost que corresponda:  composable<Rutas.MisCategorias> { ... }
 *  3. Navegar con:  navController.navigate(Rutas.MisCategorias)
 */
object Rutas {
    // ---- Sin sesión (AuthNavHost) ----
    /** Primera pantalla sin sesión: logo, mensaje y los botones "Crear cuenta" e "Iniciar sesión". */
    @Serializable data object Bienvenida
    @Serializable data object Login
    @Serializable data object CrearCuenta
    @Serializable data object RecuperarContrasena

    // ---- Con sesión: pestañas de la barra inferior (PrincipalNavHost) ----
    @Serializable data object Inicio
    @Serializable data object Historial
    @Serializable data object Anotar
    @Serializable data object Metas
    @Serializable data object Perfil

    // ---- Con sesión: pantallas internas ----
    /** Formulario de Anotar con una anotación ya cargada para corregirla o borrarla. */
    @Serializable data class EditarRegistro(val registroId: String)

    // Se abren desde la pestaña Metas.
    /** HU-17 · Sprint 3. */
    @Serializable data object NuevaMeta
    /** HU-18, HU-19 · Sprint 3. */
    @Serializable data class DetalleMeta(val metaId: String)
    /** Formulario de Nueva meta con la meta ya cargada para corregirla. */
    @Serializable data class EditarMeta(val metaId: String)

    // Se abren desde el menú del Perfil.
    /** HU-11, HU-12 · Sprint 3. */
    @Serializable data object MisCategorias
    /** HU-15, HU-16 · Sprint 3. */
    @Serializable data object Planificados
    /** HU-15: formulario de un pago o ingreso esperado (se abre desde Planificados). */
    @Serializable data object NuevoPlanificado
    /** HU-22, HU-23 · Sprint 4. */
    @Serializable data object Desafios
    /** HU-25, HU-26 · Sprint 5. */
    @Serializable data object Deudas
    /**
     * Detalle de una deuda con su línea de tiempo. [prestarMas] y [monto] llegan desde
     * "Sumar a la de Amy" en el formulario: abre la hoja "Prestar más" con ese monto.
     */
    @Serializable data class DetalleDeuda(val deudaId: String, val prestarMas: Boolean = false, val monto: String? = null)
    /** Nueva deuda ([deudaId] null, del [tipo] de la pestaña) o editar una existente. */
    @Serializable data class FormularioDeuda(val deudaId: String? = null, val tipo: String = "DEBO")
    /** HU-05 · Sprint 5: avatar, nombre, moneda y día de cobro (desde el lápiz del Perfil). */
    @Serializable data object EditarPerfil
    /** Cambiar la contraseña (solo cuentas con correo; se abre desde Editar perfil). */
    @Serializable data object CambiarContrasena
    /** Recordatorio diario (Sprint 5; se abre desde el menú del Perfil). */
    @Serializable data object Recordatorio
    /** HU-14: Historial abierto directo en la pestaña Reportes (desde el menú del Perfil). */
    @Serializable data object Reportes
    /** HU-24: la racha con su semana (se abre desde Inicio y Perfil). */
    @Serializable data object Racha
}