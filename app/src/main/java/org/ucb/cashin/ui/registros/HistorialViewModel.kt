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
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.util.Formato
import java.time.LocalDate

/** Una anotación junto con su categoría, lista para mostrarse. */
data class RegistroConCategoria(
    val registro: Registro,
    val categoria: Categoria?,
)

/** Las anotaciones de un mismo día. */
data class DiaHistorial(
    val fecha: LocalDate,
    val registros: List<RegistroConCategoria>,
)

/** Todo lo que la pantalla Historial necesita para dibujarse. */
data class HistorialUiState(
    val dias: List<DiaHistorial> = emptyList(),
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
)

/** Junta cada anotación con su categoría. Si la categoría no aparece, queda vacía. */
fun unirConCategorias(registros: List<Registro>, categorias: List<Categoria>): List<RegistroConCategoria> {
    val categoriaPorId = categorias.associateBy { it.id }
    return registros.map { RegistroConCategoria(it, categoriaPorId[it.categoriaId]) }
}

/**
 * HU-09: todas las anotaciones de la más reciente a la más antigua, agrupadas por día.
 */
class HistorialViewModel(
    private val registroRepository: RegistroRepository,
    private val categoriaRepository: CategoriaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    /**
     * Carga el historial. Se llama cada vez que se abre la pantalla, así siempre
     * muestra lo último. Si ya hay datos, se actualizan sin mostrar la carga otra vez.
     */
    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = it.dias.isEmpty(), error = null) }

            val categorias = categoriaRepository.listarTodas().getOrElse { e ->
                mostrarError(e)
                return@launch
            }
            val registros = registroRepository.listar().getOrElse { e ->
                mostrarError(e)
                return@launch
            }

            _uiState.update {
                it.copy(dias = agruparPorDia(unirConCategorias(registros, categorias)), cargando = false)
            }
        }
    }

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