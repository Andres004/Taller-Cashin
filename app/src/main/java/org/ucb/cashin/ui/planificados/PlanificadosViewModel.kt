package org.ucb.cashin.ui.planificados

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ucb.cashin.data.CategoriaRepository
import org.ucb.cashin.data.PlanificadoRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.Categoria
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.RegistroPlanificado
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.model.comoErrorApp

/** Un pago o ingreso esperado junto con su categoría, listo para mostrarse. */
data class PlanificadoConCategoria(
    val planificado: RegistroPlanificado,
    val categoria: Categoria?,
)

/** Todo lo que la pantalla Pagos e ingresos esperados necesita para dibujarse. */
data class PlanificadosUiState(
    val planificados: List<PlanificadoConCategoria> = emptyList(),
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
    /** Error de "Ya lo pagué" o "Quitar" (la lista sigue visible). */
    val errorAccion: ErrorApp? = null,
    /** Id del planificado que se está procesando, para evitar dos toques seguidos. */
    val procesandoId: String? = null,
    /** Aviso de un pago recién anotado; la pantalla lo muestra una vez y lo limpia. */
    val pagoAnotado: TipoRegistro? = null,
)

/**
 * HU-15: lista de pagos e ingresos esperados.
 * HU-16: "Ya lo pagué / cobré" crea la anotación y mueve la próxima fecha.
 */
class PlanificadosViewModel(
    private val planificadoRepository: PlanificadoRepository,
    private val categoriaRepository: CategoriaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanificadosUiState())
    val uiState: StateFlow<PlanificadosUiState> = _uiState.asStateFlow()

    /**
     * Se llama cada vez que se abre la pantalla (por ejemplo al volver de crear uno).
     * Si ya hay datos, se actualizan sin mostrar la carga otra vez.
     */
    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = it.planificados.isEmpty(), error = null) }

            val categorias = categoriaRepository.listarTodas().getOrElse { e ->
                mostrarError(e)
                return@launch
            }
            planificadoRepository.listar()
                .onSuccess { lista ->
                    val categoriaPorId = categorias.associateBy { it.id }
                    _uiState.update {
                        it.copy(
                            planificados = lista.map { p -> PlanificadoConCategoria(p, categoriaPorId[p.categoriaId]) },
                            cargando = false,
                        )
                    }
                }
                .onFailure { e -> mostrarError(e) }
        }
    }

    /** HU-16: un toque crea la anotación y deja el planificado listo para el siguiente periodo. */
    fun registrarPago(planificado: RegistroPlanificado) {
        if (_uiState.value.procesandoId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoId = planificado.id, errorAccion = null) }
            planificadoRepository.registrarPago(planificado)
                .onSuccess {
                    _uiState.update { it.copy(procesandoId = null, pagoAnotado = planificado.tipo) }
                    cargar()
                }
                .onFailure { e -> _uiState.update { it.copy(procesandoId = null, errorAccion = e.comoErrorApp()) } }
        }
    }

    /** Lo quita de la lista (queda CANCELADO). La pantalla ya pidió confirmación. */
    fun quitar(planificado: RegistroPlanificado) {
        if (_uiState.value.procesandoId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoId = planificado.id, errorAccion = null) }
            planificadoRepository.quitar(planificado.id)
                .onSuccess {
                    _uiState.update { it.copy(procesandoId = null) }
                    cargar()
                }
                .onFailure { e -> _uiState.update { it.copy(procesandoId = null, errorAccion = e.comoErrorApp()) } }
        }
    }

    /** La pantalla ya mostró el aviso del pago. */
    fun avisoMostrado() = _uiState.update { it.copy(pagoAnotado = null) }

    private fun mostrarError(e: Throwable) =
        _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) }

    companion object {
        val Factory = fabricaDe { PlanificadosViewModel(it.planificadoRepository, it.categoriaRepository) }
    }
}