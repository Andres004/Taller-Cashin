package org.ucb.cashin.prediccion

import org.ucb.cashin.model.GastoCategoriaMes
import org.ucb.cashin.model.IMPORTANCIA_POR_DEFECTO
import org.ucb.cashin.model.TotalesMes
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

/*
 * Reportes de Historial (HU-14): en qué categoría se va la plata, cómo cambia frente al mes anterior
 * y cómo vienen los últimos meses. Kotlin puro y con BigDecimal: son sumas y porcentajes exactos,
 * no estimaciones. La pantalla solo dibuja lo que sale de aquí.
 */

/** Hacia dónde se movió una categoría frente al mes anterior. */
enum class Tendencia { SUBE, BAJA, IGUAL, NUEVA }

/** Una porción de la dona: una categoría en el mes elegido. */
data class PorcionCategoria(
    val categoriaId: String,
    val nombre: String,
    val icono: String?,
    val color: String?,
    val total: BigDecimal,
    /** Porcentaje entero del total del mes. Las porciones de un reporte siempre suman 100. */
    val porcentaje: Int,
    /** Lo de la misma categoría en el mes anterior (0 si no hubo). */
    val anterior: BigDecimal,
    val tendencia: Tendencia,
    /** Cambio frente al mes anterior, en % entero y sin signo. Null si es [Tendencia.NUEVA]. */
    val cambioPorcentaje: Int?,
) {
    /** total − anterior: positivo si subió. */
    val diferencia: BigDecimal get() = total - anterior
}

/** Gastos (o ingresos) de un mes separados por categoría, de mayor a menor. */
data class ReporteCategorias(
    val mes: YearMonth,
    val total: BigDecimal,
    val totalAnterior: BigDecimal,
    val porciones: List<PorcionCategoria>,
) {
    val vacio: Boolean get() = porciones.isEmpty()

    /** total − totalAnterior: positivo si este mes fue mayor. */
    val diferencia: BigDecimal get() = total - totalAnterior

    /** Tendencia del mes completo frente al anterior. */
    val tendencia: Tendencia
        get() = tendenciaDe(total, totalAnterior)
}

/** Ingresos y gastos de un mes, para las barras de "Meses". */
data class MesResumen(
    val mes: YearMonth,
    val ingresos: BigDecimal,
    val gastos: BigDecimal,
) {
    /** Lo que sobró: ingresos − gastos. Es una diferencia de anotaciones, no un saldo. */
    val sobrante: BigDecimal get() = ingresos - gastos
}

/** El gasto del mes separado en fijo, variable y recortable. Las tres partes suman el total del mes. */
data class DivisionGastoMes(
    val fijo: BigDecimal,
    /** Lo variable que no se considera recortable. */
    val variable: BigDecimal,
    val recortable: BigDecimal,
) {
    val total: BigDecimal get() = fijo + variable + recortable
}

/** Qué frase de Cashi corresponde al reporte. Cada valor tiene su texto en strings.xml. */
enum class TipoFraseReporte {
    /** No hay datos en el mes elegido. */
    SIN_DATOS,
    /** Una categoría subió frente al mes anterior. [FraseReporte.nombre] y [FraseReporte.monto] = cuánto. */
    CATEGORIA_SUBIO,
    /** El gasto del mes bajó. [FraseReporte.monto] = cuánto menos. */
    GASTO_BAJO,
    /** El gasto va parecido al mes anterior (o no hay mes anterior para comparar). */
    GASTO_PARECIDO,
    /** Entró más por una categoría. [FraseReporte.nombre] y [FraseReporte.monto] = cuánto más. */
    INGRESO_EXTRA,
    /** Los ingresos van parecidos. [FraseReporte.monto] = lo que va sobrando (puede ser null). */
    INGRESOS_PARECIDOS,
    /** El mes es el mejor desde [FraseReporte.mesReferencia] (null: el mejor de la serie). [FraseReporte.monto] = lo que sobra. */
    MEJOR_MES,
    /** Sobra algo, pero no es el mejor mes. [FraseReporte.monto] = lo que sobra. */
    SOBRA,
    /** Este mes salió más de lo que entró. [FraseReporte.monto] = cuánto más. */
    NO_SOBRA,
}

/** La conclusión del reporte, lista para que la pantalla la convierta en texto y en una pose de Cashi. */
data class FraseReporte(
    val tipo: TipoFraseReporte,
    val nombre: String? = null,
    val monto: BigDecimal? = null,
    /** Monto adicional: en [TipoFraseReporte.INGRESO_EXTRA], lo que va sobrando en el mes. */
    val sobrante: BigDecimal? = null,
    val mesReferencia: YearMonth? = null,
)

/** Cuántos meses muestran las barras de "Meses". */
const val MESES_REPORTE = 6

/** Desde este % de cambio, una categoría "subió" para la frase de Cashi (por debajo es ruido). */
const val CAMBIO_MINIMO_PORCENTAJE = 10

private val CERO: BigDecimal = BigDecimal.ZERO.setScale(2)
private val CIEN = BigDecimal.valueOf(100)

