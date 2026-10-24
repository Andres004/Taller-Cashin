package org.ucb.cashin.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ucb.cashin.data.AuthRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.comoErrorApp

data class PerfilUiState(
    val correo: String = "",
    val cerrandoSesion: Boolean = false,
    val error: ErrorApp? = null,
)

/**
 * Sprint 1: solo correo y cerrar sesión (HU-04).
 * Sprint 2 (HU-05): agregar PerfilRepository para leer nombre, nivel, XP y racha
 * y editar nombre/moneda con PerfilEditable.
 */
class PerfilViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PerfilUiState(correo = auth.correoActual().orEmpty()))
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    /** Al cerrar sesión, CashinRaiz vuelve sola a la pantalla de Login. */
    fun cerrarSesion() {
        viewModelScope.launch {
            _uiState.update { it.copy(cerrandoSesion = true, error = null) }
            auth.cerrarSesion()
                .onFailure { e -> _uiState.update { it.copy(cerrandoSesion = false, error = e.comoErrorApp()) } }
        }
    }

    companion object {
        val Factory = fabricaDe { PerfilViewModel(it.authRepository) }
    }
}
