package org.ucb.cashin.data

import kotlinx.coroutines.CancellationException
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.ErrorAppException
import java.io.IOException

/**
 * Ejecuta una llamada a Supabase y devuelve Result<T>, traduciendo los errores a [ErrorApp].
 * TODOS los métodos de los repositorios deben pasar por aquí.
 *
 * Uso:  suspend fun listar() = llamada { supabase.from("registro").select().decodeList<Registro>() }
 */
suspend fun <T> llamada(bloque: suspend () -> T): Result<T> =
    try {
        Result.success(bloque())
    } catch (e: CancellationException) {
        throw e // nunca tragarse la cancelación de corrutinas
    } catch (e: Throwable) {
        Result.failure(ErrorAppException(traducirError(e), e))
    }

/** Convierte un error de Supabase/red en un [ErrorApp] según su mensaje y su causa. */
internal fun traducirError(e: Throwable): ErrorApp {
    if (e is ErrorAppException) return e.error
    if (generateSequence(e) { it.cause }.any { it is IOException }) return ErrorApp.SIN_CONEXION

    val texto = generateSequence(e) { it.cause }
        .mapNotNull { it.message }
        .joinToString(" ")
        .lowercase()

    return when {
        "invalid login credentials" in texto || "invalid_credentials" in texto -> ErrorApp.CREDENCIALES_INVALIDAS
        "already registered" in texto || "user_already_exists" in texto ||
            "already exists" in texto && "user" in texto -> ErrorApp.CORREO_YA_REGISTRADO
        "email not confirmed" in texto || "email_not_confirmed" in texto -> ErrorApp.CORREO_NO_CONFIRMADO
        "weak" in texto && "password" in texto -> ErrorApp.CONTRASENA_DEBIL
        "expired" in texto || "invalid" in texto && ("otp" in texto || "token" in texto) -> ErrorApp.CODIGO_INVALIDO
        "rate limit" in texto || "too many" in texto || "429" in texto -> ErrorApp.DEMASIADOS_INTENTOS
        "row-level security" in texto || "permission denied" in texto || "42501" in texto -> ErrorApp.SIN_PERMISO
        "violates" in texto || "supera" in texto || "check constraint" in texto -> ErrorApp.DATO_INVALIDO
        "unable to resolve host" in texto || "timeout" in texto || "failed to connect" in texto -> ErrorApp.SIN_CONEXION
        else -> ErrorApp.DESCONOCIDO
    }
}
