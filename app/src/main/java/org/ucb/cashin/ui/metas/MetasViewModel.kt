package org.ucb.cashin.ui.metas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ucb.cashin.data.MetaRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.AvanceMeta
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.comoErrorApp
import java.math.BigDecimal
import java.math.RoundingMode

/** Todo lo que la pestaña Metas necesita para dibujarse. */
data class MetasUiState(
    /** Primero las que faltan completar, después las alcanzadas. */
    val metas: List<AvanceMeta> = emptyList(),
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
)

/** Si lo ahorrado ya llegó al objetivo. */
val AvanceMeta.alcanzada: Boolean
    get() = totalAhorrado >= montoObjetivo

/** Lo que falta para el objetivo (nunca negativo). */
val AvanceMeta.faltante: BigDecimal
    get() = (montoObjetivo - totalAhorrado).max(BigDecimal.ZERO)

/** Porcentaje entero de avance, de 0 a 100 (se calcula con BigDecimal, sin Float). */
val AvanceMeta.porcentaje: Int
    get() {
        if (montoObjetivo.signum() <= 0) return 0
        return totalAhorrado
            .multiply(BigDecimal(100))
            .divide(montoObjetivo, 0, RoundingMode.DOWN)
            .toInt()
            .coerceIn(0, 100)
    }

/** HU-19: lista de metas con su avance (vista v_avance_meta). */
class MetasViewModel(
    private val metaRepository: MetaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetasUiState())
    val uiState: StateFlow<MetasUiState> = _uiState.asStateFlow()

    /** Se llama cada vez que se abre la pestaña (después de crear una meta o anotar un ahorro). */
    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = it.metas.isEmpty(), error = null) }
            metaRepository.listarAvances()
                .onSuccess { lista ->
                    _uiState.update { it.copy(metas = lista.sortedBy { meta -> meta.alcanzada }, cargando = false) }
                }
                .onFailure { e -> _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) } }
        }
    }

    companion object {
        val Factory = fabricaDe { MetasViewModel(it.metaRepository) }
    }
}