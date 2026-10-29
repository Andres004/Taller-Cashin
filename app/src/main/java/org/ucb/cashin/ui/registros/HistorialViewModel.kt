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
import java.time.LocalDate
import java.time.YearMonth

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

/** Todo lo que la pantalla Historial necesita para dibujarse. */
data class HistorialUiState(
    val dias: List<DiaHistorial> = emptyList(),
    val filtro: FiltroTipo = FiltroTipo.TODO,
    /** Mes que se está viendo. Null solo mientras no hay ninguna anotación. */
    val mes: YearMonth? = null,
    /** Meses que tienen al menos una anotación, del más reciente al más antiguo. */
    val mesesDisponibles: List<YearMonth> = emptyList(),
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
 * HU-10 (parte): filtro por tipo (Todo, Gastos, Ingresos) y por mes.
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
     */
    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = !it.hayAnotaciones, error = null) }

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

    fun onMesCambio(mes: YearMonth) {
        _uiState.update { it.copy(mes = mes) }
        aplicarFiltros()
    }

    /** Arma los días con las anotaciones que cumplen el filtro de tipo y el mes elegido. */
    private fun aplicarFiltros() {
        _uiState.update { estado ->
            val visibles = todas.filter { anotacion ->
                cumpleTipo(anotacion, estado.filtro) && mesDe(anotacion) == estado.mes
            }
            estado.copy(dias = agruparPorDia(visibles))
        }
    }

    private fun cumpleTipo(anotacion: RegistroConCategoria, filtro: FiltroTipo): Boolean = when (filtro) {
        FiltroTipo.TODO -> true
        FiltroTipo.GASTOS -> anotacion.registro.tipo == TipoRegistro.GASTO
        FiltroTipo.INGRESOS -> anotacion.registro.tipo == TipoRegistro.INGRESO
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