package org.ucb.cashin.ui.deudas

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ucb.cashin.data.DeudaRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.CambiosDeuda
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.NuevaDeuda
import org.ucb.cashin.model.ResumenDeuda
import org.ucb.cashin.model.TipoDeuda
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.prediccion.MovimientoDeuda
import org.ucb.cashin.prediccion.ValidacionEdicion
import org.ucb.cashin.prediccion.deudaAbiertaCon
import org.ucb.cashin.prediccion.montoValido
import org.ucb.cashin.prediccion.movimientosDe
import org.ucb.cashin.prediccion.validarEdicion
import org.ucb.cashin.ui.components.mensajeRes
import org.ucb.cashin.util.Formato
import java.math.BigDecimal
import java.time.LocalDate

/** Todo lo que el formulario de una deuda (nueva o editada) necesita para dibujarse. */
data class FormularioDeudaUiState(
    val editandoId: String? = null,
    val tipo: TipoDeuda = TipoDeuda.DEBO,
    val contraparte: String = "",
    val montoTexto: String = "",
    val descripcion: String = "",
    val fechaInicio: LocalDate = Formato.hoy(),
    val fechaLimite: LocalDate? = null,
    val hoy: LocalDate = Formato.hoy(),
    /** Al editar: aumentos y abonos que ya tiene (para revisar el monto inicial y la fecha). */
    val movimientos: List<MovimientoDeuda> = emptyList(),
    val tieneMovimientos: Boolean = false,
    /** Todas las deudas, para preguntar "¿Sumar a la de Amy?". */
    val deudas: List<ResumenDeuda> = emptyList(),
    /** Tocó "Crear otra": no se vuelve a preguntar hasta que cambie el nombre. */
    val ignorarSugerencia: Boolean = false,
    /** Los errores se muestran recién después de intentar guardar. */
    val intento: Boolean = false,
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val error: ErrorApp? = null,
    @StringRes val errorGuardar: Int? = null,
    /** Id de la deuda guardada: la pantalla navega al detalle. */
    val guardadaId: String? = null,
) {
    val editando: Boolean get() = editandoId != null
    val monto: BigDecimal? get() = Formato.parsearMonto(montoTexto)

    /** Deuda abierta con la misma persona y del mismo tipo (solo al crear). */
    val sugerencia: ResumenDeuda?
        get() = if (editando || ignorarSugerencia) null else deudaAbiertaCon(contraparte, tipo, deudas)

    val errorNombre: Boolean get() = contraparte.isBlank()
    val errorMonto: Boolean get() = !montoValido(monto)
    val errorInicioFuturo: Boolean get() = fechaInicio.isAfter(hoy)
    val errorLimite: Boolean get() = fechaLimite != null && fechaLimite.isBefore(fechaInicio)

    /** Al editar, el monto inicial y la fecha de inicio tienen que cuadrar con los movimientos. */
    val validacionEdicion: ValidacionEdicion?
        get() = if (!editando || errorMonto) null else validarEdicion(movimientos, monto, fechaInicio, hoy)

    val valido: Boolean
        get() = !errorNombre && !errorMonto && !errorInicioFuturo && !errorLimite && validacionEdicion?.problema == null
}

/**
 * HU-25: crear o editar una deuda. Al crear, si ya hay una deuda abierta con la misma persona,
 * se ofrece sumar el préstamo a esa deuda (la pantalla lleva al detalle con "Prestar más" abierto).
 */
class FormularioDeudaViewModel(private val deudaRepository: DeudaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(FormularioDeudaUiState())
    val uiState: StateFlow<FormularioDeudaUiState> = _uiState.asStateFlow()
    private var iniciado = false

    /** [deudaId] null = nueva deuda del [tipo] de la pestaña desde donde se abrió. */
    fun iniciar(deudaId: String?, tipo: TipoDeuda) {
        if (iniciado) return
        iniciado = true
        _uiState.update { it.copy(editandoId = deudaId, tipo = tipo, cargando = deudaId != null) }
        viewModelScope.launch {
            val deudas = deudaRepository.listar().getOrDefault(emptyList())
            _uiState.update { it.copy(deudas = deudas) }
            if (deudaId == null) return@launch

            val deuda = deudaRepository.obtener(deudaId).getOrElse { e ->
                _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) }
                return@launch
            }
            val guardados = deudaRepository.movimientos(deudaId).getOrElse { e ->
                _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) }
                return@launch
            }
            val inicial = deuda.montoInicial ?: deuda.montoTotal
            val inicio = deuda.fechaInicio ?: Formato.hoy()
            _uiState.update {
                it.copy(
                    tipo = deuda.tipo,
                    contraparte = deuda.contraparte,
                    montoTexto = inicial.toPlainString().replace('.', ','),
                    descripcion = deuda.descripcion.orEmpty(),
                    fechaInicio = inicio,
                    fechaLimite = deuda.fechaLimite,
                    movimientos = movimientosDe(inicial, inicio, null, guardados.aumentos, guardados.abonos),
                    tieneMovimientos = guardados.aumentos.isNotEmpty() || guardados.abonos.isNotEmpty(),
                    cargando = false,
                )
            }
        }
    }

    fun onTipo(tipo: TipoDeuda) = _uiState.update { if (it.tieneMovimientos) it else it.copy(tipo = tipo, ignorarSugerencia = false) }

    fun onContraparte(texto: String) = _uiState.update { it.copy(contraparte = texto.take(80), ignorarSugerencia = false) }

    fun onMonto(texto: String) = _uiState.update { it.copy(montoTexto = texto) }

    fun onDescripcion(texto: String) = _uiState.update { it.copy(descripcion = texto.take(200)) }

    fun onFechaInicio(fecha: LocalDate) = _uiState.update { it.copy(fechaInicio = fecha) }

    fun onFechaLimite(fecha: LocalDate?) = _uiState.update { it.copy(fechaLimite = fecha) }

    fun onCrearOtra() = _uiState.update { it.copy(ignorarSugerencia = true) }

    fun guardar() {
        val estado = _uiState.value
        _uiState.update { it.copy(intento = true, errorGuardar = null) }
        val monto = estado.monto
        if (!estado.valido || estado.guardando || monto == null) return
        val descripcion = estado.descripcion.trim().ifBlank { null }
        val contraparte = estado.contraparte.trim()

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true) }
            val resultado: Result<String> = if (estado.editandoId != null) {
                deudaRepository.editar(
                    estado.editandoId,
                    CambiosDeuda(estado.tipo, contraparte, descripcion, monto, estado.fechaInicio, estado.fechaLimite),
                ).map { estado.editandoId }
            } else {
                deudaRepository.crear(
                    NuevaDeuda(estado.tipo, contraparte, descripcion, monto, estado.fechaInicio, estado.fechaLimite),
                ).map { it.id }
            }
            resultado
                .onSuccess { id -> _uiState.update { it.copy(guardando = false, guardadaId = id) } }
                .onFailure { e ->
                    _uiState.update { it.copy(guardando = false, errorGuardar = mensajeErrorDeuda(e) ?: e.comoErrorApp().mensajeRes()) }
                }
        }
    }

    companion object {
        val Factory = fabricaDe { FormularioDeudaViewModel(it.deudaRepository) }
    }
}
