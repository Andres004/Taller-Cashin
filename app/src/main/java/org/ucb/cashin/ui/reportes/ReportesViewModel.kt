package org.ucb.cashin.ui.reportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ucb.cashin.data.CategoriaRepository
import org.ucb.cashin.data.PlanificadoRepository
import org.ucb.cashin.data.ReporteRepository
import org.ucb.cashin.data.ResumenRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.GastoCategoriaMes
import org.ucb.cashin.model.IngresoCategoriaMes
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.model.TotalesMes
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.prediccion.DesgloseGastos
import org.ucb.cashin.prediccion.DivisionGastoMes
import org.ucb.cashin.prediccion.FraseReporte
import org.ucb.cashin.prediccion.MesResumen
import org.ucb.cashin.prediccion.ReporteCategorias
import org.ucb.cashin.prediccion.desglosarGastos
import org.ucb.cashin.prediccion.dividirGastoDelMes
import org.ucb.cashin.prediccion.fraseGastos
import org.ucb.cashin.prediccion.fraseIngresos
import org.ucb.cashin.prediccion.fraseMeses
import org.ucb.cashin.prediccion.reportePorCategoria
import org.ucb.cashin.prediccion.ultimosMeses
import org.ucb.cashin.util.Formato
import java.time.YearMonth

/** Las tres vistas de la pestaña Reportes. */
enum class VistaReporte { GASTOS, INGRESOS, MESES }

/** Todo lo que la pestaña Reportes necesita para dibujarse. */
data class ReportesUiState(
    val vista: VistaReporte = VistaReporte.GASTOS,
    val mes: YearMonth = YearMonth.from(Formato.hoy()),
    /** No se puede pasar de este mes (no hay reportes del futuro). */
    val mesActual: YearMonth = YearMonth.from(Formato.hoy()),
    /** Primer mes con anotaciones: no se puede ir más atrás. */
    val primerMes: YearMonth = YearMonth.from(Formato.hoy()),
    val gastos: ReporteCategorias? = null,
    val ingresos: ReporteCategorias? = null,
    val meses: List<MesResumen> = emptyList(),
    /** Null si no se pudo leer lo de pagos fijos: la tarjeta simplemente no se muestra. */
    val division: DivisionGastoMes? = null,
    val fraseGastos: FraseReporte? = null,
    val fraseIngresos: FraseReporte? = null,
    val fraseMeses: FraseReporte? = null,
    /** Categoría tocada en la dona (Gastos o Ingresos). Null = ninguna. */
    val seleccionadaId: String? = null,
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
) {
    val puedeRetroceder: Boolean get() = mes > primerMes
    val puedeAvanzar: Boolean get() = mes < mesActual

    /** El reporte de la vista elegida (null en Meses). */
    val reporteVisible: ReporteCategorias?
        get() = when (vista) {
            VistaReporte.GASTOS -> gastos
            VistaReporte.INGRESOS -> ingresos
            VistaReporte.MESES -> null
        }

    val tipoVisible: TipoRegistro get() = if (vista == VistaReporte.INGRESOS) TipoRegistro.INGRESO else TipoRegistro.GASTO
}

/**
 * HU-14: gastos e ingresos por categoría, comparación con el mes anterior y los últimos 6 meses.
 * Los datos de todos los meses se leen una vez; cambiar de mes o de vista solo recalcula.
 */
class ReportesViewModel(
    private val resumenRepository: ResumenRepository,
    private val reporteRepository: ReporteRepository,
    private val planificadoRepository: PlanificadoRepository,
    private val categoriaRepository: CategoriaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportesUiState())
    val uiState: StateFlow<ReportesUiState> = _uiState.asStateFlow()

    private var gastoPorCategoria: List<GastoCategoriaMes> = emptyList()
    private var ingresoPorCategoria: List<IngresoCategoriaMes> = emptyList()
    private var historial: List<TotalesMes> = emptyList()
    private var desglose: DesgloseGastos? = null
    private var importancias: Map<String, Int> = emptyMap()

    /** Se llama cada vez que se abre la pestaña. Si ya hay datos, se actualizan sin mostrar la carga. */
    fun cargar() {
        viewModelScope.launch {
            val yaHabia = _uiState.value.gastos != null
            _uiState.update { it.copy(cargando = !yaHabia, error = null) }

            val gastos = async { resumenRepository.gastoPorCategoria() }
            val ingresos = async { reporteRepository.ingresoPorCategoria() }
            val meses = async { resumenRepository.historialMensual() }
            val planificados = async { planificadoRepository.listar() }
            val categorias = async { categoriaRepository.listarConArchivadas(TipoRegistro.GASTO) }

            gastoPorCategoria = gastos.await().getOrElse { return@launch mostrarError(it) }
            ingresoPorCategoria = ingresos.await().getOrElse { return@launch mostrarError(it) }
            historial = meses.await().getOrElse { return@launch mostrarError(it) }

            // Lo de pagos fijos es un extra: si falla, se muestra todo lo demás.
            importancias = categorias.await().getOrNull()?.associate { it.id to it.importancia }.orEmpty()
            desglose = planificados.await().getOrNull()?.let { lista ->
                desglosarGastos(lista, gastoPorCategoria, importancias, Formato.hoy())
            }

            val hoy = YearMonth.from(Formato.hoy())
            val primero = historial.minOfOrNull { YearMonth.from(it.mes) } ?: hoy
            _uiState.update {
                it.copy(
                    mesActual = hoy,
                    primerMes = minOf(primero, hoy),
                    mes = it.mes.coerceIn(minOf(primero, hoy), hoy),
                    cargando = false,
                )
            }
            recalcular()
        }
    }

    fun onVistaCambio(vista: VistaReporte) {
        _uiState.update { it.copy(vista = vista, seleccionadaId = null) }
    }

    fun onMesAnterior() = moverMes(-1)

    fun onMesSiguiente() = moverMes(1)

    /** Tocar una porción o una fila la elige; tocarla otra vez la suelta. */
    fun onCategoriaTocada(categoriaId: String) {
        _uiState.update { it.copy(seleccionadaId = if (it.seleccionadaId == categoriaId) null else categoriaId) }
    }

    private fun moverMes(pasos: Long) {
        _uiState.update {
            val nuevo = it.mes.plusMonths(pasos)
            if (nuevo < it.primerMes || nuevo > it.mesActual) it else it.copy(mes = nuevo, seleccionadaId = null)
        }
        recalcular()
    }

    /** Arma los tres reportes del mes elegido con lo que ya se cargó. */
    private fun recalcular() {
        _uiState.update { estado ->
            val gastos = reportePorCategoria(gastoPorCategoria, estado.mes)
            val ingresos = reportePorCategoria(ingresoPorCategoria, estado.mes)
            val serie = ultimosMeses(historial, estado.mes)
            val sobrante = serie.lastOrNull()?.takeIf { it.mes == estado.mes }?.sobrante
            estado.copy(
                gastos = gastos,
                ingresos = ingresos,
                meses = serie,
                division = desglose?.let { dividirGastoDelMes(gastos, it, importancias) }
                    ?.takeIf { gastos.total.signum() > 0 },
                fraseGastos = fraseGastos(gastos),
                fraseIngresos = fraseIngresos(ingresos, sobrante),
                fraseMeses = fraseMeses(serie),
            )
        }
    }

    private fun mostrarError(e: Throwable) =
        _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) }

    companion object {
        val Factory = fabricaDe {
            ReportesViewModel(it.resumenRepository, it.reporteRepository, it.planificadoRepository, it.categoriaRepository)
        }
    }
}
