package org.ucb.cashin.prediccion

import org.ucb.cashin.model.TotalesMes
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/*
 * Paquete prediccion/: el "modelo" de la IA de Cashin.
 * Kotlin puro: no importa android.*, androidx.* ni Supabase, así se prueba con JUnit.
 * Aquí (y solo aquí) se permite Double para los cálculos estadísticos;
 * todo lo que sale hacia la app vuelve como BigDecimal redondeado a 2 decimales.
 */

/** Un mes de la historia del usuario y su total (gastos o ingresos). */
data class PuntoMensual(
    val mes: YearMonth,
    val valor: Double,
)

/** Cuántos meses de historia usa el modelo como máximo. */
const val MAX_MESES_SERIE = 12

/**
 * Convierte las filas de `v_totales_mes` en una serie mes a mes para el modelo.
 *
 * - Usa solo **meses completos**: el mes actual queda fuera porque todavía no terminó.
 * - Rellena con 0 los meses sin anotaciones entre el primer mes con datos y el mes anterior al actual
 *   (un mes sin anotar también es información).
 * - Devuelve como máximo los últimos [MAX_MESES_SERIE] meses, del más antiguo al más reciente.
 *
 * [campo] elige qué se mide: `{ it.totalGastos }` o `{ it.totalIngresos }`.
 */
fun construirSerie(
    totales: List<TotalesMes>,
    hoy: LocalDate,
    campo: (TotalesMes) -> BigDecimal,
): List<PuntoMensual> {
    val mesActual = YearMonth.from(hoy)

    val porMes: Map<YearMonth, Double> = totales
        .filter { YearMonth.from(it.mes) < mesActual }
        .groupBy { YearMonth.from(it.mes) }
        .mapValues { (_, filas) -> filas.sumOf { campo(it).toDouble() } }

    if (porMes.isEmpty()) return emptyList()

    val primero = porMes.keys.min()
    val ultimo = mesActual.minusMonths(1)

    return generateSequence(primero) { it.plusMonths(1) }
        .takeWhile { it <= ultimo }
        .map { mes -> PuntoMensual(mes, porMes[mes] ?: 0.0) }
        .toList()
        .takeLast(MAX_MESES_SERIE)
}