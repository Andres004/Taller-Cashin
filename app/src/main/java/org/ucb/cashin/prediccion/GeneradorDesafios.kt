package org.ucb.cashin.prediccion

import org.ucb.cashin.model.GastoCategoriaMes
import org.ucb.cashin.model.PeriodoDesafio
import org.ucb.cashin.model.ReglaDesafio
import org.ucb.cashin.model.TotalesMes
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.floor
import kotlin.math.max

/**
 * HU-22: un desafío que el motor propone y la persona puede aceptar.
 * Título y descripción no van aquí: la pantalla los arma con plantillas de strings.xml.
 */
data class PropuestaDesafio(
    val periodo: PeriodoDesafio,
    val regla: ReglaDesafio,
    val categoriaId: String? = null,
    val categoriaNombre: String? = null,
    val montoLimite: BigDecimal? = null,
    val diasObjetivo: Int? = null,
    /** Promedio o pronóstico que se usó (para explicarlo en pantalla). */
    val basadoEn: BigDecimal? = null,
)

/** Para comparar con el promedio se miran como máximo los últimos 3 meses completos. */
internal const val MESES_PROMEDIO_CATEGORIA = 3

/** Recorte sobre lo de costumbre: 15 % menos en la categoría, 10 % menos en el total. */
private const val RECORTE_CATEGORIA = 0.85
private const val RECORTE_TOTAL = 0.90

/** Los límites se redondean hacia abajo a múltiplos de Bs 5 (y nunca bajan de Bs 5). */
private const val MULTIPLO_LIMITE = 5.0

/** Días que hay que anotar para cumplir el desafío de registro. */
private const val DIAS_SEMANAL = 5
private const val DIAS_MENSUAL = 20

/** Qué parte de un mes es el periodo: una semana ≈ 7 / 30,44 de un mes promedio. */
internal fun factorPeriodo(periodo: PeriodoDesafio): Double = when (periodo) {
    PeriodoDesafio.SEMANAL -> 7.0 / 30.44
    PeriodoDesafio.MENSUAL -> 1.0
}

/**
 * Propone hasta 3 desafíos para el [periodo], uno por regla y en este orden:
 *
 * 1. **LIMITE_CATEGORIA:** la categoría con mayor gasto promedio mensual en los últimos 3 meses
 *    completos. Límite = promedio × factor del periodo × 0,85.
 * 2. **LIMITE_TOTAL:** con el pronóstico del gasto del mes (regresión + promedio ponderado).
 *    Límite = esperado × factor del periodo × 0,90.
 * 3. **DIAS_REGISTRO:** siempre (aunque no haya historia): 5 días a la semana o 20 al mes.
 *
 * Los límites se redondean hacia abajo a múltiplos de Bs 5. Nada sale con monto ≤ 0.
 */
fun proponerDesafios(
    periodo: PeriodoDesafio,
    gastoPorCategoria: List<GastoCategoriaMes>,
    historial: List<TotalesMes>,
    hoy: LocalDate,
): List<PropuestaDesafio> {
    val factor = factorPeriodo(periodo)
    val propuestas = mutableListOf<PropuestaDesafio>()

    promediosPorCategoria(gastoPorCategoria, hoy).maxByOrNull { it.promedio }
        ?.takeIf { it.promedio > 0 }
        ?.let { mayor ->
            propuestas += PropuestaDesafio(
                periodo = periodo,
                regla = ReglaDesafio.LIMITE_CATEGORIA,
                categoriaId = mayor.categoriaId,
                categoriaNombre = mayor.nombre,
                montoLimite = limiteRedondeado(mayor.promedio * factor * RECORTE_CATEGORIA),
                basadoEn = aMonto(mayor.promedio),
            )
        }

    val pronostico = pronosticarSiguienteMes(construirSerie(historial, hoy) { it.totalGastos })
    if (pronostico.confianza != Confianza.SIN_DATOS && pronostico.esperado.signum() > 0) {
        propuestas += PropuestaDesafio(
            periodo = periodo,
            regla = ReglaDesafio.LIMITE_TOTAL,
            montoLimite = limiteRedondeado(pronostico.esperado.toDouble() * factor * RECORTE_TOTAL),
            basadoEn = pronostico.esperado,
        )
    }

    propuestas += PropuestaDesafio(
        periodo = periodo,
        regla = ReglaDesafio.DIAS_REGISTRO,
        diasObjetivo = if (periodo == PeriodoDesafio.SEMANAL) DIAS_SEMANAL else DIAS_MENSUAL,
    )
    return propuestas
}

/** Gasto promedio mensual de una categoría en los últimos meses completos. */
internal data class PromedioCategoria(
    val categoriaId: String,
    val nombre: String,
    val promedio: Double,
)

/**
 * Promedio mensual de cada categoría en los últimos [MESES_PROMEDIO_CATEGORIA] meses completos
 * (sin el actual). Se divide entre los meses de esa ventana desde el primer mes con datos,
 * así un mes sin gastos en la categoría cuenta como 0. Sin meses completos → lista vacía.
 */
internal fun promediosPorCategoria(
    gastoPorCategoria: List<GastoCategoriaMes>,
    hoy: LocalDate,
): List<PromedioCategoria> {
    val mesActual = YearMonth.from(hoy)
    val completos = gastoPorCategoria.filter { YearMonth.from(it.mes) < mesActual }
    if (completos.isEmpty()) return emptyList()

    val primerMes = completos.minOf { YearMonth.from(it.mes) }
    val inicioVentana = maxOf(primerMes, mesActual.minusMonths(MESES_PROMEDIO_CATEGORIA.toLong()))
    val mesesVentana = generateSequence(inicioVentana) { it.plusMonths(1) }.takeWhile { it < mesActual }.count()

    return completos
        .filter { YearMonth.from(it.mes) >= inicioVentana }
        .groupBy { it.categoriaId }
        .map { (categoriaId, filas) ->
            PromedioCategoria(
                categoriaId = categoriaId,
                nombre = filas.last().nombre,
                promedio = filas.sumOf { it.total.toDouble() } / mesesVentana,
            )
        }
}

/** Redondea hacia abajo a múltiplos de Bs 5, con un mínimo de Bs 5. */
private fun limiteRedondeado(valor: Double): BigDecimal {
    val redondeado = floor(valor / MULTIPLO_LIMITE) * MULTIPLO_LIMITE
    return aMonto(max(redondeado, MULTIPLO_LIMITE))
}
