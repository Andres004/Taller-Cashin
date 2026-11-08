package org.ucb.cashin.prediccion

import org.ucb.cashin.model.Desafio
import org.ucb.cashin.model.GastoCategoriaMes
import org.ucb.cashin.model.PeriodoDesafio
import org.ucb.cashin.model.ReglaDesafio
import org.ucb.cashin.model.TotalesMes
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.floor
import kotlin.math.max
import kotlin.random.Random

/*
 * HU-22: los desafíos llegan solos. Cada lunes se sortean 3 semanales de un catálogo de 12 y cada
 * día 1 se sortea 1 mensual de un catálogo de 5. El sorteo usa una semilla propia de cada cuenta y
 * periodo, así cada persona tiene sus propios retos; el servidor los guarda una sola vez por periodo.
 * Solo entran los retos que tienen sentido para esa persona (realistas) y que todavía se pueden ganar.
 */

/** Cuántos desafíos se asignan en cada periodo. */
fun cantidadPorPeriodo(periodo: PeriodoDesafio): Int = if (periodo == PeriodoDesafio.SEMANAL) 3 else 1

/** Límite mínimo realista: nadie gasta "como máximo Bs 5" a la semana en comida. */
fun limiteMinimo(periodo: PeriodoDesafio): BigDecimal =
    if (periodo == PeriodoDesafio.SEMANAL) BigDecimal("50.00") else BigDecimal("200.00")

/**
 * Catálogo de retos (12 semanales y 5 mensuales). Para ampliarlo basta con agregar una entrada aquí,
 * su título y descripción en strings.xml y, si la regla es nueva, su medición en el servidor.
 * - [puestoCategoria]: 1 = la categoría donde más gastas, 2 = la segunda…
 * - [recorte]: qué parte de lo de costumbre se permite gastar (0,85 = 15 % menos).
 * - [objetivo]: días, movimientos o ahorros que hay que lograr.
 */
enum class PlantillaDesafio(
    val periodo: PeriodoDesafio,
    val regla: ReglaDesafio,
    val puestoCategoria: Int = 0,
    val recorte: Double = 0.0,
    val objetivo: Int = 0,
    val dificil: Boolean = false,
) {
    // Semanales (12)
    S_CATEGORIA_1(PeriodoDesafio.SEMANAL, ReglaDesafio.LIMITE_CATEGORIA, puestoCategoria = 1, recorte = 0.85),
    S_CATEGORIA_2(PeriodoDesafio.SEMANAL, ReglaDesafio.LIMITE_CATEGORIA, puestoCategoria = 2, recorte = 0.85),
    S_CATEGORIA_3(PeriodoDesafio.SEMANAL, ReglaDesafio.LIMITE_CATEGORIA, puestoCategoria = 3, recorte = 0.85),
    S_CATEGORIA_1_DURO(PeriodoDesafio.SEMANAL, ReglaDesafio.LIMITE_CATEGORIA, puestoCategoria = 1, recorte = 0.70, dificil = true),
    S_TOTAL(PeriodoDesafio.SEMANAL, ReglaDesafio.LIMITE_TOTAL, recorte = 0.90),
    S_TOTAL_DURO(PeriodoDesafio.SEMANAL, ReglaDesafio.LIMITE_TOTAL, recorte = 0.80, dificil = true),
    S_DIAS_3(PeriodoDesafio.SEMANAL, ReglaDesafio.DIAS_REGISTRO, objetivo = 3),
    S_DIAS_5(PeriodoDesafio.SEMANAL, ReglaDesafio.DIAS_REGISTRO, objetivo = 5),
    S_DIAS_7(PeriodoDesafio.SEMANAL, ReglaDesafio.DIAS_REGISTRO, objetivo = 7, dificil = true),
    S_MOVIMIENTOS_10(PeriodoDesafio.SEMANAL, ReglaDesafio.CANTIDAD_REGISTROS, objetivo = 10),
    S_SIN_GASTO_2(PeriodoDesafio.SEMANAL, ReglaDesafio.DIAS_SIN_GASTO, objetivo = 2),
    S_AHORRO_1(PeriodoDesafio.SEMANAL, ReglaDesafio.AHORRO_META, objetivo = 1),

    // Mensuales (5)
    M_CATEGORIA_1(PeriodoDesafio.MENSUAL, ReglaDesafio.LIMITE_CATEGORIA, puestoCategoria = 1, recorte = 0.85),
    M_TOTAL(PeriodoDesafio.MENSUAL, ReglaDesafio.LIMITE_TOTAL, recorte = 0.90),
    M_DIAS_20(PeriodoDesafio.MENSUAL, ReglaDesafio.DIAS_REGISTRO, objetivo = 20),
    M_SIN_GASTO_8(PeriodoDesafio.MENSUAL, ReglaDesafio.DIAS_SIN_GASTO, objetivo = 8),
    M_AHORRO_3(PeriodoDesafio.MENSUAL, ReglaDesafio.AHORRO_META, objetivo = 3),
}

