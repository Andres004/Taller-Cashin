package org.ucb.cashin.ui.metas

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ucb.cashin.R
import org.ucb.cashin.data.MetaRepository
import org.ucb.cashin.data.ResumenRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.AporteMeta
import org.ucb.cashin.model.AvanceMeta
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.EstadoMeta
import org.ucb.cashin.model.NuevoAporteMeta
import org.ucb.cashin.model.TipoAporte
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.prediccion.EstimacionMeta
import org.ucb.cashin.prediccion.FuenteAhorro
import org.ucb.cashin.prediccion.MensajeAvatar
import org.ucb.cashin.prediccion.construirSerie
import org.ucb.cashin.prediccion.estimarMeta
import org.ucb.cashin.prediccion.mensajeParaMeta
import org.ucb.cashin.prediccion.pronosticarSiguienteMes
import java.time.YearMonth
import org.ucb.cashin.util.Formato

/** Todo lo que la pantalla de una meta necesita para dibujarse. */
data class DetalleMetaUiState(
    val avance: AvanceMeta? = null,
    /** Ahorros y retiros, del más reciente al más antiguo. */
    val aportes: List<AporteMeta> = emptyList(),
    val cargando: Boolean = true,
    val error: ErrorApp? = null,

    /** HU-20: "¿Cuándo lo logro?" calculado por el motor de predicción. */
    val estimacion: EstimacionMeta? = null,
    val mensajeIa: MensajeAvatar? = null,
    /** Cuántos meses de datos usó la estimación (para mostrar la confianza). */
    val mesesDeDatos: Int = 0,

    /** Diálogo "Anoté un ahorro / retiro": null = cerrado. */
    val dialogoTipo: TipoAporte? = null,
    val montoAporte: String = "",
    val notaAporte: String = "",
    @StringRes val errorMontoAporte: Int? = null,
    val errorAporte: ErrorApp? = null,
    val guardandoAporte: Boolean = false,

    /** Completar o cancelar la meta en curso. */
    val cambiandoEstado: Boolean = false,
    val errorEstado: ErrorApp? = null,
    /** La meta se canceló: la pantalla vuelve atrás. */
    val cancelada: Boolean = false,
) {
    /** Ya está marcada como completada: no se anotan más ahorros ni retiros. */
    val completada: Boolean
        get() = avance?.estado == EstadoMeta.COMPLETADA
}

/**
 * HU-18: anotar un ahorro o un retiro (el retiro no supera lo ahorrado).
 * HU-19: ver el avance de la meta con barra, monto y porcentaje.
 * HU-20: estimar cuándo se logra con el motor de predicción (el ViewModel solo lo orquesta).
 * Al llegar al 100 % se puede marcar como completada.
 */
