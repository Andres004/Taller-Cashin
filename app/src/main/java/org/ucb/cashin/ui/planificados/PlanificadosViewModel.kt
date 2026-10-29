package org.ucb.cashin.ui.planificados

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ucb.cashin.R
import org.ucb.cashin.data.CategoriaRepository
import org.ucb.cashin.data.PlanificadoRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.Categoria
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.RegistroPlanificado
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.model.sePuedeMarcar
import org.ucb.cashin.util.Formato

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
    /** Aviso que no es un error del servidor, por ejemplo "ese periodo ya estaba anotado". */
    @StringRes val avisoAccion: Int? = null,
    /**
     * Id del planificado que se está procesando. Sigue puesto hasta que la lista
     * se vuelve a cargar, así nadie puede tocar dos veces sobre datos viejos.
     */
    val procesandoId: String? = null,
    /** Aviso de un pago recién anotado; la pantalla lo muestra una vez y lo limpia. */
    val pagoAnotado: TipoRegistro? = null,
)

/**
 * HU-15: lista de pagos e ingresos esperados.
 * HU-16: "Ya lo pagué / cobré" crea la anotación y mueve la próxima fecha (una sola vez por periodo).
 */
class PlanificadosViewModel(
    private val planificadoRepository: PlanificadoRepository,
    private val categoriaRepository: CategoriaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanificadosUiState())
    val uiState: StateFlow<PlanificadosUiState> = _uiState.asStateFlow()

    /** Se llama cada vez que se abre la pantalla (por ejemplo al volver de crear uno). */
    fun cargar() {
        viewModelScope.launch { recargar() }
    }

    /**
     * HU-16: un toque crea la anotación y deja el planificado listo para el siguiente periodo.
     * No hace nada si todavía no toca o si ya hay otro en proceso.
     */
    fun registrarPago(planificado: RegistroPlanificado) {
        val estado = _uiState.value
        if (estado.procesandoId != null) return
        if (!planificado.sePuedeMarcar(Formato.hoy())) return

        viewModelScope.launch {
            _uiState.update { it.copy(procesandoId = planificado.id, errorAccion = null, avisoAccion = null) }
            planificadoRepository.registrarPago(planificado)
                .onSuccess { registro ->
                    if (registro == null) {
                        // Otro toque u otro celular ya marcó este periodo.
                        _uiState.update { it.copy(avisoAccion = R.string.planificados_ya_estaba_anotado) }
                    } else {
                        _uiState.update { it.copy(pagoAnotado = planificado.tipo) }
                    }
                    recargar()
                }
                .onFailure { e -> _uiState.update { it.copy(errorAccion = e.comoErrorApp()) } }
            _uiState.update { it.copy(procesandoId = null) }
        }
    }

    /** Lo quita de la lista (queda CANCELADO). La pantalla ya pidió confirmación. */
    fun quitar(planificado: RegistroPlanificado) {
        if (_uiState.value.procesandoId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoId = planificado.id, errorAccion = null, avisoAccion = null) }
            planificadoRepository.quitar(planificado.id)
                .onSuccess { recargar() }
                .onFailure { e -> _uiState.update { it.copy(errorAccion = e.comoErrorApp()) } }
            _uiState.update { it.copy(procesandoId = null) }
        }
    }

    /** La pantalla ya mostró el aviso del pago. */
    fun avisoMostrado() = _uiState.update { it.copy(pagoAnotado = null) }

    /** Trae categorías y planificados. Si ya hay datos, se actualizan sin mostrar la carga otra vez. */
    private suspend fun recargar() {
        _uiState.update { it.copy(cargando = it.planificados.isEmpty(), error = null) }

        val categorias = categoriaRepository.listarTodas().getOrElse { e ->
            mostrarError(e)
            return
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

    private fun mostrarError(e: Throwable) =
        _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) }

    companion object {
        val Factory = fabricaDe { PlanificadosViewModel(it.planificadoRepository, it.categoriaRepository) }
    }
}