/** Un desafío elegido para esta persona, listo para guardarse. El título lo arma la app con strings.xml. */
data class PropuestaDesafio(
    val periodo: PeriodoDesafio,
    val regla: ReglaDesafio,
    val categoriaId: String? = null,
    val categoriaNombre: String? = null,
    val montoLimite: BigDecimal? = null,
    /** Objetivo de las reglas que cuentan algo (días, movimientos o ahorros). */
    val diasObjetivo: Int? = null,
    /** Promedio o pronóstico que se usó (para explicarlo en la descripción). */
    val basadoEn: BigDecimal? = null,
    val plantilla: PlantillaDesafio? = null,
)

/** Lo que la persona ya hizo en el periodo en curso (desde el lunes o el día 1 hasta hoy). */
data class MedicionPeriodo(
    /** Gasto del periodo por id de categoría. */
    val gastoPorCategoria: Map<String, BigDecimal> = emptyMap(),
    val gastoTotal: BigDecimal = BigDecimal.ZERO,
    /** Días distintos con al menos una anotación. */
    val diasConRegistro: Int = 0,
    val cantidadRegistros: Int = 0,
    /** Días ya terminados del periodo sin ningún gasto. */
    val diasSinGasto: Int = 0,
    /** Ahorros anotados en metas. */
    val ahorros: Int = 0,
)

/** Historia que usa el sorteo para decidir qué retos son realistas para esta persona. */
data class HistoriaDesafios(
    val gastoPorCategoria: List<GastoCategoriaMes>,
    val totales: List<TotalesMes>,
    /** Tiene al menos una meta activa (si no, "ahorra en tu meta" no tiene sentido). */
    val tieneMetasActivas: Boolean,
    /** Con él, los límites se calculan solo sobre lo recortable; null usa la regla anterior. */
    val desglose: DesgloseGastos? = null,
)

/** Semilla propia de la cuenta y el periodo: la misma cuenta en la misma semana sortea lo mismo. */
fun semillaDe(usuarioId: String, periodo: PeriodoDesafio, inicio: LocalDate): Long =
    "$usuarioId|$periodo|$inicio".hashCode().toLong()

/**
 * Sortea los desafíos del [periodo] para esta persona:
 * 1. Arma cada reto del catálogo que sea realista y todavía se pueda ganar ([armarPropuesta]).
 * 2. Los mezcla con [semilla] (Random con semilla: mismo resultado para la misma cuenta y periodo).
 * 3. Toma los primeros sin repetir un reto (misma regla y categoría), ni entre ellos ni con [evitar]
 *    (lo que ya está activo, por ejemplo el mensual al sortear los semanales).
 * Devuelve hasta [cantidadPorPeriodo] retos.
 */
fun sortearDesafios(
    periodo: PeriodoDesafio,
    semilla: Long,
    hoy: LocalDate,
    historia: HistoriaDesafios,
    medicion: MedicionPeriodo,
    evitar: List<Desafio> = emptyList(),
): List<PropuestaDesafio> {
    val candidatos = PlantillaDesafio.entries
        .filter { it.periodo == periodo }
        .mapNotNull { armarPropuesta(it, hoy, historia, medicion) }
        .shuffled(Random(semilla))

    val elegidos = mutableListOf<PropuestaDesafio>()
    for (candidato in candidatos) {
        if (elegidos.size == cantidadPorPeriodo(periodo)) break
        val repetido = elegidos.any { it.mismoRetoQue(candidato) } || evitar.any { candidato.esMismoRetoQue(it) }
        if (!repetido) elegidos += candidato
    }
    return elegidos
}

