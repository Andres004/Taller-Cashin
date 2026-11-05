package org.ucb.cashin.ui.desafios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ucb.cashin.data.DesafioRepository
import org.ucb.cashin.data.RenovadorDesafios
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.Desafio
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.EstadoDesafio
import org.ucb.cashin.model.PeriodoDesafio
import org.ucb.cashin.model.ProgresoDesafio
import org.ucb.cashin.model.comoErrorApp

/** Todo lo que la pantalla de Desafíos necesita para dibujarse. */
data class DesafiosUiState(
    val cargando: Boolean = true,
    val error: ErrorApp? = null,

    /** En curso: los 3 de la semana y el del mes (llegan solos). */
    val activos: List<Desafio> = emptyList(),
    /** Progreso de cada activo, por id de desafío. */
    val progreso: Map<String, ProgresoDesafio> = emptyMap(),
    /** Últimos cumplidos y fallidos, del más reciente al más antiguo. */
    val historial: List<Desafio> = emptyList(),

    /** Ids de los desafíos que acaban de llegar (aviso "¡Nuevos desafíos disponibles!" y etiqueta NUEVO). */
    val nuevos: Set<String> = emptySet(),
    /** HU-23: desafíos que la evaluación acaba de cerrar y todavía no se mostraron (animación). */
    val resultadosPorMostrar: List<Desafio> = emptyList(),

    /** Desafío que se está descartando. */
    val descartando: String? = null,
    val errorAccion: ErrorApp? = null,
) {
    val semanales: List<Desafio> get() = activos.filter { it.periodo == PeriodoDesafio.SEMANAL }
    val mensuales: List<Desafio> get() = activos.filter { it.periodo == PeriodoDesafio.MENSUAL }

    /** Cuántos desafíos cumplió (de los últimos del historial). */
    val cumplidos: Int get() = historial.count { it.estado == EstadoDesafio.CUMPLIDO }

    /** XP que ganó con esos desafíos cumplidos. */
    val xpGanado: Int get() = historial.filter { it.estado == EstadoDesafio.CUMPLIDO }.sumOf { it.xpRecompensa }

    /** Desafíos cumplidos seguidos, contando desde el más reciente. */
    val cumplidosSeguidos: Int get() = historial.takeWhile { it.estado == EstadoDesafio.CUMPLIDO }.size
}

/**
 * HU-22: los desafíos llegan solos. Al abrir, el servidor evalúa los activos (HU-23: cumplido o
 * fallido, con su XP) y el renovador asigna los de la semana o el mes si todavía no llegaron.
 */
class DesafiosViewModel(
    private val desafioRepository: DesafioRepository,
    private val renovadorDesafios: RenovadorDesafios,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DesafiosUiState())
    val uiState: StateFlow<DesafiosUiState> = _uiState.asStateFlow()

    /**
     * Primero evalúa (cierra lo de la semana o mes pasado y suma XP), luego asigna lo nuevo
     * y al final trae activos, progreso e historial.
     */
    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = it.activos.isEmpty() && it.historial.isEmpty(), error = null) }

            val evaluados = desafioRepository.evaluar().getOrElse { e -> return@launch mostrarError(e) }
            val llegaron = renovadorDesafios.renovar()
            val activos = desafioRepository.listarActivos().getOrElse { e -> return@launch mostrarError(e) }
            val progreso = desafioRepository.progreso().getOrElse { e -> return@launch mostrarError(e) }
            val historial = desafioRepository.listarHistorial().getOrElse { e -> return@launch mostrarError(e) }

            _uiState.update {
                it.copy(
                    cargando = false,
                    activos = activos.sortedWith(compareBy({ d -> d.periodo.ordinal }, { d -> d.titulo })),
                    progreso = progreso.associateBy { p -> p.desafioId },
                    historial = historial,
                    nuevos = it.nuevos + llegaron.map { d -> d.id },
                    resultadosPorMostrar = it.resultadosPorMostrar + evaluados
                        .filter { d -> d.estado == EstadoDesafio.CUMPLIDO || d.estado == EstadoDesafio.FALLIDO },
                )
            }
        }
    }

    /** Descarta un desafío activo. La pantalla ya pidió confirmación. No llega otro hasta el próximo periodo. */
    fun descartar(desafioId: String) {
        if (_uiState.value.descartando != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(descartando = desafioId, errorAccion = null) }
            desafioRepository.descartar(desafioId)
                .onSuccess {
                    _uiState.update { it.copy(descartando = null) }
                    cargar()
                }
                .onFailure { e -> _uiState.update { it.copy(descartando = null, errorAccion = e.comoErrorApp()) } }
        }
    }

    /** La animación del primer resultado pendiente ya se vio: pasa al siguiente (si hay). */
    fun resultadoVisto() = _uiState.update { it.copy(resultadosPorMostrar = it.resultadosPorMostrar.drop(1)) }

    /** El aviso de desafíos nuevos ya se vio. */
    fun nuevosVistos() = _uiState.update { it.copy(nuevos = emptySet()) }

    private fun mostrarError(e: Throwable) =
        _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) }

    companion object {
        val Factory = fabricaDe { DesafiosViewModel(it.desafioRepository, it.renovadorDesafios) }
    }
}
