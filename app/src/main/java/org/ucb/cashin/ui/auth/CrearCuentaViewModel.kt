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
import org.ucb.cashin.data.ResultadoRegistro
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.util.Validaciones

data class CrearCuentaUiState(
    val nombre: String = "",
    val correo: String = "",
    val contrasena: String = "",
    val confirmar: String = "",
    @StringRes val errorNombre: Int? = null,
    @StringRes val errorCorreo: Int? = null,
    @StringRes val errorContrasena: Int? = null,
    @StringRes val errorConfirmar: Int? = null,
    val error: ErrorApp? = null,
    val cargando: Boolean = false,
    /** Distinto de null cuando Supabase envió el correo de confirmación a esa dirección. */
    val correoConfirmacion: String? = null,
)

/** HU-01: crear cuenta con nombre, correo y contraseña (el perfil se crea con un trigger). */
class CrearCuentaViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CrearCuentaUiState())
    val uiState: StateFlow<CrearCuentaUiState> = _uiState.asStateFlow()

    fun onNombreCambio(v: String) = _uiState.update { it.copy(nombre = v, errorNombre = null, error = null) }
    fun onCorreoCambio(v: String) = _uiState.update { it.copy(correo = v, errorCorreo = null, error = null) }
    fun onContrasenaCambio(v: String) = _uiState.update { it.copy(contrasena = v, errorContrasena = null, error = null) }
    fun onConfirmarCambio(v: String) = _uiState.update { it.copy(confirmar = v, errorConfirmar = null, error = null) }

    fun crearCuenta() {
        val s = _uiState.value
        val errores = s.copy(
            errorNombre = if (Validaciones.nombreValido(s.nombre)) null else R.string.validacion_nombre,
            errorCorreo = if (Validaciones.correoValido(s.correo)) null else R.string.validacion_correo,
            errorContrasena = if (Validaciones.contrasenaValida(s.contrasena)) null else R.string.validacion_contrasena,
            errorConfirmar = if (s.confirmar == s.contrasena) null else R.string.validacion_no_coinciden,
        )
        if (listOf(errores.errorNombre, errores.errorCorreo, errores.errorContrasena, errores.errorConfirmar).any { it != null }) {
            _uiState.value = errores
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            auth.registrar(s.nombre, s.correo, s.contrasena)
                .onSuccess { resultado ->
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            correoConfirmacion = if (resultado == ResultadoRegistro.CONFIRMAR_CORREO) s.correo.trim() else null,
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) } }
        }
    }

    companion object {
        val Factory = fabricaDe { CrearCuentaViewModel(it.authRepository) }
    }
}
