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
import org.ucb.cashin.model.Registro
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.util.Formato
import java.math.BigDecimal

/** Todo lo que la pantalla Anotar (o Editar) necesita para dibujarse. */
data class AnotarUiState(
    val esEdicion: Boolean = false,
    val tipo: TipoRegistro = TipoRegistro.GASTO,
    val monto: String = "",
    val categorias: List<Categoria> = emptyList(),
    val categoriaId: String? = null,
    val medioPago: MedioPago = MedioPago.EFECTIVO,
    val nota: String = "",
    val cargandoRegistro: Boolean = false,
    val errorRegistro: ErrorApp? = null,
    val cargandoCategorias: Boolean = true,
    val errorCategorias: ErrorApp? = null,
    @StringRes val errorMonto: Int? = null,
    @StringRes val errorCategoria: Int? = null,
    val error: ErrorApp? = null,
    val cargando: Boolean = false,
    val eliminando: Boolean = false,
    val guardado: Boolean = false,
    val eliminado: Boolean = false,
    val ganoXp: Boolean = false,
)

/**
 * HU-06 / HU-07: anotar un gasto o un ingreso en pocos segundos.
 * HU-08: con un [registroId], el mismo formulario sirve para corregir o borrar esa anotación.
 */
class AnotarViewModel(
    private val registroRepository: RegistroRepository,
    private val categoriaRepository: CategoriaRepository,
    private val registroId: String? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AnotarUiState(esEdicion = registroId != null, cargandoRegistro = registroId != null),
    )
    val uiState: StateFlow<AnotarUiState> = _uiState.asStateFlow()

    private var cargaDeCategorias: Job? = null

    /** En modo edición: la anotación tal como estaba antes de cambiarla. */
    private var original: Registro? = null

    init {
        if (registroId != null) cargarRegistro() else cargarCategorias()
    }

    /** Modo edición: trae la anotación y llena el formulario con sus datos. */
    fun cargarRegistro() {
        val id = registroId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(cargandoRegistro = true, errorRegistro = null) }
            registroRepository.obtener(id)
                .onSuccess { registro ->
                    original = registro
                    _uiState.update {
                        it.copy(
                            cargandoRegistro = false,
                            tipo = registro.tipo,
                            monto = montoParaEditar(registro.monto),
                            categoriaId = registro.categoriaId,
                            medioPago = registro.medioPago,
                            nota = registro.nota.orEmpty(),
                        )
                    }
                    cargarCategorias()
                }
                .onFailure { e -> _uiState.update { it.copy(cargandoRegistro = false, errorRegistro = e.comoErrorApp()) } }
        }
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

    /** Revisa el formulario y, si está bien, guarda la anotación nueva o los cambios. */
    fun guardar() {
        val estado = _uiState.value
        if (estado.cargando || estado.eliminando) return

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

        val anterior = original
        if (estado.esEdicion && anterior == null) return

        val datos = NuevoRegistro(
            categoriaId = categoriaId,
            tipo = estado.tipo,
            monto = monto,
            medioPago = estado.medioPago,
            nota = estado.nota.trim().ifBlank { null },
        )

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            val resultado = if (anterior == null) {
                registroRepository.crear(datos)
            } else {
                // Al corregir se mantiene la fecha original: la anotación no se mueve a "ahora".
                registroRepository.actualizar(
                    id = anterior.id,
                    cambios = datos.copy(fechaHora = anterior.fechaHora, planificadoId = anterior.planificadoId),
                )
            }
            resultado
                .onSuccess { _ ->
                    val ganoXp = anterior == null && ganoXpConEsteRegistro()
                    _uiState.update { it.copy(cargando = false, guardado = true, ganoXp = ganoXp) }
                }
                .onFailure { e -> _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) } }
        }
    }

    /** Modo edición: borra la anotación (la pantalla ya pidió confirmación). */
    fun eliminar() {
        val id = registroId ?: return
        if (_uiState.value.eliminando || _uiState.value.cargando) return
        viewModelScope.launch {
            _uiState.update { it.copy(eliminando = true, error = null) }
            registroRepository.eliminar(id)
                .onSuccess { _ -> _uiState.update { it.copy(eliminando = false, eliminado = true) } }
                .onFailure { e -> _uiState.update { it.copy(eliminando = false, error = e.comoErrorApp()) } }
        }
    }

    /** Deja el formulario vacío para la siguiente anotación, manteniendo el tipo y sus categorías. */
    fun limpiarFormulario() =
        _uiState.update {
            AnotarUiState(
                esEdicion = it.esEdicion,
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

    /** 20.50 → "20,5": el monto como se escribe en el campo, con coma como en el resto de la app. */
    private fun montoParaEditar(monto: BigDecimal): String =
        monto.stripTrailingZeros().toPlainString().replace('.', ',')

    companion object {
        const val MAX_NOTA = 200
        private const val MAX_REGISTROS_CON_XP = 5

        /** Sin id: anotar algo nuevo. Con id: editar esa anotación. */
        fun fabrica(registroId: String? = null) =
            fabricaDe { AnotarViewModel(it.registroRepository, it.categoriaRepository, registroId) }
    }
}