private fun BigDecimal.dos(): BigDecimal = setScale(2, RoundingMode.HALF_UP)

private fun tendenciaDe(actual: BigDecimal, anterior: BigDecimal): Tendencia = when {
    anterior.signum() == 0 && actual.signum() > 0 -> Tendencia.NUEVA
    actual.compareTo(anterior) == 0 -> Tendencia.IGUAL
    actual > anterior -> Tendencia.SUBE
    else -> Tendencia.BAJA
}

/** % entero de cambio (sin signo) de [anterior] a [actual]. Null si no había nada antes. */
private fun cambioPorcentaje(actual: BigDecimal, anterior: BigDecimal): Int? =
    if (anterior.signum() == 0) null
    else (actual - anterior).abs().multiply(CIEN).divide(anterior, 0, RoundingMode.HALF_UP).toInt()

/**
 * Reparte 100 puntos entre [montos] según su peso, con el método del mayor resto:
 * los porcentajes enteros siempre suman exactamente 100 (si hay algo que repartir).
 */
internal fun porcentajesEnteros(montos: List<BigDecimal>): List<Int> {
    val total = montos.fold(BigDecimal.ZERO) { s, m -> s + m }
    if (total.signum() <= 0) return montos.map { 0 }
    val exactos = montos.map { it.multiply(CIEN).divide(total, 10, RoundingMode.HALF_UP) }
    val pisos = exactos.map { it.setScale(0, RoundingMode.FLOOR).toInt() }.toMutableList()
    val faltan = 100 - pisos.sum()
    exactos.indices
        .sortedWith(compareByDescending<Int> { exactos[it] - BigDecimal(pisos[it]) }.thenBy { it })
        .take(faltan)
        .forEach { pisos[it] += 1 }
    return pisos
}

/**
 * Arma el reporte por categoría de [mes] a partir de las filas de `v_gasto_categoria_mes`
 * (o `v_ingreso_categoria_mes`, que tiene la misma forma).
 *
 * - Solo entran las categorías con algo en [mes], ordenadas de mayor a menor (empate: por nombre).
 * - Cada porción se compara con la misma categoría del mes anterior.
 */
fun reportePorCategoria(filas: List<GastoCategoriaMes>, mes: YearMonth): ReporteCategorias {
    val delMes = filas.filter { YearMonth.from(it.mes) == mes && it.total.signum() > 0 }
        .groupBy { it.categoriaId }
        .map { (_, grupo) -> grupo.first() to grupo.fold(BigDecimal.ZERO) { s, f -> s + f.total } }
        .sortedWith(compareByDescending<Pair<GastoCategoriaMes, BigDecimal>> { it.second }.thenBy { it.first.nombre })
    val anterior: Map<String, BigDecimal> = filas.filter { YearMonth.from(it.mes) == mes.minusMonths(1) }
        .groupBy({ it.categoriaId }, { it.total })
        .mapValues { (_, montos) -> montos.fold(BigDecimal.ZERO) { s, m -> s + m } }

    val porcentajes = porcentajesEnteros(delMes.map { it.second })
    val porciones = delMes.mapIndexed { i, (fila, total) ->
        val antes = (anterior[fila.categoriaId] ?: BigDecimal.ZERO).dos()
        PorcionCategoria(
            categoriaId = fila.categoriaId,
            nombre = fila.nombre,
            icono = fila.icono,
            color = fila.color,
            total = total.dos(),
            porcentaje = porcentajes[i],
            anterior = antes,
            tendencia = tendenciaDe(total, antes),
            cambioPorcentaje = cambioPorcentaje(total, antes),
        )
    }
    return ReporteCategorias(
        mes = mes,
        total = porciones.fold(CERO) { s, p -> s + p.total },
        totalAnterior = anterior.values.fold(CERO) { s, m -> s + m }.dos(),
        porciones = porciones,
    )
}

/**
 * Ingresos y gastos de los [cantidad] meses que terminan en [hasta] (incluido), del más antiguo al más
 * reciente, a partir de `v_totales_mes`. Los meses sin anotaciones van en 0, pero se quitan los del
 * principio que están antes del primer mes con datos. Si no hay nada, devuelve una lista vacía.
 */
fun ultimosMeses(totales: List<TotalesMes>, hasta: YearMonth, cantidad: Int = MESES_REPORTE): List<MesResumen> {
    val porMes: Map<YearMonth, List<TotalesMes>> = totales.groupBy { YearMonth.from(it.mes) }
    val meses = (cantidad - 1 downTo 0).map { hasta.minusMonths(it.toLong()) }
        .map { mes ->
            val filas = porMes[mes].orEmpty()
            MesResumen(
                mes = mes,
                ingresos = filas.fold(CERO) { s, f -> s + f.totalIngresos }.dos(),
                gastos = filas.fold(CERO) { s, f -> s + f.totalGastos }.dos(),
            )
        }
    val primero = meses.indexOfFirst { it.ingresos.signum() > 0 || it.gastos.signum() > 0 }
    return if (primero < 0) emptyList() else meses.drop(primero)
}

