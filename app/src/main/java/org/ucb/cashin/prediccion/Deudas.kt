package org.ucb.cashin.prediccion

import org.ucb.cashin.model.AbonoDeuda
import org.ucb.cashin.model.AumentoDeuda
import org.ucb.cashin.model.EstadoDeuda
import org.ucb.cashin.model.ResumenDeuda
import org.ucb.cashin.model.TipoDeuda
import java.math.BigDecimal
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/*
 * Deudas (HU-25, HU-26): una deuda es una historia de movimientos con una persona.
 * Inicio + aumentos − abonos = lo que falta. Las mismas reglas las revisa la base de datos
 * (migración 009); aquí se calculan antes de guardar para mostrar un mensaje claro.
 * Kotlin puro y con BigDecimal: son sumas exactas.
 */

/** El orden también decide qué va primero el mismo día: inicio, luego aumentos y al final abonos. */
enum class TipoMovimiento { INICIO, AUMENTO, ABONO }

/** Un movimiento de la línea de tiempo. [id] es null solo en el inicio (no es una fila propia). */
data class MovimientoDeuda(
    val id: String?,
    val tipo: TipoMovimiento,
    val monto: BigDecimal,
    val fecha: LocalDate,
    val nota: String? = null,
    val creadoEn: Instant? = null,
)

/** Un movimiento y cuánto se debía justo después de él. */
data class PasoDeuda(
    val movimiento: MovimientoDeuda,
    val saldo: BigDecimal,
)

/** Mismo orden que la base de datos: fecha, tipo, momento en que se anotó e id. */
private val ORDEN_MOVIMIENTOS: Comparator<MovimientoDeuda> =
    compareBy<MovimientoDeuda> { it.fecha }
        .thenBy { it.tipo.ordinal }
        .thenBy { it.creadoEn ?: Instant.MIN }
        .thenBy { it.id.orEmpty() }

/** Junta el inicio, los aumentos y los abonos de una deuda en una sola lista. */
fun movimientosDe(
    montoInicial: BigDecimal,
    fechaInicio: LocalDate,
    notaInicio: String?,
    aumentos: List<AumentoDeuda>,
    abonos: List<AbonoDeuda>,
): List<MovimientoDeuda> =
    listOf(MovimientoDeuda(null, TipoMovimiento.INICIO, montoInicial, fechaInicio, notaInicio)) +
        aumentos.map { MovimientoDeuda(it.id, TipoMovimiento.AUMENTO, it.monto, it.fecha, it.nota, it.creadoEn) } +
        abonos.map { MovimientoDeuda(it.id, TipoMovimiento.ABONO, it.monto, it.fecha, it.nota, it.creadoEn) }

/** La línea de tiempo del más antiguo al más nuevo, con el saldo después de cada movimiento. */
fun lineaDeTiempo(movimientos: List<MovimientoDeuda>): List<PasoDeuda> {
    var saldo = BigDecimal.ZERO
    return movimientos.sortedWith(ORDEN_MOVIMIENTOS).map { mov ->
        saldo = if (mov.tipo == TipoMovimiento.ABONO) saldo - mov.monto else saldo + mov.monto
        PasoDeuda(mov, saldo)
    }
}

/**
 * El único movimiento que se puede borrar: el último de la línea de tiempo.
 * El inicio no se borra (para eso se borra la deuda entera).
 */
fun ultimoBorrable(linea: List<PasoDeuda>): MovimientoDeuda? =
    linea.lastOrNull()?.movimiento?.takeIf { it.tipo != TipoMovimiento.INICIO }

/** Por qué un movimiento no se puede guardar. */
enum class ProblemaMovimiento {
    MONTO_INVALIDO,
    FECHA_ANTES_DEL_INICIO,
    FECHA_FUTURA,
    /** El abono pasa de lo que falta. [ValidacionMovimiento.limite] = lo que falta. */
    SUPERA_PENDIENTE,
    /** En la fecha elegida todavía no se debía tanto. [ValidacionMovimiento.limite] = lo que se debía ese día. */
    NO_SE_DEBIA_TANTO,
}

