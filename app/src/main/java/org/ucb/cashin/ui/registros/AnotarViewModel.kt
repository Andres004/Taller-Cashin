package org.ucb.cashin.ui.registros

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
import org.ucb.cashin.data.RegistroRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.Categoria
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.MedioPago
import org.ucb.cashin.model.NuevoRegistro
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.util.Formato

/** Todo lo que la pantalla Anotar necesita para dibujarse. */
data class AnotarUiState(
    val tipo: TipoRegistro = TipoRegistro.GASTO,
    val monto: String = "",
    val categorias: List<Categoria> = emptyList(),
    val categoriaId: String? = null,
    val medioPago: MedioPago = MedioPago.EFECTIVO,
    val nota: String = "",
    val cargandoCategorias: Boolean = true,
    val errorCategorias: ErrorApp? = null,
    @StringRes val errorMonto: Int? = null,
    @StringRes val errorCategoria: Int? = null,
    val error: ErrorApp? = null,
    val cargando: Boolean = false,
    val guardado: Boolean = false,
    val ganoXp: Boolean = false,
)

/**
 * HU-06 / HU-07: anotar un gasto o un ingreso en pocos segundos.
 */
class AnotarViewModel(
    private val registroRepository: RegistroRepository,
    private val categoriaRepository: CategoriaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnotarUiState())
    val uiState: StateFlow<AnotarUiState> = _uiState.asStateFlow()

    private var cargaDeCategorias: Job? = null

    init {
        cargarCategorias()
    }

    /** Trae las categorías del tipo elegido. Si el usuario cambia de tipo, se olvida la carga anterior. */
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

    /** Al pasar de Gasto a Ingreso (o al revés) cambian las categorías disponibles. */
    fun onTipoCambio(tipo: TipoRegistro) {
        if (tipo == _uiState.value.tipo) return
        _uiState.update {
            it.copy(tipo = tipo, categorias = emptyList(), categoriaId = null, errorCategoria = null, error = null)
        }
        cargarCategorias()
    }

    fun onMontoCambio(valor: String) =
        _uiState.update { it.copy(monto = valor, errorMonto = null, error = null) }

    fun onCategoriaElegida(categoria: Categoria) =
        _uiState.update { it.copy(categoriaId = categoria.id, errorCategoria = null, error = null) }

    fun onMedioPagoCambio(medioPago: MedioPago) =
        _uiState.update { it.copy(medioPago = medioPago) }

    /** La nota no puede pasar de 200 caracteres, igual que en la base de datos. */
    fun onNotaCambio(valor: String) =
        _uiState.update { it.copy(nota = valor.take(MAX_NOTA)) }

    /** Revisa el formulario y, si está bien, guarda la anotación. */
    fun guardar() {
        val estado = _uiState.value
        if (estado.cargando) return

        val monto = Formato.parsearMonto(estado.monto)?.takeIf { it.signum() > 0 }
        val categoriaId = estado.categoriaId
        if (monto == null || categoriaId == null) {
            _uiState.update {
                it.copy(
                    errorMonto = if (monto == null) R.string.validacion_monto else null,
                    errorCategoria = if (categoriaId == null) R.string.validacion_categoria else null,
                )
            }
            return
        }

        val nuevo = NuevoRegistro(
            categoriaId = categoriaId,
            tipo = estado.tipo,
            monto = monto,
            medioPago = estado.medioPago,
            nota = estado.nota.trim().ifBlank { null },
        )

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            registroRepository.crear(nuevo)
                .onSuccess { _ ->
                    val ganoXp = ganoXpConEsteRegistro()
                    _uiState.update { it.copy(cargando = false, guardado = true, ganoXp = ganoXp) }
                }
                .onFailure { e -> _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) } }
        }
    }

    /** Deja el formulario vacío para la siguiente anotación, manteniendo el tipo y sus categorías. */
    fun limpiarFormulario() =
        _uiState.update {
            AnotarUiState(
                tipo = it.tipo,
                categorias = it.categorias,
                cargandoCategorias = it.cargandoCategorias,
                errorCategorias = it.errorCategorias,
            )
        }

    /** La base de datos solo da XP a las 5 primeras anotaciones del día. */
    private suspend fun ganoXpConEsteRegistro(): Boolean =
        registroRepository.contarCreadosHoy()
            .map { cantidad -> cantidad <= MAX_REGISTROS_CON_XP }
            .getOrDefault(false)

    companion object {
        const val MAX_NOTA = 200
        private const val MAX_REGISTROS_CON_XP = 5

        val Factory = fabricaDe { AnotarViewModel(it.registroRepository, it.categoriaRepository) }
    }
}