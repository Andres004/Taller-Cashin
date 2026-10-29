package org.ucb.cashin.ui.registros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ucb.cashin.data.CategoriaRepository
import org.ucb.cashin.data.RegistroRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.Categoria
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.Registro
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.util.Formato
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** Una anotación junto con su categoría, lista para mostrarse. */
data class RegistroConCategoria(
    val registro: Registro,
    val categoria: Categoria?,
)

/** Las anotaciones de un mismo día. */
data class DiaHistorial(
    val fecha: LocalDate,
    val registros: List<RegistroConCategoria>,
) {
    /** Ingresos menos gastos de ese día, contando solo lo que se está mostrando. */
    val total: BigDecimal
        get() = registros.fold(BigDecimal.ZERO) { suma, anotacion ->
            if (anotacion.registro.tipo == TipoRegistro.INGRESO) suma + anotacion.registro.monto
            else suma - anotacion.registro.monto
        }
}

/** Qué tipo de anotaciones muestra el historial. */
enum class FiltroTipo { TODO, GASTOS, INGRESOS }

/** Qué fechas muestra el historial (HU-10). */
enum class FiltroPeriodo { HOY, SEMANA, MES, PERSONALIZADO }

/** Rango de fechas elegido en el calendario, con ambos extremos incluidos. */
data class RangoFechas(
    val desde: LocalDate,
    val hasta: LocalDate,
) {
    fun contiene(fecha: LocalDate): Boolean = !fecha.isBefore(desde) && !fecha.isAfter(hasta)

    companion object {
        /** Semana de lunes a domingo que contiene a [fecha]. */
        fun semanaDe(fecha: LocalDate): RangoFechas {
            val lunes = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            return RangoFechas(desde = lunes, hasta = lunes.plusDays(6))
        }
    }
}

/** Todo lo que la pantalla Historial necesita para dibujarse. */
data class HistorialUiState(
    val dias: List<DiaHistorial> = emptyList(),
    val filtro: FiltroTipo = FiltroTipo.TODO,
    val periodo: FiltroPeriodo = FiltroPeriodo.MES,
    /** Mes que se está viendo cuando el periodo es MES. Null solo mientras no hay ninguna anotación. */
    val mes: YearMonth? = null,
    /** Meses que tienen al menos una anotación, del más reciente al más antiguo. */
    val mesesDisponibles: List<YearMonth> = emptyList(),
    /** Fechas elegidas en el calendario cuando el periodo es PERSONALIZADO. */
    val rango: RangoFechas? = null,
    /** Categoría elegida. Null = todas. */
    val categoriaId: String? = null,
    /** Categorías que tienen anotaciones del tipo elegido, ordenadas por nombre. */
    val categoriasDisponibles: List<Categoria> = emptyList(),
    /** False si el usuario todavía no anotó nada (distinto de "el filtro no encontró nada"). */
    val hayAnotaciones: Boolean = false,
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
)

/** Junta cada anotación con su categoría. Si la categoría no aparece, queda vacía. */
fun unirConCategorias(registros: List<Registro>, categorias: List<Categoria>): List<RegistroConCategoria> {
    val categoriaPorId = categorias.associateBy { it.id }
    return registros.map { RegistroConCategoria(it, categoriaPorId[it.categoriaId]) }
}

/**
 * HU-09: anotaciones de la más reciente a la más antigua, agrupadas por día.
 * HU-10: filtros por tipo, categoría y fechas (hoy, esta semana, un mes o un rango personalizado).
 * Los filtros se aplican sobre lo que ya se cargó, sin volver a llamar a Supabase.
 */
