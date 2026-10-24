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

data class LoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    @StringRes val errorCorreo: Int? = null,
    @StringRes val errorContrasena: Int? = null,
    val error: ErrorApp? = null,
    val cargando: Boolean = false,
)

/** HU-01 (ingresar con correo). Al ingresar, CashinRaiz cambia sola a la app principal. */
class LoginViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

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

    companion object {
        val Factory = fabricaDe { LoginViewModel(it.authRepository) }
    }
}