data class ValidacionMovimiento(
    val problema: ProblemaMovimiento? = null,
    val limite: BigDecimal? = null,
) {
    val valido: Boolean get() = problema == null
}

/** Monto mayor a 0 y con 2 decimales como máximo, como NUMERIC(14,2). */
fun montoValido(monto: BigDecimal?): Boolean =
    monto != null && monto.signum() > 0 && monto.stripTrailingZeros().scale() <= 2

private fun validarFecha(fecha: LocalDate, fechaInicio: LocalDate, hoy: LocalDate): ProblemaMovimiento? = when {
    fecha.isBefore(fechaInicio) -> ProblemaMovimiento.FECHA_ANTES_DEL_INICIO
    fecha.isAfter(hoy) -> ProblemaMovimiento.FECHA_FUTURA
    else -> null
}

/**
 * Revisa un abono antes de guardarlo: monto, fecha y que el saldo no quede negativo
 * ni al final ni en ningún momento de la historia.
 */
fun validarAbono(
    movimientos: List<MovimientoDeuda>,
    monto: BigDecimal?,
    fecha: LocalDate,
    fechaInicio: LocalDate,
    hoy: LocalDate,
): ValidacionMovimiento {
    if (!montoValido(monto)) return ValidacionMovimiento(ProblemaMovimiento.MONTO_INVALIDO)
    validarFecha(fecha, fechaInicio, hoy)?.let { return ValidacionMovimiento(it) }

    // El abono nuevo se anota ahora: queda último entre los del mismo día.
    val nuevo = MovimientoDeuda("~nuevo", TipoMovimiento.ABONO, monto!!, fecha, creadoEn = Instant.MAX)
    val linea = lineaDeTiempo(movimientos + nuevo)
    val pendiente = lineaDeTiempo(movimientos).lastOrNull()?.saldo ?: BigDecimal.ZERO
    if (linea.last().saldo.signum() < 0) {
        return ValidacionMovimiento(ProblemaMovimiento.SUPERA_PENDIENTE, pendiente)
    }
    val indice = linea.indexOfFirst { it.movimiento === nuevo }
    if (linea.any { it.saldo.signum() < 0 }) {
        val antes = if (indice > 0) linea[indice - 1].saldo else BigDecimal.ZERO
        return ValidacionMovimiento(ProblemaMovimiento.NO_SE_DEBIA_TANTO, antes)
    }
    return ValidacionMovimiento()
}

/** Un aumento nunca deja el saldo negativo: basta revisar el monto y la fecha. */
fun validarAumento(monto: BigDecimal?, fecha: LocalDate, fechaInicio: LocalDate, hoy: LocalDate): ValidacionMovimiento {
    if (!montoValido(monto)) return ValidacionMovimiento(ProblemaMovimiento.MONTO_INVALIDO)
    validarFecha(fecha, fechaInicio, hoy)?.let { return ValidacionMovimiento(it) }
    return ValidacionMovimiento()
}

/** Por qué no se puede guardar la edición de una deuda. */
enum class ProblemaEdicion {
    MONTO_INVALIDO,
    INICIO_FUTURO,
    /** Algún movimiento quedaría antes de la nueva fecha de inicio. */
    MOVIMIENTO_ANTES_DEL_INICIO,
    /** Con ese monto inicial, lo devuelto pasaría lo prestado. [limite] = el mínimo posible. */
    MENOR_QUE_LO_DEVUELTO,
}

data class ValidacionEdicion(val problema: ProblemaEdicion? = null, val limite: BigDecimal? = null)

/** Revisa un nuevo monto inicial y fecha de inicio contra los movimientos que ya existen. */
fun validarEdicion(
    movimientos: List<MovimientoDeuda>,
    montoInicial: BigDecimal?,
    fechaInicio: LocalDate,
    hoy: LocalDate,
): ValidacionEdicion {
    if (!montoValido(montoInicial)) return ValidacionEdicion(ProblemaEdicion.MONTO_INVALIDO)
    if (fechaInicio.isAfter(hoy)) return ValidacionEdicion(ProblemaEdicion.INICIO_FUTURO)
    val otros = movimientos.filter { it.tipo != TipoMovimiento.INICIO }
    if (otros.any { it.fecha.isBefore(fechaInicio) }) return ValidacionEdicion(ProblemaEdicion.MOVIMIENTO_ANTES_DEL_INICIO)
    val inicio = MovimientoDeuda(null, TipoMovimiento.INICIO, montoInicial!!, fechaInicio)
    val minimo = lineaDeTiempo(otros + inicio).minOf { it.saldo }
    if (minimo.signum() < 0) {
        return ValidacionEdicion(ProblemaEdicion.MENOR_QUE_LO_DEVUELTO, montoInicial - minimo)
    }
    return ValidacionEdicion()
}

// ------------------------------------------------------------------ Vencimiento

/** Desde cuántos días antes se avisa que una deuda vence pronto. */
const val DIAS_VENCE_PRONTO = 7L

enum class AvisoVencimiento { SIN_FECHA, NORMAL, VENCE_PRONTO, VENCE_HOY, VENCIDA }

/** [dias]: los que faltan (NORMAL, VENCE_PRONTO) o los que pasaron (VENCIDA). */
data class Vencimiento(val aviso: AvisoVencimiento, val dias: Long = 0)

/** Null si la deuda ya está pagada: no tiene sentido avisar. */
fun vencimientoDe(fechaLimite: LocalDate?, pagada: Boolean, hoy: LocalDate): Vencimiento? {
    if (pagada) return null
    if (fechaLimite == null) return Vencimiento(AvisoVencimiento.SIN_FECHA)
    val dias = ChronoUnit.DAYS.between(hoy, fechaLimite)
    return when {
        dias < 0 -> Vencimiento(AvisoVencimiento.VENCIDA, -dias)
        dias == 0L -> Vencimiento(AvisoVencimiento.VENCE_HOY)
        dias <= DIAS_VENCE_PRONTO -> Vencimiento(AvisoVencimiento.VENCE_PRONTO, dias)
        else -> Vencimiento(AvisoVencimiento.NORMAL, dias)
    }
}

// ------------------------------------------------------------------ Misma persona

/** "  Ámy  R. " → "amy r.": sin tildes, sin mayúsculas y sin espacios de más. */
fun normalizarNombre(nombre: String): String =
    Normalizer.normalize(nombre.trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("\\s+"), " ")
        .lowercase()

/**
 * Deuda pendiente con la misma persona y del mismo tipo, para preguntar
 * "¿Sumar a la de Amy?" en vez de crear otra. Si hay varias, la más reciente.
 */
fun deudaAbiertaCon(nombre: String, tipo: TipoDeuda, deudas: List<ResumenDeuda>, excluirId: String? = null): ResumenDeuda? {
    val buscado = normalizarNombre(nombre)
    if (buscado.isEmpty()) return null
    return deudas
        .filter { it.deudaId != excluirId && it.tipo == tipo && it.estado == EstadoDeuda.PENDIENTE }
        .filter { normalizarNombre(it.contraparte) == buscado }
        .maxByOrNull { it.fechaInicio ?: LocalDate.MIN }
}

/** "También le debes Bs 20 a Amy": la deuda pendiente con la misma persona en el otro sentido. */
fun deudaContraria(deuda: ResumenDeuda, deudas: List<ResumenDeuda>): ResumenDeuda? {
    val otroTipo = if (deuda.tipo == TipoDeuda.DEBO) TipoDeuda.ME_DEBEN else TipoDeuda.DEBO
    return deudaAbiertaCon(deuda.contraparte, otroTipo, deudas)
}

/** Suma de lo pendiente de las deudas abiertas de un tipo. */
fun totalPendiente(deudas: List<ResumenDeuda>, tipo: TipoDeuda): BigDecimal =
    deudas.filter { it.tipo == tipo && it.estado == EstadoDeuda.PENDIENTE }
        .fold(BigDecimal.ZERO) { suma, d -> suma + d.pendiente }
