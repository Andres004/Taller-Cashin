package org.ucb.cashin.ui.deudas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ucb.cashin.data.DeudaRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.EstadoDeuda
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.ResumenDeuda
import org.ucb.cashin.model.TipoDeuda
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.prediccion.totalPendiente
import org.ucb.cashin.util.Formato
import java.math.BigDecimal
import java.time.LocalDate

/** Todo lo que la lista de deudas necesita para dibujarse. */
data class DeudasUiState(
    val deudas: List<ResumenDeuda> = emptyList(),
    val tipo: TipoDeuda = TipoDeuda.DEBO,
    val mostrarPagadas: Boolean = false,
    val hoy: LocalDate = Formato.hoy(),
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
) {
    val totalDebo: BigDecimal get() = totalPendiente(deudas, TipoDeuda.DEBO)
    val totalMeDeben: BigDecimal get() = totalPendiente(deudas, TipoDeuda.ME_DEBEN)

    /** Pendientes del tipo elegido: primero las vencidas y las que vencen antes; las sin fecha al final. */
    val pendientes: List<ResumenDeuda>
        get() = deudas
            .filter { it.tipo == tipo && it.estado == EstadoDeuda.PENDIENTE }
            .sortedWith(compareBy<ResumenDeuda> { it.fechaLimite ?: LocalDate.MAX }.thenByDescending { it.pendiente })

    /** Pagadas del tipo elegido, la más reciente primero. */
    val pagadas: List<ResumenDeuda>
        get() = deudas
            .filter { it.tipo == tipo && it.estado == EstadoDeuda.PAGADA }
            .sortedByDescending { it.fechaCancelacion ?: LocalDate.MIN }
}

/** HU-25: lista de deudas en dos pestañas, "Debo" y "Me deben", con lo que falta de cada lado. */
class DeudasViewModel(private val deudaRepository: DeudaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DeudasUiState())
    val uiState: StateFlow<DeudasUiState> = _uiState.asStateFlow()

    /** Se llama cada vez que se abre la pantalla. Si ya hay datos, se actualizan sin mostrar la carga. */
    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = it.deudas.isEmpty(), error = null, hoy = Formato.hoy()) }
            deudaRepository.listar()
                .onSuccess { lista -> _uiState.update { it.copy(deudas = lista, cargando = false) } }
                .onFailure { e -> _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) } }
        }
    }

    fun onTipoCambio(tipo: TipoDeuda) = _uiState.update { it.copy(tipo = tipo, mostrarPagadas = false) }

    fun onPagadasCambio() = _uiState.update { it.copy(mostrarPagadas = !it.mostrarPagadas) }

    companion object {
        val Factory = fabricaDe { DeudasViewModel(it.deudaRepository) }
    }
}