class HistorialViewModel(
    private val registroRepository: RegistroRepository,
    private val categoriaRepository: CategoriaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    /** Todas las anotaciones del usuario, sin filtrar. */
    private var todas: List<RegistroConCategoria> = emptyList()

    /**
     * Carga el historial. Se llama cada vez que se abre la pantalla, así siempre
     * muestra lo último. Si ya hay datos, se actualizan sin mostrar la carga otra vez.
     * Los filtros elegidos se mantienen.
     */
    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = !it.hayAnotaciones, error = null) }

            // Se piden todas, incluso las archivadas, para que el historial siga completo.
            val categorias = categoriaRepository.listarTodas().getOrElse { e ->
                mostrarError(e)
                return@launch
            }
            val registros = registroRepository.listar().getOrElse { e ->
                mostrarError(e)
                return@launch
            }

            todas = unirConCategorias(registros, categorias)
            val meses = todas.map { mesDe(it) }.distinct()

            _uiState.update {
                it.copy(
                    mesesDisponibles = meses,
                    // Se mantiene el mes elegido; si ya no tiene anotaciones, se muestra el más reciente.
                    mes = it.mes?.takeIf { mes -> mes in meses } ?: meses.firstOrNull(),
                    hayAnotaciones = todas.isNotEmpty(),
                    cargando = false,
                )
            }
            aplicarFiltros()
        }
    }

    fun onFiltroCambio(filtro: FiltroTipo) {
        _uiState.update { it.copy(filtro = filtro) }
        aplicarFiltros()
    }

    /** Null = todas las categorías. */
    fun onCategoriaCambio(categoriaId: String?) {
        _uiState.update { it.copy(categoriaId = categoriaId) }
        aplicarFiltros()
    }

    /** Hoy o esta semana. El mes y el rango personalizado tienen su propia función. */
    fun onPeriodoCambio(periodo: FiltroPeriodo) {
        _uiState.update { it.copy(periodo = periodo) }
        aplicarFiltros()
    }

    fun onMesCambio(mes: YearMonth) {
        _uiState.update { it.copy(periodo = FiltroPeriodo.MES, mes = mes) }
        aplicarFiltros()
    }

    /** Fechas elegidas en el calendario. Si vienen al revés, se ordenan. */
    fun onRangoCambio(desde: LocalDate, hasta: LocalDate) {
        val rango = if (desde.isAfter(hasta)) RangoFechas(hasta, desde) else RangoFechas(desde, hasta)
        _uiState.update { it.copy(periodo = FiltroPeriodo.PERSONALIZADO, rango = rango) }
        aplicarFiltros()
    }

    /**
     * Arma los días con las anotaciones que cumplen todos los filtros.
     * También recalcula las categorías que se pueden elegir según el tipo.
     */
    private fun aplicarFiltros() {
        _uiState.update { estado ->
            val delTipo = todas.filter { cumpleTipo(it, estado.filtro) }

            val categorias = delTipo
                .mapNotNull { it.categoria }
                .distinctBy { it.id }
                .sortedBy { it.nombre.lowercase() }

            // Si la categoría elegida no tiene anotaciones del tipo actual, se vuelve a "todas".
            val categoriaId = estado.categoriaId?.takeIf { id -> categorias.any { it.id == id } }
            val hoy = Formato.hoy()

            val visibles = delTipo.filter { anotacion ->
                (categoriaId == null || anotacion.registro.categoriaId == categoriaId) &&
                        cumplePeriodo(Formato.fechaDe(anotacion.registro.fechaHora), estado, hoy)
            }

            estado.copy(
                dias = agruparPorDia(visibles),
                categoriasDisponibles = categorias,
                categoriaId = categoriaId,
            )
        }
    }

    private fun cumpleTipo(anotacion: RegistroConCategoria, filtro: FiltroTipo): Boolean = when (filtro) {
        FiltroTipo.TODO -> true
        FiltroTipo.GASTOS -> anotacion.registro.tipo == TipoRegistro.GASTO
        FiltroTipo.INGRESOS -> anotacion.registro.tipo == TipoRegistro.INGRESO
    }

    /** Todas las fechas se comparan en hora de Bolivia. */
    private fun cumplePeriodo(fecha: LocalDate, estado: HistorialUiState, hoy: LocalDate): Boolean =
        when (estado.periodo) {
            FiltroPeriodo.HOY -> fecha == hoy
            FiltroPeriodo.SEMANA -> RangoFechas.semanaDe(hoy).contiene(fecha)
            FiltroPeriodo.MES -> YearMonth.from(fecha) == estado.mes
            FiltroPeriodo.PERSONALIZADO -> estado.rango?.contiene(fecha) ?: true
        }

    /** Mes de la anotación en hora de Bolivia. */
    private fun mesDe(anotacion: RegistroConCategoria): YearMonth =
        YearMonth.from(Formato.fechaDe(anotacion.registro.fechaHora))

    private fun mostrarError(e: Throwable) =
        _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) }

    /** Agrupa por día en hora de Bolivia. Como la lista ya viene ordenada, los días también. */
    private fun agruparPorDia(anotaciones: List<RegistroConCategoria>): List<DiaHistorial> =
        anotaciones
            .groupBy { Formato.fechaDe(it.registro.fechaHora) }
            .map { (fecha, delDia) -> DiaHistorial(fecha, delDia) }

    companion object {
        val Factory = fabricaDe { HistorialViewModel(it.registroRepository, it.categoriaRepository) }
    }
}