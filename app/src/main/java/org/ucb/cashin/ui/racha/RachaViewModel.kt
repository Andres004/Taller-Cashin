package org.ucb.cashin.ui.racha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ucb.cashin.data.PerfilRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.DiaRacha
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.model.semanaDeRacha
import org.ucb.cashin.util.Formato

data class RachaUiState(
    val racha: Int = 0,
    val rachaMaxima: Int = 0,
    val anotoHoy: Boolean = false,
    val nivel: Int = 1,
    val xp: Int = 0,
    val semana: List<DiaRacha> = emptyList(),
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
)

/** HU-24: el detalle de la racha. Todo sale del perfil: la base de datos ya lleva la cuenta. */
class RachaViewModel(private val perfilRepository: PerfilRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RachaUiState())
    val uiState: StateFlow<RachaUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            perfilRepository.obtener()
                .onSuccess { perfil ->
                    val hoy = Formato.hoy()
                    val racha = perfil.rachaVigente(hoy)
                    val anotoHoy = perfil.anotoHoy(hoy)
                    _uiState.update {
                        it.copy(
                            racha = racha,
                            rachaMaxima = perfil.rachaMaxima,
                            anotoHoy = anotoHoy,
                            nivel = perfil.nivel,
                            xp = perfil.xp,
                            semana = semanaDeRacha(hoy, racha, anotoHoy),
                            cargando = false,
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) } }
        }
    }

    companion object {
        val Factory = fabricaDe { RachaViewModel(it.perfilRepository) }
    }
}