class DetalleMetaViewModel(
    private val metaRepository: MetaRepository,
    private val resumenRepository: ResumenRepository,
    private val metaId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetalleMetaUiState())
    val uiState: StateFlow<DetalleMetaUiState> = _uiState.asStateFlow()

    /**
     * Trae el avance (de la vista), los ahorros y retiros, y la historia de meses
     * para la estimación. La pantalla lo llama cada vez que se abre; también se llama
     * después de anotar, así la estimación se recalcula sola.
     */
    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = it.avance == null, error = null) }
            val avance = metaRepository.obtenerAvance(metaId).getOrElse { e ->
                _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) }
                return@launch
            }
            val aportes = metaRepository.listarAportes(metaId).getOrElse { e ->
                _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) }
                return@launch
            }
            // Si la historia no se puede leer, la meta igual se muestra: la estimación queda "sin datos".
            val historial = resumenRepository.historialMensual().getOrDefault(emptyList())

            val hoy = Formato.hoy()
            val pronosticoIngresos = pronosticarSiguienteMes(construirSerie(historial, hoy) { it.totalIngresos })
            val pronosticoGastos = pronosticarSiguienteMes(construirSerie(historial, hoy) { it.totalGastos })
            val estimacion = estimarMeta(avance, aportes, pronosticoIngresos, pronosticoGastos, hoy)

            val mesesDeDatos = when (estimacion.fuente) {
                FuenteAhorro.APORTES -> aportes.map { YearMonth.from(it.fecha) }.distinct().size.coerceAtMost(3)
                FuenteAhorro.CAPACIDAD -> minOf(pronosticoIngresos.mesesUsados, pronosticoGastos.mesesUsados)
                FuenteAhorro.SIN_DATOS -> 0
            }

            _uiState.update {
                it.copy(
                    avance = avance,
                    aportes = aportes,
                    estimacion = estimacion,
                    mensajeIa = mensajeParaMeta(estimacion),
                    mesesDeDatos = mesesDeDatos,
                    cargando = false,
                )
            }
        }
    }

    fun abrirDialogo(tipo: TipoAporte) {
        if (_uiState.value.completada) return
        _uiState.update {
            it.copy(dialogoTipo = tipo, montoAporte = "", notaAporte = "", errorMontoAporte = null, errorAporte = null)
        }
    }

    fun cerrarDialogo() {
        if (_uiState.value.guardandoAporte) return
        _uiState.update { it.copy(dialogoTipo = null) }
    }

    fun onMontoAporteCambio(valor: String) =
        _uiState.update { it.copy(montoAporte = valor, errorMontoAporte = null, errorAporte = null) }

    fun onNotaAporteCambio(valor: String) =
        _uiState.update { it.copy(notaAporte = valor.take(MAX_NOTA)) }

    /**
     * Guarda el ahorro o retiro con fecha de hoy (hora de Bolivia).
     * Un retiro mayor a lo ahorrado se frena aquí y, por si acaso, también en la base de datos.
     */
    fun guardarAporte() {
        val estado = _uiState.value
        val tipo = estado.dialogoTipo ?: return
        val avance = estado.avance ?: return
        if (estado.guardandoAporte) return

        val monto = Formato.parsearMonto(estado.montoAporte)?.takeIf { it.signum() > 0 }
        if (monto == null) {
            _uiState.update { it.copy(errorMontoAporte = R.string.validacion_monto) }
            return
        }
        if (tipo == TipoAporte.RETIRO && monto > avance.totalAhorrado) {
            _uiState.update { it.copy(errorMontoAporte = R.string.metas_error_retiro) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(guardandoAporte = true, errorAporte = null) }
            metaRepository.anotarAporte(
                NuevoAporteMeta(
                    metaId = metaId,
                    tipo = tipo,
                    monto = monto,
                    fecha = Formato.hoy(),
                    nota = estado.notaAporte.trim().ifBlank { null },
                ),
            )
                .onSuccess {
                    _uiState.update { it.copy(guardandoAporte = false, dialogoTipo = null) }
                    cargar()
                }
                .onFailure { e ->
                    _uiState.update {
                        if (e.esRetiroMayor()) {
                            it.copy(guardandoAporte = false, errorMontoAporte = R.string.metas_error_retiro)
                        } else {
                            it.copy(guardandoAporte = false, errorAporte = e.comoErrorApp())
                        }
                    }
                }
        }
    }

    /** "Marcar como completada": solo cuando ya llegó al objetivo. (El XP llega en el Sprint 4.) */
    fun completarMeta() {
        val avance = _uiState.value.avance ?: return
        if (!avance.alcanzada || _uiState.value.cambiandoEstado) return
        viewModelScope.launch {
            _uiState.update { it.copy(cambiandoEstado = true, errorEstado = null) }
            metaRepository.cambiarEstado(metaId, EstadoMeta.COMPLETADA)
                .onSuccess {
                    _uiState.update { it.copy(cambiandoEstado = false) }
                    cargar()
                }
                .onFailure { e -> _uiState.update { it.copy(cambiandoEstado = false, errorEstado = e.comoErrorApp()) } }
        }
    }

    /** Quita la meta (queda CANCELADA). La pantalla ya pidió confirmación. */
    fun cancelarMeta() {
        if (_uiState.value.cambiandoEstado) return
        viewModelScope.launch {
            _uiState.update { it.copy(cambiandoEstado = true, errorEstado = null) }
            metaRepository.cambiarEstado(metaId, EstadoMeta.CANCELADA)
                .onSuccess { _uiState.update { it.copy(cambiandoEstado = false, cancelada = true) } }
                .onFailure { e -> _uiState.update { it.copy(cambiandoEstado = false, errorEstado = e.comoErrorApp()) } }
        }
    }

    /** El trigger fn_validar_retiro responde "El retiro supera lo ahorrado en la meta". */
    private fun Throwable.esRetiroMayor(): Boolean =
        generateSequence(this) { it.cause }
            .mapNotNull { it.message?.lowercase() }
            .any { "supera lo ahorrado" in it }

    companion object {
        /** Igual que varchar(200) de aporte_meta.nota. */
        const val MAX_NOTA = 200

        fun fabrica(metaId: String) = fabricaDe { DetalleMetaViewModel(it.metaRepository, it.resumenRepository, metaId) }
    }
}