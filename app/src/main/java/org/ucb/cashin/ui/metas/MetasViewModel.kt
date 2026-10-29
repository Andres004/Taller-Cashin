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
import org.ucb.cashin.model.EstadoMeta
import org.ucb.cashin.model.comoErrorApp
import java.math.BigDecimal
import java.math.RoundingMode

/** Todo lo que la pestaña Metas necesita para dibujarse. */
data class MetasUiState(
    /** Metas ACTIVAS (las alcanzadas que aún no se marcaron como completadas también van aquí). */
    val activas: List<AvanceMeta> = emptyList(),
    /** Metas marcadas como COMPLETADAS, en su sección plegable. */
    val completadas: List<AvanceMeta> = emptyList(),
    /** Si la sección "Completadas" está abierta. */
    val verCompletadas: Boolean = false,
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
) {
    val sinMetas: Boolean get() = activas.isEmpty() && completadas.isEmpty()
}

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

/** HU-19: lista de metas con su avance (vista v_avance_meta), separando las completadas. */
class MetasViewModel(
    private val metaRepository: MetaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetasUiState())
    val uiState: StateFlow<MetasUiState> = _uiState.asStateFlow()

    /** Se llama cada vez que se abre la pestaña (después de crear una meta o anotar un ahorro). */
    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = it.sinMetas, error = null) }
            metaRepository.listarAvances()
                .onSuccess { lista ->
                    val (completadas, activas) = lista.partition { meta -> meta.estado == EstadoMeta.COMPLETADA }
                    _uiState.update { it.copy(activas = activas, completadas = completadas, cargando = false) }
                }
                .onFailure { e -> _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) } }
        }
    }

    fun onVerCompletadasCambio() = _uiState.update { it.copy(verCompletadas = !it.verCompletadas) }

    companion object {
        val Factory = fabricaDe { MetasViewModel(it.metaRepository) }
    }
}