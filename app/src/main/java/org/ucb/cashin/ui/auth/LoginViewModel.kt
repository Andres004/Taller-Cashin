package org.ucb.cashin.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.compose.auth.ComposeAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ucb.cashin.R
import org.ucb.cashin.data.AuthRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.util.Validaciones

/** Cómo terminó la hoja de cuentas de Google (la pantalla traduce el resultado de compose-auth). */
enum class ResultadoGoogle {
    /** Entró: Supabase guardó la sesión y CashinRaiz cambia sola a la app. */
    LISTO,
    /** Cerró la hoja sin elegir: no se muestra nada. */
    CANCELADO,
    SIN_CONEXION,
    /** El correo ya tiene una cuenta con contraseña (y no se pudo unir). */
    CUENTA_CON_CORREO,
    /** El teléfono no tiene una cuenta de Google para elegir. */
    SIN_CUENTA_GOOGLE,
    ERROR,
}

data class LoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    @StringRes val errorCorreo: Int? = null,
    @StringRes val errorContrasena: Int? = null,
    val error: ErrorApp? = null,
    val cargando: Boolean = false,
    /** La hoja de Google está abierta o se está guardando la sesión. */
    val cargandoGoogle: Boolean = false,
    /** Aviso corto abajo (sin internet, sin configurar...). La pantalla lo muestra y lo limpia. */
    @StringRes val avisoGoogle: Int? = null,
    /** Diálogo "Ese correo ya tiene cuenta". */
    val correoConCuenta: Boolean = false,
)

/**
 * HU-01 (ingresar con correo) y HU-02 (ingresar con Google).
 * Al ingresar de cualquiera de las dos formas, CashinRaiz cambia sola a la app principal.
 */
class LoginViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** Null si falta GOOGLE_WEB_CLIENT_ID en local.properties. */
    val googleNativo: ComposeAuth? = auth.googleNativo()

    fun onCorreoCambio(valor: String) =
        _uiState.update { it.copy(correo = valor, errorCorreo = null, error = null) }

    fun onContrasenaCambio(valor: String) =
        _uiState.update { it.copy(contrasena = valor, errorContrasena = null, error = null) }

    fun ingresar() {
        val estado = _uiState.value
        val errorCorreo = if (Validaciones.correoValido(estado.correo)) null else R.string.validacion_correo
        val errorContrasena = if (estado.contrasena.isNotEmpty()) null else R.string.validacion_contrasena_vacia
        if (errorCorreo != null || errorContrasena != null) {
            _uiState.update { it.copy(errorCorreo = errorCorreo, errorContrasena = errorContrasena) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            auth.ingresar(estado.correo, estado.contrasena)
                .onSuccess { _uiState.update { it.copy(cargando = false) } }
                .onFailure { e -> _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) } }
        }
    }

    /** Se tocó "Continuar con Google" y se abrió la hoja de cuentas. */
    fun onGoogleInicio() = _uiState.update { it.copy(cargandoGoogle = true, error = null, avisoGoogle = null) }

    /** Falta GOOGLE_WEB_CLIENT_ID: se avisa en vez de cerrar la app. */
    fun onGoogleSinConfigurar() = _uiState.update { it.copy(avisoGoogle = R.string.login_google_sin_configurar) }

    fun onGoogleResultado(resultado: ResultadoGoogle) = _uiState.update {
        when (resultado) {
            ResultadoGoogle.LISTO, ResultadoGoogle.CANCELADO -> it.copy(cargandoGoogle = false)
            ResultadoGoogle.SIN_CONEXION -> it.copy(cargandoGoogle = false, avisoGoogle = R.string.login_google_sin_conexion)
            ResultadoGoogle.SIN_CUENTA_GOOGLE -> it.copy(cargandoGoogle = false, avisoGoogle = R.string.login_google_sin_cuenta)
            ResultadoGoogle.CUENTA_CON_CORREO -> it.copy(cargandoGoogle = false, correoConCuenta = true)
            ResultadoGoogle.ERROR -> it.copy(cargandoGoogle = false, avisoGoogle = R.string.login_google_error)
        }
    }

    fun onAvisoVisto() = _uiState.update { it.copy(avisoGoogle = null) }

    fun onCerrarCorreoConCuenta() = _uiState.update { it.copy(correoConCuenta = false) }

    companion object {
        val Factory = fabricaDe { LoginViewModel(it.authRepository) }

        /** Supabase responde así cuando el correo ya existe con otra forma de entrar. */
        fun esCorreoConCuenta(mensaje: String?): Boolean {
            val texto = mensaje.orEmpty().lowercase()
            return "already" in texto || "email_exists" in texto || "identity_already_exists" in texto ||
                "user already registered" in texto
        }
    }
}
