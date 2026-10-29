package org.ucb.cashin.prediccion

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/** Qué tanto confiar en un pronóstico, según cuántos meses de historia hay. */
enum class Confianza { SIN_DATOS, BAJA, MEDIA, ALTA }

/** Resultado del modelo: un valor esperado y un rango probable. */
data class Pronostico(
    /** Valor más probable. */
    val esperado: BigDecimal,
    /** esperado − margen (nunca menor a 0). */
    val minimo: BigDecimal,
    /** esperado + margen. */
    val maximo: BigDecimal,
    val confianza: Confianza,
    /** Cuántos meses usó el modelo para calcularlo. */
    val mesesUsados: Int,
) {
    companion object {
        val SIN_DATOS = Pronostico(
            esperado = BigDecimal.ZERO.setScale(2),
            minimo = BigDecimal.ZERO.setScale(2),
            maximo = BigDecimal.ZERO.setScale(2),
            confianza = Confianza.SIN_DATOS,
            mesesUsados = 0,
        )
    }
}

/** La regresión usa como máximo los últimos 6 meses: lo reciente describe mejor el presente. */
private const val MESES_REGRESION = 6

/**
 * Pronostica el total del **siguiente mes** a partir de la serie mensual.
 *
 * Modelo según cuántos meses hay:
 * - 0 meses → SIN_DATOS.
 * - 1 mes → ese mismo valor, margen 25 %. Confianza BAJA.
 * - 2 meses → promedio, margen = |diferencia| / 2 (mínimo 15 %). Confianza BAJA.
 * - 3 o más → **regresión lineal por mínimos cuadrados** sobre los últimos hasta 6 meses,
 *   combinada 50/50 con el **promedio móvil ponderado** (el mes más reciente pesa más),
 *   para que una tendencia fuerte no se dispare. Margen = desviación estándar de los
 *   residuos de la regresión (mínimo 10 %). Confianza MEDIA con 3–5 meses, ALTA con 6 o más.
 *
 * El esperado nunca es negativo.
 */
fun pronosticarSiguienteMes(serie: List<PuntoMensual>): Pronostico {
    val valores = serie.map { it.valor }

    return when (valores.size) {
        0 -> Pronostico.SIN_DATOS

        1 -> {
            val esperado = max(valores[0], 0.0)
            armarPronostico(esperado, margen = esperado * 0.25, Confianza.BAJA, mesesUsados = 1)
        }

        2 -> {
            val esperado = max(valores.average(), 0.0)
            val margen = max(abs(valores[1] - valores[0]) / 2, esperado * 0.15)
            armarPronostico(esperado, margen, Confianza.BAJA, mesesUsados = 2)
        }

        else -> {
            val ventana = valores.takeLast(MESES_REGRESION)
            val recta = regresionLineal(ventana)
            val porRegresion = recta.predecir(ventana.size.toDouble())
            val porPromedio = promedioPonderado(ventana)
            val esperado = max(0.5 * porRegresion + 0.5 * porPromedio, 0.0)
            val margen = max(recta.desviacionResiduos(ventana), esperado * 0.10)
            val confianza = if (valores.size >= MESES_REGRESION) Confianza.ALTA else Confianza.MEDIA
            armarPronostico(esperado, margen, confianza, mesesUsados = ventana.size)
        }
    }
}

/**
 * Proyecta cómo **terminará el mes actual** según lo gastado hasta hoy.
 *
 * - Ritmo = gastado hasta hoy ÷ día del mes × días del mes.
 * - Si hay pronóstico, se mezclan: peso = día del mes ÷ días del mes;
 *   resultado = peso × ritmo + (1 − peso) × esperado.
 *   Al inicio del mes manda la historia; al final manda el ritmo real.
 * - Sin pronóstico, solo el ritmo.
 */
fun proyectarMesActual(
    gastoHastaHoy: BigDecimal,
    hoy: LocalDate,
    pronosticoDelMes: Pronostico,
): BigDecimal {
    val dia = hoy.dayOfMonth.toDouble()
    val diasDelMes = hoy.lengthOfMonth().toDouble()
    val ritmo = gastoHastaHoy.toDouble() / dia * diasDelMes

    val proyeccion = if (pronosticoDelMes.confianza == Confianza.SIN_DATOS) {
        ritmo
    } else {
        val peso = dia / diasDelMes
        peso * ritmo + (1 - peso) * pronosticoDelMes.esperado.toDouble()
    }
    return aMonto(max(proyeccion, 0.0))
}

/**
 * **Evaluación del modelo** (validación "walk-forward"): para cada mes desde el 3.º,
 * pronostica usando solo los meses anteriores y lo compara con lo que pasó de verdad.
 * Devuelve el promedio del error absoluto en Bs, o null si hay menos de 4 meses.
 *
 * Sirve para decir "el modelo se equivoca en promedio ±Bs X".
 */
fun errorMedioAbsoluto(serie: List<PuntoMensual>): Double? {
    if (serie.size < 4) return null
    val errores = (2 until serie.size).map { k ->
        val pronostico = pronosticarSiguienteMes(serie.take(k))
        abs(pronostico.esperado.toDouble() - serie[k].valor)
    }
    return errores.average()
}

// ------------------------------------------------------------------ Piezas del modelo

/** Recta y = pendiente·x + intercepto, con x = 0, 1, …, n−1. */
private data class Recta(val pendiente: Double, val intercepto: Double) {
    fun predecir(x: Double): Double = pendiente * x + intercepto

    /** Desviación estándar de los residuos (lo que la recta no explica). */
    fun desviacionResiduos(valores: List<Double>): Double {
        val sumaCuadrados = valores.indices.sumOf { i ->
            val residuo = valores[i] - predecir(i.toDouble())
            residuo * residuo
        }
        return sqrt(sumaCuadrados / valores.size)
    }
}

/** Mínimos cuadrados: la recta que pasa más cerca de todos los puntos. */
private fun regresionLineal(valores: List<Double>): Recta {
    val n = valores.size.toDouble()
    val promedioX = (n - 1) / 2
    val promedioY = valores.average()

    var numerador = 0.0
    var denominador = 0.0
    valores.forEachIndexed { i, y ->
        val dx = i - promedioX
        numerador += dx * (y - promedioY)
        denominador += dx * dx
    }
    val pendiente = if (denominador == 0.0) 0.0 else numerador / denominador
    return Recta(pendiente = pendiente, intercepto = promedioY - pendiente * promedioX)
}

/** Promedio con pesos 1, 2, …, n: el mes más reciente pesa más. */
private fun promedioPonderado(valores: List<Double>): Double {
    val sumaPesos = valores.indices.sumOf { (it + 1).toDouble() }
    return valores.indices.sumOf { (it + 1) * valores[it] } / sumaPesos
}

private fun armarPronostico(esperado: Double, margen: Double, confianza: Confianza, mesesUsados: Int) =
    Pronostico(
        esperado = aMonto(esperado),
        minimo = aMonto(max(esperado - margen, 0.0)),
        maximo = aMonto(esperado + margen),
        confianza = confianza,
        mesesUsados = mesesUsados,
    )

/** Double del modelo → BigDecimal de 2 decimales para la app. */
internal fun aMonto(valor: Double): BigDecimal = BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP)