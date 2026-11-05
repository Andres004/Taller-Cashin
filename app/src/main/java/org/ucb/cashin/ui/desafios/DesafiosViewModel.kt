package org.ucb.cashin.ui.desafios

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ucb.cashin.R
import org.ucb.cashin.data.DesafioRepository
import org.ucb.cashin.data.ResumenRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.Desafio
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.EstadoDesafio
import org.ucb.cashin.model.PeriodoDesafio
import org.ucb.cashin.model.ProgresoDesafio
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.prediccion.PropuestaDesafio
import org.ucb.cashin.prediccion.proponerDesafios
import org.ucb.cashin.util.Formato

/** Todo lo que la pantalla de Desafíos necesita para dibujarse. */
data class DesafiosUiState(
    val cargando: Boolean = true,
    val error: ErrorApp? = null,

    /** En curso: como máximo uno semanal y uno mensual. */
    val activos: List<Desafio> = emptyList(),
    /** Progreso de cada activo, por id de desafío. */
    val progreso: Map<String, ProgresoDesafio> = emptyMap(),
    /** Últimos cumplidos y fallidos. */
    val historial: List<Desafio> = emptyList(),

    /** HU-22: propuestas del motor, solo para el periodo que no tiene un desafío activo. */
    val propuestasSemanales: List<PropuestaDesafio> = emptyList(),
    val propuestasMensuales: List<PropuestaDesafio> = emptyList(),

    /** HU-23: lo que cerró la última evaluación (para celebrar o animar). */
    val recienEvaluados: List<Desafio> = emptyList(),

    /** Propuesta que se está aceptando (para mostrar "cargando" en su botón). */
    val aceptando: PropuestaDesafio? = null,
    /** Desafío que se está descartando. */
    val descartando: String? = null,
    /** Error al aceptar o descartar: texto propio (p. ej. "ya tienes uno activo") o del servidor. */
    @StringRes val errorAccionTexto: Int? = null,
    val errorAccion: ErrorApp? = null,
) {
    /** Nunca aceptó ni terminó un desafío. */
    val vacio: Boolean get() = activos.isEmpty() && historial.isEmpty()

    fun activoDe(periodo: PeriodoDesafio): Desafio? = activos.firstOrNull { it.periodo == periodo }

    /** Cumplidos de la última evaluación (para la celebración). */
    val cumplidosRecientes: List<Desafio>
        get() = recienEvaluados.filter { it.estado == EstadoDesafio.CUMPLIDO }

    /** Fallidos de la última evaluación (para el mensaje de ánimo). */
    val fallidosRecientes: List<Desafio>
        get() = recienEvaluados.filter { it.estado == EstadoDesafio.FALLIDO }
}

/**
 * HU-22: proponer desafíos con el motor (prediccion/GeneradorDesafios.kt) y aceptar uno.
 * HU-23: al abrir, el servidor evalúa los activos (cumplido / fallido) y suma el XP.
 */
class DesafiosViewModel(
    private val desafioRepository: DesafioRepository,
    private val resumenRepository: ResumenRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DesafiosUiState())
    val uiState: StateFlow<DesafiosUiState> = _uiState.asStateFlow()

    /**
     * Primero evalúa (puede cerrar desafíos y sumar XP), luego trae activos, progreso e historial,
     * y con la historia de gastos arma las propuestas de los periodos libres.
     */
    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = it.activos.isEmpty() && it.historial.isEmpty(), error = null) }

            val evaluados = desafioRepository.evaluar().getOrElse { e -> return@launch mostrarError(e) }
            val activos = desafioRepository.listarActivos().getOrElse { e -> return@launch mostrarError(e) }
            val progreso = desafioRepository.progreso().getOrElse { e -> return@launch mostrarError(e) }
            val historial = desafioRepository.listarHistorial().getOrElse { e -> return@launch mostrarError(e) }
            // Si la historia no se puede leer, igual se propone el desafío de anotar días.
            val gastoPorCategoria = resumenRepository.gastoPorCategoria().getOrDefault(emptyList())
            val totales = resumenRepository.historialMensual().getOrDefault(emptyList())

            val hoy = Formato.hoy()
            fun propuestasPara(periodo: PeriodoDesafio) =
                if (activos.any { it.periodo == periodo }) emptyList()
                else proponerDesafios(periodo, gastoPorCategoria, totales, hoy)

            _uiState.update {
                it.copy(
                    cargando = false,
                    activos = activos.sortedBy { d -> d.periodo.ordinal },
                    progreso = progreso.associateBy { p -> p.desafioId },
                    historial = historial,
                    propuestasSemanales = propuestasPara(PeriodoDesafio.SEMANAL),
                    propuestasMensuales = propuestasPara(PeriodoDesafio.MENSUAL),
                    // Al recargar tras aceptar o descartar no hay cambios nuevos: se mantiene el aviso anterior.
                    recienEvaluados = evaluados.ifEmpty { it.recienEvaluados },
                )
            }
        }
    }

    /**
     * Acepta una propuesta. [titulo] y [descripcion] los arma la pantalla con las plantillas
     * de strings.xml; el servidor pone las fechas y el XP.
     */
    fun aceptar(propuesta: PropuestaDesafio, titulo: String, descripcion: String) {
        if (_uiState.value.aceptando != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(aceptando = propuesta, errorAccion = null, errorAccionTexto = null) }
            desafioRepository.aceptar(propuesta, titulo, descripcion)
                .onSuccess {
                    _uiState.update { it.copy(aceptando = null) }
                    cargar()
                }
                .onFailure { e ->
                    _uiState.update {
                        if (e.esYaActivo()) {
                            it.copy(aceptando = null, errorAccionTexto = R.string.desafios_error_ya_activo)
                        } else {
                            it.copy(aceptando = null, errorAccion = e.comoErrorApp())
                        }
                    }
                    // Si ya había uno activo (por ejemplo desde otro celular), la lista se pone al día.
                    if (e.esYaActivo()) cargar()
                }
        }
    }

    /** Descarta un desafío activo. La pantalla ya pidió confirmación. */
    fun descartar(desafioId: String) {
        if (_uiState.value.descartando != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(descartando = desafioId, errorAccion = null, errorAccionTexto = null) }
            desafioRepository.descartar(desafioId)
                .onSuccess {
                    _uiState.update { it.copy(descartando = null) }
                    cargar()
                }
                .onFailure { e -> _uiState.update { it.copy(descartando = null, errorAccion = e.comoErrorApp()) } }
        }
    }

    private fun mostrarError(e: Throwable) =
        _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) }

    /** aceptar_desafio responde "Ya tienes un desafío semanal/mensual activo". */
    private fun Throwable.esYaActivo(): Boolean =
        generateSequence(this) { it.cause }
            .mapNotNull { it.message?.lowercase() }
            .any { "ya tienes un desaf" in it }

    companion object {
        val Factory = fabricaDe { DesafiosViewModel(it.desafioRepository, it.resumenRepository) }
    }
}

