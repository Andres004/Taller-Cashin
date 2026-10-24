package org.ucb.cashin.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

enum class PasoRecuperar { CORREO, CODIGO }

data class RecuperarUiState(
    val paso: PasoRecuperar = PasoRecuperar.CORREO,
    val correo: String = "",
    val codigo: String = "",
    val nueva: String = "",
    val confirmar: String = "",
    @StringRes val errorCorreo: Int? = null,
    @StringRes val errorCodigo: Int? = null,
    @StringRes val errorNueva: Int? = null,
    @StringRes val errorConfirmar: Int? = null,
    val error: ErrorApp? = null,
    val cargando: Boolean = false,
    /** Mensaje de un solo uso (Snackbar). La pantalla llama a [RecuperarViewModel.mensajeMostrado]. */
    @StringRes val mensaje: Int? = null,
)

/**
 * HU-03 en dos pasos y sin enlaces: (1) se envía un código al correo, (2) se escribe el
 * código y la nueva contraseña. Al terminar, Supabase deja la sesión iniciada y la app pasa a Inicio.
 */
class RecuperarViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RecuperarUiState())
    val uiState: StateFlow<RecuperarUiState> = _uiState.asStateFlow()

    fun onCorreoCambio(v: String) = _uiState.update { it.copy(correo = v, errorCorreo = null, error = null) }
    fun onCodigoCambio(v: String) = _uiState.update { it.copy(codigo = v.filter(Char::isDigit).take(Validaciones.LARGO_CODIGO), errorCodigo = null, error = null) }
    fun onNuevaCambio(v: String) = _uiState.update { it.copy(nueva = v, errorNueva = null, error = null) }
    fun onConfirmarCambio(v: String) = _uiState.update { it.copy(confirmar = v, errorConfirmar = null, error = null) }
    fun mensajeMostrado() = _uiState.update { it.copy(mensaje = null) }
    fun volverAlCorreo() = _uiState.update { it.copy(paso = PasoRecuperar.CORREO, error = null) }

    fun enviarCodigo() {
        val s = _uiState.value
        if (!Validaciones.correoValido(s.correo)) {
            _uiState.update { it.copy(errorCorreo = R.string.validacion_correo) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            auth.enviarCodigoRecuperacion(s.correo)
                .onSuccess {
                    _uiState.update {
                        it.copy(cargando = false, paso = PasoRecuperar.CODIGO, mensaje = R.string.recuperar_codigo_enviado)
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) } }
        }
    }

    fun cambiarContrasena() {
        val s = _uiState.value
        val conErrores = s.copy(
            errorCodigo = if (Validaciones.codigoValido(s.codigo)) null else R.string.validacion_codigo,
            errorNueva = if (Validaciones.contrasenaValida(s.nueva)) null else R.string.validacion_contrasena,
            errorConfirmar = if (s.confirmar == s.nueva) null else R.string.validacion_no_coinciden,
        )
        if (conErrores.errorCodigo != null || conErrores.errorNueva != null || conErrores.errorConfirmar != null) {
            _uiState.value = conErrores
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            auth.cambiarContrasenaConCodigo(s.correo, s.codigo, s.nueva)
                .onSuccess { _uiState.update { it.copy(cargando = false) } }
                .onFailure { e -> _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) } }
        }
    }

    companion object {
        val Factory = fabricaDe { RecuperarViewModel(it.authRepository) }
    }
}
