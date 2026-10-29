package org.ucb.cashin.prediccion

import org.ucb.cashin.model.AporteMeta
import org.ucb.cashin.model.AvanceMeta
import org.ucb.cashin.model.TipoAporte
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** De dónde sale el ritmo de ahorro que se usa para estimar. */
enum class FuenteAhorro {
    /** Del historial de ahorros y retiros de la propia meta. */
    APORTES,
    /** De la capacidad de ahorro: ingresos esperados − gastos esperados. */
    CAPACIDAD,
    /** No hay datos suficientes. */
    SIN_DATOS,
}

/** HU-20: cuánto falta para la meta y cuándo se lograría al ritmo actual. */
data class EstimacionMeta(
    val alcanzada: Boolean,
    val faltante: BigDecimal,
    /** Ritmo de ahorro mensual usado para estimar. */
    val ahorroMensual: BigDecimal,
    val fuente: FuenteAhorro,
    /** Meses que faltan; null si al ritmo actual no se alcanza. */
    val meses: Int?,
    val fechaEstimada: LocalDate?,
    /** null si la meta no tiene fecha límite. */
    val llegaAntesDeLaFecha: Boolean?,
    /** Cuánto habría que ahorrar por mes para llegar a la fecha límite (si tiene). */
    val ahorroNecesarioPorMes: BigDecimal?,
    val confianza: Confianza,
)

/** Para el ritmo por aportes se miran como máximo los últimos 3 meses con aportes. */
private const val MESES_RITMO_APORTES = 3

/**
 * Estima cuándo se alcanza una meta.
 *
 * Ritmo de ahorro:
 * 1. Si hay aportes en **2 o más meses distintos**: promedio mensual de los aportes netos
 *    (AHORRO − RETIRO) de los últimos 3 meses con aportes → APORTES.
 * 2. Si no: capacidad = ingresos esperados − gastos esperados → CAPACIDAD
 *    (la confianza es la menor de los dos pronósticos).
 * 3. Si tampoco hay pronósticos → SIN_DATOS.
 *
 * Con el ritmo: meses = ⌈faltante ÷ ritmo⌉ y fecha estimada = hoy + meses.
 * Si el ritmo es 0 o negativo, la meta no se alcanza al ritmo actual (meses = null).
 * Con fecha límite: se compara la fecha estimada y se calcula cuánto haría falta por mes.
 */
fun estimarMeta(
    avance: AvanceMeta,
    aportes: List<AporteMeta>,
    pronosticoIngresos: Pronostico,
    pronosticoGastos: Pronostico,
    hoy: LocalDate,
): EstimacionMeta {
    val faltante = (avance.montoObjetivo - avance.totalAhorrado).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
    val fechaLimite = avance.fechaLimite

    if (faltante.signum() == 0) {
        return EstimacionMeta(
            alcanzada = true,
            faltante = faltante,
            ahorroMensual = BigDecimal.ZERO.setScale(2),
            fuente = FuenteAhorro.APORTES,
            meses = 0,
            fechaEstimada = hoy,
            llegaAntesDeLaFecha = fechaLimite?.let { true },
            ahorroNecesarioPorMes = fechaLimite?.let { BigDecimal.ZERO.setScale(2) },
            confianza = Confianza.ALTA,
        )
    }

    val (ritmo, fuente, confianza) = ritmoDeAhorro(aportes, pronosticoIngresos, pronosticoGastos)

    val meses: Int? = if (fuente == FuenteAhorro.SIN_DATOS || ritmo.signum() <= 0) {
        null
    } else {
        faltante.divide(ritmo, 0, RoundingMode.CEILING).toInt()
    }
    val fechaEstimada = meses?.let { hoy.plusMonths(it.toLong()) }

    var llegaAntes: Boolean? = null
    var necesarioPorMes: BigDecimal? = null
    if (fechaLimite != null) {
        val mesesHastaLimite = ChronoUnit.MONTHS.between(hoy, fechaLimite).coerceAtLeast(1)
        llegaAntes = fechaEstimada != null && !fechaEstimada.isAfter(fechaLimite)
        necesarioPorMes = faltante.divide(BigDecimal.valueOf(mesesHastaLimite), 2, RoundingMode.HALF_UP)
    }

    return EstimacionMeta(
        alcanzada = false,
        faltante = faltante,
        ahorroMensual = ritmo,
        fuente = fuente,
        meses = meses,
        fechaEstimada = fechaEstimada,
        llegaAntesDeLaFecha = llegaAntes,
        ahorroNecesarioPorMes = necesarioPorMes,
        confianza = confianza,
    )
}

/** Ritmo mensual de ahorro, de dónde salió y qué tanto confiar en él. */
private fun ritmoDeAhorro(
    aportes: List<AporteMeta>,
    pronosticoIngresos: Pronostico,
    pronosticoGastos: Pronostico,
): Triple<BigDecimal, FuenteAhorro, Confianza> {
    // 1. Por aportes: neto de cada mes (ahorros suman, retiros restan).
    val netoPorMes: Map<YearMonth, BigDecimal> = aportes
        .groupBy { YearMonth.from(it.fecha) }
        .mapValues { (_, delMes) ->
            delMes.fold(BigDecimal.ZERO) { suma, aporte ->
                if (aporte.tipo == TipoAporte.AHORRO) suma + aporte.monto else suma - aporte.monto
            }
        }

    if (netoPorMes.size >= 2) {
        val recientes = netoPorMes.entries
            .sortedByDescending { it.key }
            .take(MESES_RITMO_APORTES)
            .map { it.value }
        val promedio = recientes
            .fold(BigDecimal.ZERO) { suma, valor -> suma + valor }
            .divide(BigDecimal.valueOf(recientes.size.toLong()), 2, RoundingMode.HALF_UP)
        val confianza = if (recientes.size >= MESES_RITMO_APORTES) Confianza.MEDIA else Confianza.BAJA
        return Triple(promedio, FuenteAhorro.APORTES, confianza)
    }

    // 2. Por capacidad: lo que suele entrar menos lo que suele salir.
    if (pronosticoIngresos.confianza != Confianza.SIN_DATOS && pronosticoGastos.confianza != Confianza.SIN_DATOS) {
        val capacidad = (pronosticoIngresos.esperado - pronosticoGastos.esperado).setScale(2, RoundingMode.HALF_UP)
        val confianza = minOf(pronosticoIngresos.confianza, pronosticoGastos.confianza)
        return Triple(capacidad, FuenteAhorro.CAPACIDAD, confianza)
    }

    // 3. Sin datos.
    return Triple(BigDecimal.ZERO.setScale(2), FuenteAhorro.SIN_DATOS, Confianza.SIN_DATOS)
}