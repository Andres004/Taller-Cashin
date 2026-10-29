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

/**
 * Todo lo que el formulario de un pago o ingreso esperado necesita para dibujarse.
 * Tipo, medio de pago, frecuencia y fecha empiezan vacíos a propósito:
 * el usuario tiene que elegir cada uno (nada se guarda "por defecto" sin que lo vea).
 */
data class NuevoPlanificadoUiState(
    val tipo: TipoRegistro? = null,
    val nombre: String = "",
    val monto: String = "",
    val categorias: List<Categoria> = emptyList(),
    val categoriaId: String? = null,
    val medioPago: MedioPago? = null,
    val frecuencia: Frecuencia? = null,
    val proximaFecha: LocalDate? = null,
    val cargandoCategorias: Boolean = false,
    val errorCategorias: ErrorApp? = null,
    @StringRes val errorTipo: Int? = null,
    @StringRes val errorNombre: Int? = null,
    @StringRes val errorMonto: Int? = null,
    @StringRes val errorCategoria: Int? = null,
    @StringRes val errorMedioPago: Int? = null,
    @StringRes val errorFrecuencia: Int? = null,
    @StringRes val errorFecha: Int? = null,
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

    /** Categorías activas del tipo elegido (las archivadas no se ofrecen). Sin tipo no hay nada que cargar. */
    fun cargarCategorias() {
        val tipo = _uiState.value.tipo ?: return
        cargaDeCategorias?.cancel()
        cargaDeCategorias = viewModelScope.launch {
            _uiState.update { it.copy(cargandoCategorias = true, errorCategorias = null) }
            categoriaRepository.listar(tipo)
                .onSuccess { lista -> _uiState.update { it.copy(categorias = lista, cargandoCategorias = false) } }
                .onFailure { e -> _uiState.update { it.copy(cargandoCategorias = false, errorCategorias = e.comoErrorApp()) } }
        }
    }

    /** Al cambiar de tipo, la categoría elegida ya no sirve y se cargan las del nuevo tipo. */
    fun onTipoCambio(tipo: TipoRegistro) {
        if (tipo == _uiState.value.tipo) return
        _uiState.update {
            it.copy(
                tipo = tipo,
                errorTipo = null,
                categorias = emptyList(),
                categoriaId = null,
                errorCategoria = null,
                error = null,
            )
        }
        cargarCategorias()
    }

    fun onNombreCambio(valor: String) =
        _uiState.update { it.copy(nombre = valor.take(MAX_NOMBRE), errorNombre = null, error = null) }

    fun onMontoCambio(valor: String) =
        _uiState.update { it.copy(monto = valor, errorMonto = null, error = null) }

    fun onCategoriaElegida(categoria: Categoria) =
        _uiState.update { it.copy(categoriaId = categoria.id, errorCategoria = null, error = null) }

    fun onMedioPagoCambio(medioPago: MedioPago) =
        _uiState.update { it.copy(medioPago = medioPago, errorMedioPago = null, error = null) }

    fun onFrecuenciaCambio(frecuencia: Frecuencia) =
        _uiState.update { it.copy(frecuencia = frecuencia, errorFrecuencia = null, error = null) }

    fun onFechaCambio(fecha: LocalDate) =
        _uiState.update { it.copy(proximaFecha = fecha, errorFecha = null, error = null) }

    /**
     * Todo es obligatorio: tipo, nombre, monto mayor a 0, categoría, medio de pago,
     * frecuencia y una fecha desde hoy en adelante. Si falta algo, se marca todo a la vez.
     */
    fun guardar() {
        val estado = _uiState.value
        if (estado.guardando) return

        val tipo = estado.tipo
        val nombre = estado.nombre.trim()
        val monto = Formato.parsearMonto(estado.monto)?.takeIf { it.signum() > 0 }
        val categoriaId = estado.categoriaId
        val medioPago = estado.medioPago
        val frecuencia = estado.frecuencia
        val fecha = estado.proximaFecha
        val fechaPasada = fecha != null && fecha.isBefore(Formato.hoy())

        if (tipo == null || nombre.isEmpty() || monto == null || categoriaId == null ||
            medioPago == null || frecuencia == null || fecha == null || fechaPasada
        ) {
            _uiState.update {
                it.copy(
                    errorTipo = if (tipo == null) R.string.validacion_tipo else null,
                    errorNombre = if (nombre.isEmpty()) R.string.planificados_error_nombre else null,
                    errorMonto = if (monto == null) R.string.validacion_monto else null,
                    // Sin tipo no se muestran categorías: basta con el error del tipo.
                    errorCategoria = if (tipo != null && categoriaId == null) R.string.validacion_categoria else null,
                    errorMedioPago = if (medioPago == null) R.string.validacion_medio_pago else null,
                    errorFrecuencia = if (frecuencia == null) R.string.validacion_frecuencia else null,
                    errorFecha = when {
                        fecha == null -> R.string.validacion_fecha
                        fechaPasada -> R.string.planificados_error_fecha_pasada
                        else -> null
                    },
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, error = null) }
            planificadoRepository.crear(
                NuevoPlanificado(
                    categoriaId = categoriaId,
                    tipo = tipo,
                    nombre = nombre,
                    monto = monto,
                    medioPago = medioPago,
                    frecuencia = frecuencia,
                    proximaFecha = fecha,
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