/**
 * Convierte una plantilla en un reto concreto para esta persona, o null si no tiene sentido:
 * - Límites: solo si suele gastar al menos el mínimo del periodo (Bs 50 semanal, Bs 200 mensual) y,
 *   en categorías, si gasta en ella con regularidad (en 2 de los últimos 3 meses). El límite es lo de
 *   costumbre × recorte, redondeado hacia abajo a Bs 5 y nunca menor al mínimo.
 * - En todos: no se propone si ya está perdido (gastó el 80 % del límite) ni si ya está logrado,
 *   ni si con los días que quedan ya no se puede lograr.
 */
fun armarPropuesta(
    plantilla: PlantillaDesafio,
    hoy: LocalDate,
    historia: HistoriaDesafios,
    medicion: MedicionPeriodo,
): PropuestaDesafio? {
    val periodo = plantilla.periodo
    val factor = factorPeriodo(periodo)
    val minimo = limiteMinimo(periodo)
    val diasQueQuedan = finDelPeriodo(periodo, hoy).toEpochDay() - hoy.toEpochDay() + 1

    return when (plantilla.regla) {
        ReglaDesafio.LIMITE_CATEGORIA -> {
            val categoria = categoriasParaLimite(historia, hoy).getOrNull(plantilla.puestoCategoria - 1) ?: return null
            val deCostumbre = categoria.promedio * factor
            if (deCostumbre < minimo.toDouble()) return null
            val limite = limiteRealista(deCostumbre * plantilla.recorte, minimo)
            val gastado = medicion.gastoPorCategoria[categoria.categoriaId] ?: BigDecimal.ZERO
            if (yaPerdido(gastado, limite)) return null
            PropuestaDesafio(
                periodo, plantilla.regla, categoria.categoriaId, categoria.nombre,
                montoLimite = limite, basadoEn = aMonto(deCostumbre), plantilla = plantilla,
            )
        }

        ReglaDesafio.LIMITE_TOTAL -> {
            val pronostico = pronosticarSiguienteMes(construirSerie(historia.totales, hoy) { it.totalGastos })
            if (pronostico.confianza == Confianza.SIN_DATOS) return null
            val medible = historia.desglose?.let { (pronostico.esperado - it.fueraDeRetos).max(BigDecimal.ZERO) } ?: pronostico.esperado
            val deCostumbre = medible.toDouble() * factor
            if (deCostumbre < minimo.toDouble()) return null
            val limite = limiteRealista(deCostumbre * plantilla.recorte, minimo)
            if (yaPerdido(medicion.gastoTotal, limite)) return null
            PropuestaDesafio(
                periodo, plantilla.regla, montoLimite = limite, basadoEn = aMonto(deCostumbre), plantilla = plantilla,
            )
        }

        ReglaDesafio.DIAS_REGISTRO -> contable(plantilla, medicion.diasConRegistro, posible = medicion.diasConRegistro + diasQueQuedan)

        // Movimientos: siempre se puede (se pueden anotar varios en un día), si queda tiempo.
        ReglaDesafio.CANTIDAD_REGISTROS -> contable(plantilla, medicion.cantidadRegistros, posible = Long.MAX_VALUE)

        // Hoy todavía no terminó: cuenta como un día que se puede lograr.
        ReglaDesafio.DIAS_SIN_GASTO -> contable(plantilla, medicion.diasSinGasto, posible = medicion.diasSinGasto + diasQueQuedan)

        ReglaDesafio.AHORRO_META ->
            if (!historia.tieneMetasActivas) null
            else contable(plantilla, medicion.ahorros, posible = Long.MAX_VALUE)
    }
}

/**
 * Categorías regulares, de mayor a menor gasto. Con desglose: solo las de importancia 1 a 3,
 * y su gasto es únicamente la parte variable.
 */
private fun categoriasParaLimite(historia: HistoriaDesafios, hoy: LocalDate): List<PromedioCategoria> {
    val regulares = promediosPorCategoria(historia.gastoPorCategoria, hoy).filter { it.esRegular }
    val desglose = historia.desglose?.porCategoria?.associateBy { it.categoriaId }
    val medibles = if (desglose == null) {
        regulares
    } else {
        regulares.mapNotNull { promedio ->
            desglose[promedio.categoriaId]
                ?.takeIf { it.importancia < IMPORTANCIA_SIN_RETOS }
                ?.let { promedio.copy(promedio = it.variable.toDouble()) }
        }
    }
    return medibles.sortedByDescending { it.promedio }
}

