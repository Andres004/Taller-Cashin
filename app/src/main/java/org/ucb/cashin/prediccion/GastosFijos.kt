package org.ucb.cashin.prediccion

import org.ucb.cashin.model.EstadoPlanificado
import org.ucb.cashin.model.Frecuencia
import org.ucb.cashin.model.GastoCategoriaMes
import org.ucb.cashin.model.IMPORTANCIA_POR_DEFECTO
import org.ucb.cashin.model.RegistroPlanificado
import org.ucb.cashin.model.TipoRegistro
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/** Gasto de costumbre de una categoría, separado en lo que no se puede dejar y lo que sí. */
data class DesgloseCategoria(
    val categoriaId: String,
    val fijo: BigDecimal,
    val variable: BigDecimal,
    val importancia: Int,
    val recortable: BigDecimal,
)

/** Montos mensuales: lo comprometido en pagos fijos, lo variable y cuánto de eso se podría recortar. */
data class DesgloseGastos(
    val porCategoria: List<DesgloseCategoria>,
) {
    val fijoMensual: BigDecimal get() = porCategoria.fold(CERO) { suma, c -> suma + c.fijo }
    val variableMensual: BigDecimal get() = porCategoria.fold(CERO) { suma, c -> suma + c.variable }
    val margenRecortable: BigDecimal get() = porCategoria.fold(CERO) { suma, c -> suma + c.recortable }
    val comprometidoMensual: BigDecimal get() = fijoMensual + variableMensual
}

private val CERO: BigDecimal = BigDecimal.ZERO.setScale(2)
private val DOCE = BigDecimal.valueOf(12)
private val CIEN = BigDecimal.valueOf(100)

/**
 * Cuánto cuesta por mes un pago planificado. Null si no es un compromiso mensual:
 * un ingreso, un pago único o uno pausado o cancelado.
 */
fun RegistroPlanificado.montoMensual(): BigDecimal? {
    if (tipo != TipoRegistro.GASTO || estado != EstadoPlanificado.ACTIVO) return null
    val vecesPorAnio = when (frecuencia) {
        Frecuencia.UNICA -> return null
        Frecuencia.SEMANAL -> 52L
        Frecuencia.QUINCENAL -> 26L // la app avanza cada 2 semanas
        Frecuencia.MENSUAL -> 12L
        Frecuencia.ANUAL -> 1L
    }
    return (monto * BigDecimal.valueOf(vecesPorAnio)).divide(DOCE, 2, RoundingMode.HALF_UP)
}

/** Qué parte del gasto variable se considera recortable según su importancia (1 a 5). */
fun porcentajeRecortable(importancia: Int): BigDecimal = when (importancia.coerceIn(1, 5)) {
    5 -> BigDecimal.ZERO
    4 -> BigDecimal.valueOf(5)
    3 -> BigDecimal.valueOf(15)
    2 -> BigDecimal.valueOf(30)
    else -> BigDecimal.valueOf(50)
}

/**
 * Separa el gasto de costumbre en fijo y variable, por categoría.
 *
 * - Fijo: la suma mensual de sus planificados activos, aunque aún no estén en el historial.
 * - Variable: máx(0, promedio histórico de la categoría − fijo). Así, un alquiler anotado a mano
 *   que luego se volvió planificado no se cuenta dos veces.
 * - Recortable: solo sobre lo variable; un pago planificado nunca se considera recortable.
 *
 * Una categoría sin importancia conocida usa [IMPORTANCIA_POR_DEFECTO].
 */
fun desglosarGastos(
    planificados: List<RegistroPlanificado>,
    gastoPorCategoria: List<GastoCategoriaMes>,
    importancias: Map<String, Int>,
    hoy: LocalDate,
): DesgloseGastos {
    val fijos: Map<String, BigDecimal> = planificados
        .mapNotNull { p -> p.montoMensual()?.let { p.categoriaId to it } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, montos) -> montos.fold(CERO) { suma, monto -> suma + monto } }
    val promedios: Map<String, BigDecimal> = promediosPorCategoria(gastoPorCategoria, hoy)
        .associate { it.categoriaId to BigDecimal.valueOf(it.promedio).setScale(2, RoundingMode.HALF_UP) }

    val categorias = (fijos.keys + promedios.keys).map { id ->
        val fijo = fijos[id] ?: CERO
        val variable = ((promedios[id] ?: CERO) - fijo).max(CERO)
        val importancia = importancias[id] ?: IMPORTANCIA_POR_DEFECTO
        val recortable = (variable * porcentajeRecortable(importancia)).divide(CIEN, 2, RoundingMode.HALF_UP)
        DesgloseCategoria(id, fijo, variable, importancia, recortable)
    }
    return DesgloseGastos(categorias)
}

/**
 * Lo que queda libre al mes sin hacer ningún sacrificio. Se toma el mayor gasto entre el pronóstico
 * y lo comprometido: un pago recién creado baja la capacidad aunque aún no esté en el historial.
 */
fun capacidadDeAhorro(
    ingresosEsperados: BigDecimal,
    gastosEsperados: BigDecimal,
    desglose: DesgloseGastos?,
): BigDecimal {
    val gastos = desglose?.let { gastosEsperados.max(it.comprometidoMensual) } ?: gastosEsperados
    return (ingresosEsperados - gastos).setScale(2, RoundingMode.HALF_UP)
}
