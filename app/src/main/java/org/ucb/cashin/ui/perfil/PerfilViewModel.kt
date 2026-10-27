package org.ucb.cashin.ui.perfil

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
import org.ucb.cashin.data.PerfilRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.Perfil
import org.ucb.cashin.model.PerfilEditable
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.util.Formato
import org.ucb.cashin.util.Validaciones

/** Todo lo que la pantalla Perfil necesita para dibujarse. */
data class PerfilUiState(
    val correo: String = "",
    val perfil: Perfil? = null,
    val racha: Int = 0,
    val cargando: Boolean = true,
    val errorCarga: ErrorApp? = null,
    val editandoNombre: Boolean = false,
    val nombreEditado: String = "",
    @StringRes val errorNombre: Int? = null,
    val guardandoNombre: Boolean = false,
    val errorGuardar: ErrorApp? = null,
    val cerrandoSesion: Boolean = false,
    val error: ErrorApp? = null,
)

/**
 * HU-05: ver el progreso (nivel, XP y racha) y cambiar el nombre.
 * HU-04: cerrar sesión.
 */
class PerfilViewModel(
    private val auth: AuthRepository,
    private val perfilRepository: PerfilRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerfilUiState(correo = auth.correoActual().orEmpty()))
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    /** Trae el perfil. Se llama cada vez que se abre la pestaña, así el XP y la racha están al día. */
    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = it.perfil == null, errorCarga = null) }
            perfilRepository.obtener()
                .onSuccess { perfil -> mostrarPerfil(perfil) }
                .onFailure { e -> _uiState.update { it.copy(cargando = false, errorCarga = e.comoErrorApp()) } }
        }
    }

    /** Abre el diálogo con el nombre actual ya escrito. */
    fun onEditarNombre() {
        val perfil = _uiState.value.perfil ?: return
        _uiState.update {
            it.copy(editandoNombre = true, nombreEditado = perfil.nombre, errorNombre = null, errorGuardar = null)
        }
    }

    /** El nombre no puede pasar de 80 caracteres, igual que en la base de datos. */
    fun onNombreCambio(valor: String) =
        _uiState.update {
            it.copy(nombreEditado = valor.take(Validaciones.MAX_NOMBRE), errorNombre = null, errorGuardar = null)
        }

    fun onCancelarEdicion() =
        _uiState.update { it.copy(editandoNombre = false) }

    /** Revisa el nombre y, si está bien, lo guarda sin tocar la moneda ni el avatar. */
    fun guardarNombre() {
        val estado = _uiState.value
        val perfil = estado.perfil ?: return
        if (estado.guardandoNombre) return

        if (!Validaciones.nombreValido(estado.nombreEditado)) {
            _uiState.update { it.copy(errorNombre = R.string.validacion_nombre) }
            return
        }

        val cambios = PerfilEditable(
            nombre = estado.nombreEditado.trim(),
            moneda = perfil.moneda,
            avatar = perfil.avatar,
        )

        viewModelScope.launch {
            _uiState.update { it.copy(guardandoNombre = true, errorGuardar = null) }
            perfilRepository.actualizar(perfil.id, cambios)
                .onSuccess { actualizado ->
                    mostrarPerfil(actualizado)
                    _uiState.update { it.copy(guardandoNombre = false, editandoNombre = false) }
                }
                .onFailure { e -> _uiState.update { it.copy(guardandoNombre = false, errorGuardar = e.comoErrorApp()) } }
        }
    }

    /** Al cerrar sesión, CashinRaiz vuelve sola a la pantalla de Login. */
    fun cerrarSesion() {
        viewModelScope.launch {
            _uiState.update { it.copy(cerrandoSesion = true, error = null) }
            auth.cerrarSesion()
                .onFailure { e -> _uiState.update { it.copy(cerrandoSesion = false, error = e.comoErrorApp()) } }
        }
    }

    private fun mostrarPerfil(perfil: Perfil) =
        _uiState.update {
            it.copy(perfil = perfil, racha = perfil.rachaVigente(Formato.hoy()), cargando = false, errorCarga = null)
        }

    companion object {
        val Factory = fabricaDe { PerfilViewModel(it.authRepository, it.perfilRepository) }
    }
}