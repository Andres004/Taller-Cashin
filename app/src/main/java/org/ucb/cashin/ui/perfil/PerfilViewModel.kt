package org.ucb.cashin.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ucb.cashin.data.AuthRepository
import org.ucb.cashin.data.PerfilRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.EstadisticasPerfil
import org.ucb.cashin.model.Perfil
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.notificaciones.RecordatorioLocal
import org.ucb.cashin.prediccion.AjusteRecordatorio
import org.ucb.cashin.util.Formato

/** Todo lo que la pantalla Perfil necesita para dibujarse. */
data class PerfilUiState(
    val correo: String = "",
    val perfil: Perfil? = null,
    val racha: Int = 0,
    /** Null si todavía no se pudieron contar (por ejemplo, sin la migración 010): se muestra "—". */
    val estadisticas: EstadisticasPerfil? = null,
    val recordatorio: AjusteRecordatorio = AjusteRecordatorio(),
    val cargando: Boolean = true,
    val errorCarga: ErrorApp? = null,
    val cerrandoSesion: Boolean = false,
    val error: ErrorApp? = null,
)

/**
 * HU-05: el perfil con avatar, nivel, XP, racha y los números de todo lo que hizo en la app.
 * Lo que se edita está en "Editar perfil". HU-04: cerrar sesión.
 */
class PerfilViewModel(
    private val auth: AuthRepository,
    private val perfilRepository: PerfilRepository,
    private val recordatorio: RecordatorioLocal,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerfilUiState(correo = auth.correoActual().orEmpty()))
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    /** Trae el perfil y sus números. Se llama cada vez que se abre la pestaña, así todo está al día. */
    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = it.perfil == null, errorCarga = null, recordatorio = recordatorio.ajuste()) }
            val estadisticas = async { perfilRepository.estadisticas() }
            perfilRepository.obtener()
                .onSuccess { perfil ->
                    _uiState.update {
                        it.copy(perfil = perfil, racha = perfil.rachaVigente(Formato.hoy()), cargando = false, errorCarga = null)
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(cargando = false, errorCarga = e.comoErrorApp()) } }
            // Los números son un extra: si fallan, el resto del perfil se ve igual.
            estadisticas.await().onSuccess { numeros -> _uiState.update { it.copy(estadisticas = numeros) } }
        }
    }

    /** Al cerrar sesión, CashinRaiz vuelve sola a la pantalla de Bienvenida. */
    fun cerrarSesion() {
        viewModelScope.launch {
            _uiState.update { it.copy(cerrandoSesion = true, error = null) }
            auth.cerrarSesion()
                .onFailure { e -> _uiState.update { it.copy(cerrandoSesion = false, error = e.comoErrorApp()) } }
        }
    }

    companion object {
        val Factory = fabricaDe { PerfilViewModel(it.authRepository, it.perfilRepository, it.recordatorio) }
    }
}