/**
 * Separa el gasto de cada categoría del mes en fijo, variable y recortable:
 * - fijo: hasta lo comprometido en pagos planificados de esa categoría ([desglose]);
 * - recortable: la parte de lo variable que su importancia permite recortar ([porcentajeRecortable]);
 * - variable: el resto.
 * Las tres partes suman exactamente el total del reporte.
 */
fun dividirGastoDelMes(
    reporte: ReporteCategorias,
    desglose: DesgloseGastos?,
    importancias: Map<String, Int>,
): DivisionGastoMes {
    val fijos: Map<String, BigDecimal> = desglose?.porCategoria?.associate { it.categoriaId to it.fijo }.orEmpty()
    var fijo = CERO
    var variable = CERO
    var recortable = CERO
    reporte.porciones.forEach { p ->
        val f = p.total.min(fijos[p.categoriaId] ?: CERO)
        val resto = p.total - f
        val importancia = importancias[p.categoriaId] ?: IMPORTANCIA_POR_DEFECTO
        val r = (resto * porcentajeRecortable(importancia)).divide(CIEN, 2, RoundingMode.HALF_UP)
        fijo += f
        recortable += r
        variable += resto - r
    }
    return DivisionGastoMes(fijo.dos(), variable.dos(), recortable.dos())
}

/**
 * Frase de Cashi para el reporte de gastos:
 * - si el mes completo bajó, se celebra eso (aunque alguna categoría haya subido);
 * - si no, la categoría que más subió (al menos [CAMBIO_MINIMO_PORCENTAJE] % o nueva);
 * - si no, que va parecido.
 */
fun fraseGastos(reporte: ReporteCategorias): FraseReporte {
    if (reporte.vacio) return FraseReporte(TipoFraseReporte.SIN_DATOS)
    val subio = reporte.porciones
        .filter { it.diferencia.signum() > 0 && (it.cambioPorcentaje ?: 100) >= CAMBIO_MINIMO_PORCENTAJE }
        .filter { reporte.totalAnterior.signum() > 0 } // sin mes anterior no hay con qué comparar
        .maxByOrNull { it.diferencia }
    return when {
        reporte.totalAnterior.signum() > 0 && reporte.diferencia.signum() < 0 ->
            FraseReporte(TipoFraseReporte.GASTO_BAJO, monto = reporte.diferencia.abs())
        subio != null -> FraseReporte(TipoFraseReporte.CATEGORIA_SUBIO, nombre = subio.nombre, monto = subio.diferencia)
        else -> FraseReporte(TipoFraseReporte.GASTO_PARECIDO)
    }
}

/**
 * Frase de Cashi para el reporte de ingresos: la categoría por la que más entró de más (o una nueva),
 * junto con lo que va sobrando en el mes ([sobrante], si se conoce).
 */
fun fraseIngresos(reporte: ReporteCategorias, sobrante: BigDecimal?): FraseReporte {
    if (reporte.vacio) return FraseReporte(TipoFraseReporte.SIN_DATOS)
    val extra = reporte.porciones
        .filter { it.diferencia.signum() > 0 }
        .filter { reporte.totalAnterior.signum() > 0 }
        .maxByOrNull { it.diferencia }
    val sobra = sobrante?.takeIf { it.signum() > 0 }?.dos()
    return if (extra != null) {
        FraseReporte(TipoFraseReporte.INGRESO_EXTRA, nombre = extra.nombre, monto = extra.diferencia, sobrante = sobra)
    } else {
        FraseReporte(TipoFraseReporte.INGRESOS_PARECIDOS, monto = sobra)
    }
}

/**
 * Frase de Cashi para "Meses", sobre el último mes de [serie]:
 * - si salió más de lo que entró: [TipoFraseReporte.NO_SOBRA];
 * - si es el mejor: [TipoFraseReporte.MEJOR_MES], desde el último mes que fue igual o mejor
 *   (mesReferencia), o null si es el mejor de toda la serie;
 * - si sobra algo pero hubo un mes mejor justo antes: [TipoFraseReporte.SOBRA].
 */
fun fraseMeses(serie: List<MesResumen>): FraseReporte {
    val actual = serie.lastOrNull() ?: return FraseReporte(TipoFraseReporte.SIN_DATOS)
    val sobra = actual.sobrante.dos()
    if (sobra.signum() <= 0) return FraseReporte(TipoFraseReporte.NO_SOBRA, monto = sobra.abs())
    val anteriores = serie.dropLast(1)
    if (anteriores.isEmpty()) return FraseReporte(TipoFraseReporte.SOBRA, monto = sobra)
    val ultimoMejor = anteriores.lastOrNull { it.sobrante >= actual.sobrante }
    return when {
        ultimoMejor == null -> FraseReporte(TipoFraseReporte.MEJOR_MES, monto = sobra)
        // "el mejor desde X" solo dice algo si X no es el mes de justo antes
        ultimoMejor.mes < actual.mes.minusMonths(1) ->
            FraseReporte(TipoFraseReporte.MEJOR_MES, monto = sobra, mesReferencia = ultimoMejor.mes)
        else -> FraseReporte(TipoFraseReporte.SOBRA, monto = sobra)
    }
}
