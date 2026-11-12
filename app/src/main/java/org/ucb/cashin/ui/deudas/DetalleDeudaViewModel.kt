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
import org.ucb.cashin.model.EstadoDeuda
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.NuevoAbonoDeuda
import org.ucb.cashin.model.NuevoAumentoDeuda
import org.ucb.cashin.model.ResumenDeuda
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.prediccion.MovimientoDeuda
import org.ucb.cashin.prediccion.PasoDeuda
import org.ucb.cashin.prediccion.TipoMovimiento
import org.ucb.cashin.prediccion.ValidacionMovimiento
import org.ucb.cashin.prediccion.deudaContraria
import org.ucb.cashin.prediccion.lineaDeTiempo
import org.ucb.cashin.prediccion.movimientosDe
import org.ucb.cashin.prediccion.ultimoBorrable
import org.ucb.cashin.prediccion.validarAbono
import org.ucb.cashin.prediccion.validarAumento
import org.ucb.cashin.ui.components.mensajeRes
import org.ucb.cashin.util.Formato
import java.math.BigDecimal
import java.time.LocalDate

/** Qué hoja de abajo está abierta. */
enum class HojaDeuda { NINGUNA, ABONO, AUMENTO }

/** Todo lo que el detalle de una deuda necesita para dibujarse. */
data class DetalleDeudaUiState(
    val deuda: ResumenDeuda? = null,
    /** Del más antiguo al más nuevo, con el saldo después de cada uno. */
    val linea: List<PasoDeuda> = emptyList(),
    /** Deuda pendiente con la misma persona en el otro sentido ("También le debes Bs 20 a Amy"). */
    val contraria: ResumenDeuda? = null,
    val hoy: LocalDate = Formato.hoy(),
    val cargando: Boolean = true,
    val error: ErrorApp? = null,

    // Hoja abierta (abono o aumento)
    val hoja: HojaDeuda = HojaDeuda.NINGUNA,
    val montoTexto: String = "",
    val fecha: LocalDate = Formato.hoy(),
    val nota: String = "",
    val cambiarLimite: Boolean = false,
    val nuevaFechaLimite: LocalDate? = null,
    val guardando: Boolean = false,
    /** Error de la base de datos o de conexión al guardar o borrar. */
    @StringRes val errorGuardar: Int? = null,

    val confirmarBorrar: MovimientoDeuda? = null,
    val confirmarEliminar: Boolean = false,
    /** Se acaba de pagar todo: se muestra la celebración. */
    val celebrar: Boolean = false,
    val eliminada: Boolean = false,
) {
    val pagada: Boolean get() = deuda?.estado == EstadoDeuda.PAGADA
    val borrable: MovimientoDeuda? get() = ultimoBorrable(linea)
    val movimientos: List<MovimientoDeuda> get() = linea.map { it.movimiento }
    val montoIngresado: BigDecimal? get() = Formato.parsearMonto(montoTexto)

    /** Revisión en vivo de lo que se está escribiendo en la hoja (null si todavía no escribió nada). */
    val validacion: ValidacionMovimiento?
        get() {
            val d = deuda ?: return null
            if (montoTexto.isBlank()) return null
            val inicio = d.fechaInicio ?: return null
            return when (hoja) {
                HojaDeuda.ABONO -> validarAbono(movimientos, montoIngresado, fecha, inicio, hoy)
                HojaDeuda.AUMENTO -> validarAumento(montoIngresado, fecha, inicio, hoy)
                HojaDeuda.NINGUNA -> null
            }
        }
}

/**
 * HU-26: detalle de una deuda con su línea de tiempo. Se puede anotar un abono, sumar un préstamo
 * a la misma deuda y borrar solo el último movimiento. La base de datos recalcula el total y el estado.
 */
