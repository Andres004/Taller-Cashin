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
import java.time.LocalDate

/** Todo lo que el formulario de una meta nueva necesita para dibujarse. */
data class NuevaMetaUiState(
    val nombre: String = "",
    val montoObjetivo: String = "",
    /** Opcional: una meta puede no tener fecha límite. */
    val fechaLimite: LocalDate? = null,
    val descripcion: String = "",
    @StringRes val errorNombre: Int? = null,
    @StringRes val errorMonto: Int? = null,
    @StringRes val errorFecha: Int? = null,
    val error: ErrorApp? = null,
    val guardando: Boolean = false,
    /** Id de la meta creada; la pantalla la usa para abrir su detalle. */
    val metaCreadaId: String? = null,
)

/** HU-17: crear una meta de ahorro (nombre, monto objetivo y fecha límite opcional). */
class NuevaMetaViewModel(
    private val metaRepository: MetaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NuevaMetaUiState())
    val uiState: StateFlow<NuevaMetaUiState> = _uiState.asStateFlow()

    fun onNombreCambio(valor: String) =
        _uiState.update { it.copy(nombre = valor.take(MAX_NOMBRE), errorNombre = null, error = null) }

    fun onMontoCambio(valor: String) =
        _uiState.update { it.copy(montoObjetivo = valor, errorMonto = null, error = null) }

    /** Null = sin fecha límite. */
    fun onFechaCambio(fecha: LocalDate?) =
        _uiState.update { it.copy(fechaLimite = fecha, errorFecha = null, error = null) }

    fun onDescripcionCambio(valor: String) =
        _uiState.update { it.copy(descripcion = valor.take(MAX_DESCRIPCION)) }

    /** Nombre y monto mayor a 0 son obligatorios; si hay fecha límite, tiene que ser después de hoy. */
    fun guardar() {
        val estado = _uiState.value
        if (estado.guardando) return

        val nombre = estado.nombre.trim()
        val monto = Formato.parsearMonto(estado.montoObjetivo)?.takeIf { it.signum() > 0 }
        val fecha = estado.fechaLimite
        val fechaInvalida = fecha != null && !fecha.isAfter(Formato.hoy())

        if (nombre.isEmpty() || monto == null || fechaInvalida) {
            _uiState.update {
                it.copy(
                    errorNombre = if (nombre.isEmpty()) R.string.metas_error_nombre else null,
                    errorMonto = if (monto == null) R.string.validacion_monto else null,
                    errorFecha = if (fechaInvalida) R.string.metas_error_fecha else null,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, error = null) }
            metaRepository.crear(
                NuevaMeta(
                    nombre = nombre,
                    descripcion = estado.descripcion.trim().ifBlank { null },
                    montoObjetivo = monto,
                    fechaLimite = fecha,
                ),
            )
                .onSuccess { meta -> _uiState.update { it.copy(guardando = false, metaCreadaId = meta.id) } }
                .onFailure { e -> _uiState.update { it.copy(guardando = false, error = e.comoErrorApp()) } }
        }
    }

    companion object {
        /** Igual que en la tabla meta: nombre varchar(60) y descripcion varchar(200). */
        const val MAX_NOMBRE = 60
        const val MAX_DESCRIPCION = 200

        val Factory = fabricaDe { NuevaMetaViewModel(it.metaRepository) }
    }
}