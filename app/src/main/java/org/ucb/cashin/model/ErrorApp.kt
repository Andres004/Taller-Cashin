package org.ucb.cashin.model

/**
 * Errores que la app sabe explicar al usuario. Los repositorios convierten cualquier
 * excepción en un [ErrorAppException]; la UI muestra el texto con `ErrorApp.mensaje()`
 * (ver ui/components/Errores.kt).
 */
enum class ErrorApp {
    SIN_CONEXION,
    CREDENCIALES_INVALIDAS,
    CORREO_YA_REGISTRADO,
    CORREO_NO_CONFIRMADO,
    CONTRASENA_DEBIL,
    CODIGO_INVALIDO,
    DEMASIADOS_INTENTOS,
    SIN_PERMISO,
    DATO_INVALIDO,
    DESCONOCIDO,
}

class ErrorAppException(val error: ErrorApp, causa: Throwable? = null) :
    Exception(error.name, causa)

/** Obtiene el [ErrorApp] de cualquier Throwable (DESCONOCIDO si no se reconoce). */
fun Throwable.comoErrorApp(): ErrorApp = (this as? ErrorAppException)?.error ?: ErrorApp.DESCONOCIDO
