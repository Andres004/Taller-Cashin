package org.ucb.cashin.ui.metas

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ucb.cashin.R
import org.ucb.cashin.data.MetaRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.NuevaMeta
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.util.Formato
import java.math.BigDecimal
import java.time.LocalDate

/** Todo lo que el formulario de una meta (nueva o editada) necesita para dibujarse. */
data class NuevaMetaUiState(
    val esEdicion: Boolean = false,
    val nombre: String = "",
    val montoObjetivo: String = "",
    /** Lo único opcional: una meta puede no tener fecha límite. */
    val fechaLimite: LocalDate? = null,
    val descripcion: String = "",
    /** En edición: mientras se trae la meta, o si falló. */
    val cargandoMeta: Boolean = false,
    val errorMeta: ErrorApp? = null,
    @StringRes val errorNombre: Int? = null,
    @StringRes val errorMonto: Int? = null,
    @StringRes val errorFecha: Int? = null,
    @StringRes val errorDescripcion: Int? = null,
    val error: ErrorApp? = null,
    val guardando: Boolean = false,
    /** Id de la meta guardada; la pantalla avisa a quien la abrió. */
    val metaGuardadaId: String? = null,
) {
    /** Hay al menos un campo marcado en rojo (para el aviso junto al botón). */
    val hayErrores: Boolean
        get() = errorNombre != null || errorMonto != null || errorFecha != null || errorDescripcion != null
}

/**
 * HU-17: crear una meta de ahorro.
 * Con un [metaId], el mismo formulario sirve para editarla.
 * Obligatorios: nombre, monto objetivo y descripción. Opcional: fecha límite.
 */
class NuevaMetaViewModel(
    private val metaRepository: MetaRepository,
    private val metaId: String? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        NuevaMetaUiState(esEdicion = metaId != null, cargandoMeta = metaId != null),
    )
    val uiState: StateFlow<NuevaMetaUiState> = _uiState.asStateFlow()

    /** En edición: la fecha límite que ya tenía (se puede mantener aunque ya haya pasado). */
    private var fechaOriginal: LocalDate? = null

    init {
        if (metaId != null) cargarMeta()
    }

    /** Edición: trae la meta y llena el formulario con sus datos. */
    fun cargarMeta() {
        val id = metaId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(cargandoMeta = true, errorMeta = null) }
            metaRepository.obtener(id)
                .onSuccess { meta ->
                    fechaOriginal = meta.fechaLimite
                    _uiState.update {
                        it.copy(
                            cargandoMeta = false,
                            nombre = meta.nombre,
                            montoObjetivo = montoParaEditar(meta.montoObjetivo),
                            fechaLimite = meta.fechaLimite,
                            descripcion = meta.descripcion.orEmpty(),
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(cargandoMeta = false, errorMeta = e.comoErrorApp()) } }
        }
    }

    fun onNombreCambio(valor: String) =
        _uiState.update { it.copy(nombre = valor.take(MAX_NOMBRE), errorNombre = null, error = null) }

    fun onMontoCambio(valor: String) =
        _uiState.update { it.copy(montoObjetivo = valor, errorMonto = null, error = null) }

    /** Null = sin fecha límite. */
    fun onFechaCambio(fecha: LocalDate?) =
        _uiState.update { it.copy(fechaLimite = fecha, errorFecha = null, error = null) }

    fun onDescripcionCambio(valor: String) =
        _uiState.update { it.copy(descripcion = valor.take(MAX_DESCRIPCION), errorDescripcion = null, error = null) }

    /**
     * Revisa todo a la vez y marca cada campo que falta.
     * La fecha es opcional; si se elige una nueva, tiene que ser después de hoy.
     */
    fun guardar() {
        val estado = _uiState.value
        if (estado.guardando || estado.cargandoMeta) return

        val nombre = estado.nombre.trim()
        val monto = Formato.parsearMonto(estado.montoObjetivo)?.takeIf { it.signum() > 0 }
        val descripcion = estado.descripcion.trim()
        val fecha = estado.fechaLimite
        val fechaInvalida = fecha != null && fecha != fechaOriginal && !fecha.isAfter(Formato.hoy())

        if (nombre.isEmpty() || monto == null || descripcion.isEmpty() || fechaInvalida) {
            _uiState.update {
                it.copy(
                    errorNombre = if (nombre.isEmpty()) R.string.metas_error_nombre else null,
                    errorMonto = if (monto == null) R.string.validacion_monto else null,
                    errorDescripcion = if (descripcion.isEmpty()) R.string.metas_error_descripcion else null,
                    errorFecha = if (fechaInvalida) R.string.metas_error_fecha else null,
                )
            }
            return
        }

        val datos = NuevaMeta(
            nombre = nombre,
            descripcion = descripcion,
            montoObjetivo = monto,
            fechaLimite = fecha,
        )

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, error = null) }
            val resultado = if (metaId == null) metaRepository.crear(datos) else metaRepository.actualizar(metaId, datos)
            resultado
                .onSuccess { meta -> _uiState.update { it.copy(guardando = false, metaGuardadaId = meta.id) } }
                .onFailure { e -> _uiState.update { it.copy(guardando = false, error = e.comoErrorApp()) } }
        }
    }

    /** 2500.00 → "2500": el monto como se escribe en el campo, con coma como en el resto de la app. */
    private fun montoParaEditar(monto: BigDecimal): String =
        monto.stripTrailingZeros().toPlainString().replace('.', ',')

    companion object {
        /** Igual que en la tabla meta: nombre varchar(60) y descripcion varchar(200). */
        const val MAX_NOMBRE = 60
        const val MAX_DESCRIPCION = 200

        /** Sin id: meta nueva. Con id: editar esa meta. */
        fun fabrica(metaId: String? = null) = fabricaDe { NuevaMetaViewModel(it.metaRepository, metaId) }
    }
}