class DetalleDeudaViewModel(private val deudaRepository: DeudaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DetalleDeudaUiState())
    val uiState: StateFlow<DetalleDeudaUiState> = _uiState.asStateFlow()

    private var deudaId: String = ""

    fun cargar(id: String) {
        deudaId = id
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = it.deuda == null, error = null, hoy = Formato.hoy()) }
            recargar().onFailure { e -> _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) } }
        }
    }

    /** Lee la deuda, sus movimientos y las demás deudas (para el aviso de la misma persona). */
    private suspend fun recargar(): Result<ResumenDeuda> {
        val deuda = deudaRepository.obtener(deudaId).getOrElse { return Result.failure(it) }
        val guardados = deudaRepository.movimientos(deudaId).getOrElse { return Result.failure(it) }
        val todas = deudaRepository.listar().getOrDefault(emptyList())
        val linea = lineaDeTiempo(
            movimientosDe(
                montoInicial = deuda.montoInicial ?: deuda.montoTotal,
                fechaInicio = deuda.fechaInicio ?: Formato.hoy(),
                notaInicio = deuda.descripcion,
                aumentos = guardados.aumentos,
                abonos = guardados.abonos,
            ),
        )
        _uiState.update {
            it.copy(deuda = deuda, linea = linea, contraria = deudaContraria(deuda, todas), cargando = false)
        }
        return Result.success(deuda)
    }

    /** Abre la hoja. [monto] llega desde "Sumar a la de Amy" en el formulario. */
    fun abrirHoja(hoja: HojaDeuda, monto: String? = null) = _uiState.update {
        it.copy(
            hoja = hoja,
            montoTexto = monto.orEmpty(),
            fecha = it.hoy,
            nota = "",
            cambiarLimite = false,
            nuevaFechaLimite = it.deuda?.fechaLimite,
            errorGuardar = null,
        )
    }

    fun cerrarHoja() = _uiState.update { it.copy(hoja = HojaDeuda.NINGUNA, errorGuardar = null) }

    fun onMonto(texto: String) = _uiState.update { it.copy(montoTexto = texto, errorGuardar = null) }

    fun onFecha(fecha: LocalDate) = _uiState.update { it.copy(fecha = fecha, errorGuardar = null) }

    fun onNota(texto: String) = _uiState.update { it.copy(nota = texto.take(200)) }

    /** "Todo · Bs 70": llena lo que falta. */
    fun onTodo() = _uiState.update { it.copy(montoTexto = it.deuda?.pendiente?.toPlainString().orEmpty().replace('.', ','), errorGuardar = null) }

    fun onCambiarLimite(activo: Boolean) = _uiState.update { it.copy(cambiarLimite = activo) }

    fun onNuevaFechaLimite(fecha: LocalDate?) = _uiState.update { it.copy(nuevaFechaLimite = fecha) }

    fun guardar() {
        val estado = _uiState.value
        val deuda = estado.deuda ?: return
        val monto = estado.montoIngresado
        if (estado.guardando || monto == null || estado.validacion?.valido != true) return
        val nota = estado.nota.trim().ifBlank { null }

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, errorGuardar = null) }
            val resultado = when (estado.hoja) {
                HojaDeuda.ABONO -> deudaRepository.anotarAbono(NuevoAbonoDeuda(deuda.deudaId, monto, estado.fecha, nota)).map { }
                HojaDeuda.AUMENTO -> deudaRepository.anotarAumento(NuevoAumentoDeuda(deuda.deudaId, monto, estado.fecha, nota)).map { }
                HojaDeuda.NINGUNA -> Result.success(Unit)
            }
            resultado.onFailure { e ->
                _uiState.update { it.copy(guardando = false, errorGuardar = mensajeErrorDeuda(e) ?: e.comoErrorApp().mensajeRes()) }
                return@launch
            }
            // La fecha límite es aparte: si falla, el préstamo ya quedó guardado.
            if (estado.hoja == HojaDeuda.AUMENTO && estado.cambiarLimite && estado.nuevaFechaLimite != deuda.fechaLimite) {
                deudaRepository.cambiarFechaLimite(deuda.deudaId, estado.nuevaFechaLimite)
            }
            val antes = deuda.estado
            val nueva = recargar().getOrNull()
            _uiState.update {
                it.copy(
                    guardando = false,
                    hoja = HojaDeuda.NINGUNA,
                    celebrar = antes == EstadoDeuda.PENDIENTE && nueva?.estado == EstadoDeuda.PAGADA,
                )
            }
        }
    }

    fun pedirBorrar(movimiento: MovimientoDeuda) = _uiState.update { it.copy(confirmarBorrar = movimiento, errorGuardar = null) }

    fun cancelarBorrar() = _uiState.update { it.copy(confirmarBorrar = null) }

    /** Solo el último movimiento se puede borrar; la base de datos también lo revisa. */
    fun confirmarBorrar() {
        val movimiento = _uiState.value.confirmarBorrar ?: return
        val id = movimiento.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(confirmarBorrar = null, guardando = true) }
            val resultado = if (movimiento.tipo == TipoMovimiento.ABONO) deudaRepository.borrarAbono(id) else deudaRepository.borrarAumento(id)
            resultado.onFailure { e ->
                _uiState.update { it.copy(guardando = false, errorGuardar = mensajeErrorDeuda(e) ?: e.comoErrorApp().mensajeRes()) }
                return@launch
            }
            recargar()
            _uiState.update { it.copy(guardando = false) }
        }
    }

    fun pedirEliminar() = _uiState.update { it.copy(confirmarEliminar = true) }

    fun cancelarEliminar() = _uiState.update { it.copy(confirmarEliminar = false) }

    fun confirmarEliminar() {
        viewModelScope.launch {
            _uiState.update { it.copy(confirmarEliminar = false, guardando = true) }
            deudaRepository.eliminar(deudaId)
                .onSuccess { _uiState.update { it.copy(guardando = false, eliminada = true) } }
                .onFailure { e -> _uiState.update { it.copy(guardando = false, errorGuardar = e.comoErrorApp().mensajeRes()) } }
        }
    }

    fun cerrarCelebracion() = _uiState.update { it.copy(celebrar = false) }

    fun limpiarError() = _uiState.update { it.copy(errorGuardar = null) }

    companion object {
        val Factory = fabricaDe { DetalleDeudaViewModel(it.deudaRepository) }
    }
}
