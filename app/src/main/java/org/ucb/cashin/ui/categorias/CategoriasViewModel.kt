package org.ucb.cashin.ui.categorias

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
import org.ucb.cashin.data.AuthRepository
import org.ucb.cashin.data.CategoriaRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.Categoria
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.NuevaCategoria
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.ui.theme.ColoresCategoria

/** Todo lo que la pantalla Mis categorías necesita para dibujarse. */
data class CategoriasUiState(
    val tipo: TipoRegistro = TipoRegistro.GASTO,
    /** Ya ordenadas: del sistema, propias y, al final, archivadas. */
    val categorias: List<Categoria> = emptyList(),
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
    /** Error de archivar/desarchivar (la lista sigue visible). */
    val errorAccion: ErrorApp? = null,
    val dialogoAbierto: Boolean = false,
    val nombre: String = "",
    val icono: String = ICONOS.first(),
    val color: String = ColoresCategoria.first(),
    @StringRes val errorNombre: Int? = null,
    val errorGuardar: ErrorApp? = null,
    val guardando: Boolean = false,
)

/** Nombres de ícono que entiende `iconoCategoria()`. */
val ICONOS = listOf("restaurant", "bus", "home", "health", "school", "movie", "work", "store", "gift", "more")

private const val MAX_NOMBRE = 50

/** HU-11 / HU-12: crear categorías propias y archivarlas sin perder el historial. */
class CategoriasViewModel(
    private val categoriaRepository: CategoriaRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriasUiState())
    val uiState: StateFlow<CategoriasUiState> = _uiState.asStateFlow()

    private var carga: Job? = null

    init {
        cargar()
    }

    fun cargar() {
        val tipo = _uiState.value.tipo
        carga?.cancel()
        carga = viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            categoriaRepository.listarConArchivadas(tipo)
                .onSuccess { lista -> _uiState.update { it.copy(categorias = ordenar(lista), cargando = false) } }
                .onFailure { e -> _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) } }
        }
    }

    fun onTipoCambio(tipo: TipoRegistro) {
        if (tipo == _uiState.value.tipo) return
        _uiState.update { it.copy(tipo = tipo, categorias = emptyList(), errorAccion = null) }
        cargar()
    }

    fun abrirDialogo() = _uiState.update {
        it.copy(
            dialogoAbierto = true,
            nombre = "",
            icono = ICONOS.first(),
            color = ColoresCategoria.first(),
            errorNombre = null,
            errorGuardar = null,
        )
    }

    fun cerrarDialogo() {
        if (_uiState.value.guardando) return
        _uiState.update { it.copy(dialogoAbierto = false) }
    }

    fun onNombreCambio(valor: String) =
        _uiState.update { it.copy(nombre = valor.take(MAX_NOMBRE), errorNombre = null, errorGuardar = null) }

    fun onIconoCambio(valor: String) = _uiState.update { it.copy(icono = valor) }

    fun onColorCambio(valor: String) = _uiState.update { it.copy(color = valor) }

    fun crear() {
        val estado = _uiState.value
        if (estado.guardando) return
        val nombre = estado.nombre.trim()
        if (nombre.isEmpty() || nombre.length > MAX_NOMBRE) {
            _uiState.update { it.copy(errorNombre = R.string.categorias_error_nombre) }
            return
        }
        val usuarioId = authRepository.usuarioIdActual()
        if (usuarioId == null) {
            _uiState.update { it.copy(errorGuardar = ErrorApp.SIN_PERMISO) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, errorGuardar = null, errorNombre = null) }
            val nueva = NuevaCategoria(
                usuarioId = usuarioId,
                nombre = nombre,
                tipo = estado.tipo,
                icono = estado.icono,
                color = estado.color,
            )
            categoriaRepository.crear(nueva)
                .onSuccess {
                    _uiState.update { it.copy(guardando = false, dialogoAbierto = false) }
                    cargar()
                }
                .onFailure { e ->
                    _uiState.update {
                        if (e.esNombreRepetido()) {
                            it.copy(guardando = false, errorNombre = R.string.categorias_error_repetida)
                        } else {
                            it.copy(guardando = false, errorGuardar = e.comoErrorApp())
                        }
                    }
                }
        }
    }

    fun archivar(categoria: Categoria, archivada: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorAccion = null) }
            categoriaRepository.archivar(categoria.id, archivada)
                .onSuccess { cargar() }
                .onFailure { e -> _uiState.update { it.copy(errorAccion = e.comoErrorApp()) } }
        }
    }

    fun descartarErrorAccion() = _uiState.update { it.copy(errorAccion = null) }

    /** Activas primero (del sistema y luego propias); las archivadas van al final. */
    private fun ordenar(lista: List<Categoria>): List<Categoria> =
        lista.sortedWith(compareBy({ it.archivada }, { !it.esDelSistema }, { it.nombre.lowercase() }))

    /** Postgres avisa la unicidad con "duplicate key value violates unique constraint" (código 23505). */
    private fun Throwable.esNombreRepetido(): Boolean =
        generateSequence(this) { it.cause }
            .mapNotNull { it.message?.lowercase() }
            .any { "duplicate key" in it || "23505" in it || "unique constraint" in it }

    companion object {
        val Factory = fabricaDe { CategoriasViewModel(it.categoriaRepository, it.authRepository) }
    }
}
