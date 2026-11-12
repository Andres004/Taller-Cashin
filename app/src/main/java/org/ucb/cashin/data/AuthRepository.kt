package org.ucb.cashin.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.composeAuth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.ucb.cashin.di.AppContainer

/** Estado de la sesión, simplificado para la UI. */
enum class EstadoSesion { CARGANDO, CON_SESION, SIN_SESION }

/** Resultado de crear una cuenta. */
enum class ResultadoRegistro {
    /** La cuenta quedó activa y con sesión iniciada (confirmación de correo desactivada). */
    LISTO,
    /** Supabase envió un correo de confirmación; el usuario debe confirmarlo y luego ingresar. */
    CONFIRMAR_CORREO,
}

/**
 * Módulo M1 · Cuenta (HU-01, HU-03, HU-04). Dueño: Sprint 1.
 * El login con Google (HU-02) se agrega aquí en el Sprint 2.
 */
class AuthRepository(private val supabase: SupabaseClient) {

    /** Emite el estado de sesión. La navegación raíz (CashinRaiz) reacciona a este Flow. */
    val estadoSesion: Flow<EstadoSesion> = supabase.auth.sessionStatus
        .map { estado ->
            when (estado) {
                is SessionStatus.Authenticated -> EstadoSesion.CON_SESION
                is SessionStatus.Initializing -> EstadoSesion.CARGANDO
                else -> EstadoSesion.SIN_SESION // NotAuthenticated o RefreshFailure
            }
        }
        .distinctUntilChanged()

    /** Id del usuario actual (para columnas usuario_id que no tienen DEFAULT, como en categoria). */
    fun usuarioIdActual(): String? = supabase.auth.currentUserOrNull()?.id

    fun correoActual(): String? = supabase.auth.currentUserOrNull()?.email

    /** HU-01. El nombre viaja como metadato y el trigger fn_crear_perfil lo copia a `perfil`. */
    suspend fun registrar(nombre: String, correo: String, contrasena: String): Result<ResultadoRegistro> =
        llamada {
            supabase.auth.signUpWith(Email) {
                email = correo.trim()
                password = contrasena
                data = buildJsonObject { put("nombre", nombre.trim()) }
            }
            if (supabase.auth.currentSessionOrNull() != null) ResultadoRegistro.LISTO
            else ResultadoRegistro.CONFIRMAR_CORREO
        }

    suspend fun ingresar(correo: String, contrasena: String): Result<Unit> = llamada {
        supabase.auth.signInWith(Email) {
            email = correo.trim()
            password = contrasena
        }
        Unit
    }

    /**
     * HU-03 paso 1: envía al correo un código de 6 dígitos.
     * Requiere que la plantilla "Reset Password" de Supabase muestre {{ .Token }} (ver README).
     */
    suspend fun enviarCodigoRecuperacion(correo: String): Result<Unit> = llamada {
        supabase.auth.resetPasswordForEmail(correo.trim())
        Unit
    }

    /**
     * HU-03 paso 2: valida el código y guarda la nueva contraseña.
     * Al validar el código Supabase inicia sesión; por eso ambos pasos corren en
     * NonCancellable: la pantalla cambia a Inicio, pero la contraseña igual se guarda.
     */
    suspend fun cambiarContrasenaConCodigo(correo: String, codigo: String, nueva: String): Result<Unit> =
        llamada {
            withContext(NonCancellable) {
                supabase.auth.verifyEmailOtp(type = OtpType.Email.RECOVERY, email = correo.trim(), token = codigo.trim())
                supabase.auth.updateUser { password = nueva }
            }
            Unit
        }

    /**
     * Si la cuenta entró con correo y contraseña (true) o con Google (false). Solo las de correo
     * pueden cambiar su contraseña desde la app. Si no se sabe, se asume correo.
     */
    fun esCuentaConCorreo(): Boolean {
        val proveedor = supabase.auth.currentUserOrNull()?.appMetadata?.get("provider")?.jsonPrimitive?.content
        return proveedor == null || proveedor == "email"
    }

    /** HU-02: la cuenta entró con Google (para la bienvenida de la primera vez). */
    fun esCuentaDeGoogle(): Boolean =
        supabase.auth.currentUserOrNull()?.appMetadata?.get("provider")?.jsonPrimitive?.content == "google"

    /**
     * HU-02: el plugin que abre la hoja de cuentas de Google. Null si falta GOOGLE_WEB_CLIENT_ID.
     * La pantalla lo usa con rememberSignInWithGoogle (la API de compose-auth es un Composable);
     * al volver, Supabase guarda la sesión y CashinRaiz cambia sola a la app.
     */
    fun googleNativo(): ComposeAuth? =
        if (AppContainer.googleConfigurado) supabase.composeAuth else null

    /** Perfil → Cambiar contraseña: con la sesión iniciada no hace falta la contraseña anterior. */
    suspend fun cambiarContrasena(nueva: String): Result<Unit> = llamada {
        supabase.auth.updateUser { password = nueva }
        Unit
    }

    /** HU-04. Cierra la sesión y borra el token guardado en el celular. */
    suspend fun cerrarSesion(): Result<Unit> = llamada {
        supabase.auth.signOut()
        Unit
    }
}