/** Retos que cuentan algo: no se proponen si ya están logrados o si ya no alcanza el tiempo. */
private fun contable(plantilla: PlantillaDesafio, logrado: Int, posible: Long): PropuestaDesafio? =
    if (logrado >= plantilla.objetivo || posible < plantilla.objetivo) null
    else PropuestaDesafio(plantilla.periodo, plantilla.regla, diasObjetivo = plantilla.objetivo, plantilla = plantilla)

/** Desde el 80 % del límite ya gastado, aceptar el reto sería perderlo apenas empieza. */
private fun yaPerdido(gastado: BigDecimal, limite: BigDecimal): Boolean =
    gastado.toDouble() >= limite.toDouble() * UMBRAL_YA_PERDIDO

private const val UMBRAL_YA_PERDIDO = 0.80

/** Redondea hacia abajo a múltiplos de Bs 5, sin bajar del mínimo realista del periodo. */
private fun limiteRealista(valor: Double, minimo: BigDecimal): BigDecimal =
    aMonto(max(floor(valor / MULTIPLO_LIMITE) * MULTIPLO_LIMITE, minimo.toDouble()))

private const val MULTIPLO_LIMITE = 5.0

/**
 * Es "el mismo reto" que un desafío ya activo: misma regla y, si es de categoría, la misma categoría.
 * Así no se puede tener "Alimentación bajo control" dos veces a la vez.
 */
fun PropuestaDesafio.esMismoRetoQue(desafio: Desafio): Boolean =
    regla == desafio.regla && (regla != ReglaDesafio.LIMITE_CATEGORIA || categoriaId == desafio.categoriaId)

private fun PropuestaDesafio.mismoRetoQue(otra: PropuestaDesafio): Boolean =
    regla == otra.regla && (regla != ReglaDesafio.LIMITE_CATEGORIA || categoriaId == otra.categoriaId)

/** Qué parte de un mes es el periodo: una semana ≈ 7 / 30,44 de un mes promedio. */
internal fun factorPeriodo(periodo: PeriodoDesafio): Double = when (periodo) {
    PeriodoDesafio.SEMANAL -> 7.0 / 30.44
    PeriodoDesafio.MENSUAL -> 1.0
}

/**
 * Primer día del periodo en curso, igual que el servidor (fn_rango_periodo):
 * SEMANAL = el lunes de esta semana · MENSUAL = el día 1 del mes.
 */
fun inicioDelPeriodo(periodo: PeriodoDesafio, hoy: LocalDate): LocalDate = when (periodo) {
    PeriodoDesafio.SEMANAL -> hoy.with(DayOfWeek.MONDAY)
    PeriodoDesafio.MENSUAL -> hoy.withDayOfMonth(1)
}

/** Último día del periodo en curso: el domingo o el último día del mes. */
fun finDelPeriodo(periodo: PeriodoDesafio, hoy: LocalDate): LocalDate = when (periodo) {
    PeriodoDesafio.SEMANAL -> hoy.with(DayOfWeek.SUNDAY)
    PeriodoDesafio.MENSUAL -> hoy.withDayOfMonth(hoy.lengthOfMonth())
}

/** Para comparar con el promedio se miran como máximo los últimos 3 meses completos. */
internal const val MESES_PROMEDIO_CATEGORIA = 3

/** Gasto promedio mensual de una categoría en los últimos meses completos. */
internal data class PromedioCategoria(
    val categoriaId: String,
    val nombre: String,
    val promedio: Double,
    /** En cuántos meses de la ventana hubo gasto en esta categoría. */
    val mesesConGasto: Int,
    val mesesVentana: Int,
) {
    /** Gasta en ella casi siempre (en 2 de 3 meses, o en todos si hay menos): no es un gasto ocasional. */
    val esRegular: Boolean get() = mesesConGasto >= minOf(2, mesesVentana)
}

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
                mesesConGasto = filas.filter { it.total.signum() > 0 }.map { YearMonth.from(it.mes) }.distinct().size,
                mesesVentana = mesesVentana,
            )
        }
}
