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
}