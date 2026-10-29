package org.ucb.cashin.ui.planificados

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
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
import org.ucb.cashin.model.Frecuencia
import org.ucb.cashin.model.MedioPago
import org.ucb.cashin.model.NuevoPlanificado
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.util.Formato
import java.time.LocalDate

/** Todo lo que el formulario de un pago o ingreso esperado necesita para dibujarse. */
data class NuevoPlanificadoUiState(
    val tipo: TipoRegistro = TipoRegistro.GASTO,
    val nombre: String = "",
    val monto: String = "",
    val categorias: List<Categoria> = emptyList(),
    val categoriaId: String? = null,
    /** Por defecto Efectivo, como dice la base de datos. */
    val medioPago: MedioPago = MedioPago.EFECTIVO,
    val frecuencia: Frecuencia = Frecuencia.MENSUAL,
    val proximaFecha: LocalDate = Formato.hoy(),
    val cargandoCategorias: Boolean = true,
    val errorCategorias: ErrorApp? = null,
    @StringRes val errorNombre: Int? = null,
    @StringRes val errorMonto: Int? = null,
    @StringRes val errorCategoria: Int? = null,
    val error: ErrorApp? = null,
    val guardando: Boolean = false,
    val guardado: Boolean = false,
)

/** HU-15: anotar un pago o ingreso esperado (nombre, monto, frecuencia y próxima fecha). */
class NuevoPlanificadoViewModel(
    private val planificadoRepository: PlanificadoRepository,
    private val categoriaRepository: CategoriaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NuevoPlanificadoUiState())
    val uiState: StateFlow<NuevoPlanificadoUiState> = _uiState.asStateFlow()

    private var cargaDeCategorias: Job? = null

    init {
        cargarCategorias()
    }

    /** Categorías activas del tipo elegido (las archivadas no se ofrecen). */
    fun cargarCategorias() {
        val tipo = _uiState.value.tipo
        cargaDeCategorias?.cancel()
        cargaDeCategorias = viewModelScope.launch {
            _uiState.update { it.copy(cargandoCategorias = true, errorCategorias = null) }
            categoriaRepository.listar(tipo)
                .onSuccess { lista -> _uiState.update { it.copy(categorias = lista, cargandoCategorias = false) } }
                .onFailure { e -> _uiState.update { it.copy(cargandoCategorias = false, errorCategorias = e.comoErrorApp()) } }
        }
    }

    /** Al cambiar de tipo, la categoría elegida ya no sirve. */
    fun onTipoCambio(tipo: TipoRegistro) {
        if (tipo == _uiState.value.tipo) return
        _uiState.update { it.copy(tipo = tipo, categorias = emptyList(), categoriaId = null, errorCategoria = null) }
        cargarCategorias()
    }

    fun onNombreCambio(valor: String) =
        _uiState.update { it.copy(nombre = valor.take(MAX_NOMBRE), errorNombre = null, error = null) }

    fun onMontoCambio(valor: String) =
        _uiState.update { it.copy(monto = valor, errorMonto = null, error = null) }

    fun onCategoriaElegida(categoria: Categoria) =
        _uiState.update { it.copy(categoriaId = categoria.id, errorCategoria = null, error = null) }

    fun onMedioPagoCambio(medioPago: MedioPago) = _uiState.update { it.copy(medioPago = medioPago) }

    fun onFrecuenciaCambio(frecuencia: Frecuencia) = _uiState.update { it.copy(frecuencia = frecuencia) }

    fun onFechaCambio(fecha: LocalDate) = _uiState.update { it.copy(proximaFecha = fecha) }

    /** Nombre, monto mayor a 0 y categoría son obligatorios. Si falta algo, se marca todo a la vez. */
    fun guardar() {
        val estado = _uiState.value
        if (estado.guardando) return

        val nombre = estado.nombre.trim()
        val monto = Formato.parsearMonto(estado.monto)?.takeIf { it.signum() > 0 }
        val categoriaId = estado.categoriaId
        if (nombre.isEmpty() || monto == null || categoriaId == null) {
            _uiState.update {
                it.copy(
                    errorNombre = if (nombre.isEmpty()) R.string.planificados_error_nombre else null,
                    errorMonto = if (monto == null) R.string.validacion_monto else null,
                    errorCategoria = if (categoriaId == null) R.string.validacion_categoria else null,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, error = null) }
            planificadoRepository.crear(
                NuevoPlanificado(
                    categoriaId = categoriaId,
                    tipo = estado.tipo,
                    nombre = nombre,
                    monto = monto,
                    medioPago = estado.medioPago,
                    frecuencia = estado.frecuencia,
                    proximaFecha = estado.proximaFecha,
                ),
            )
                .onSuccess { _uiState.update { it.copy(guardando = false, guardado = true) } }
                .onFailure { e -> _uiState.update { it.copy(guardando = false, error = e.comoErrorApp()) } }
        }
    }

    companion object {
        /** Igual que varchar(60) de la columna registro_planificado.nombre. */
        const val MAX_NOMBRE = 60

        val Factory = fabricaDe { NuevoPlanificadoViewModel(it.planificadoRepository, it.categoriaRepository) }
    }
}