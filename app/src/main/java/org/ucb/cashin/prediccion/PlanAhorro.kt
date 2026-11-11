package org.ucb.cashin.prediccion

import org.ucb.cashin.model.AporteMeta
import org.ucb.cashin.model.AvanceMeta
import org.ucb.cashin.model.TipoAporte
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class EstadoPlan {
    /** Le basta lo que le queda libre. */
    ALCANZA,

    /** Necesita usar parte de lo que puede recortar. */
    CON_RECORTE,

    /** Ni con el máximo posible llega a su fecha límite: el camino usa la fecha real. */
    NO_LLEGA_A_FECHA,

    /** Sus gastos ocupan casi todo: pasos simbólicos y sin prometer fecha. */
    SIN_MARGEN,

    /** No hay ingresos o gastos suficientes para calcular: no se piden montos. */
    SIN_DATOS,

    LOGRADA,
}

enum class EstadoPaso { COMPLETADO, ACTUAL, PENDIENTE }

enum class CadenciaPlan { SEMANAL, QUINCENAL, MENSUAL }

data class PasoAhorro(
    val numero: Int,
    val fecha: LocalDate,
    /** En un paso hecho, lo ahorrado; en el actual, lo que falta; en un pendiente, lo que tocará. */
    val monto: BigDecimal,
    val acumulado: BigDecimal,
    val estado: EstadoPaso,
    /** Es el último paso: con él se llega a la meta. */
    val esMeta: Boolean,
)

data class PlanAhorro(
    val estado: EstadoPlan,
    val cadencia: CadenciaPlan?,
    /** Con cadencia mensual, cada cuántos meses es un paso (metas muy largas). */
    val mesesPorPaso: Int,
    val ahorroPorPaso: BigDecimal,
    val pasos: List<PasoAhorro>,
    /** Cuándo se llegaría; null si no se puede prometer una fecha. */
    val fechaEstimada: LocalDate?,
) {
    /** El próximo paso por cumplir; null si ya no queda ninguno. */
    val pasoActual: PasoAhorro?
        get() = pasos.firstOrNull { it.estado != EstadoPaso.COMPLETADO }
}

private val CERO: BigDecimal = BigDecimal.ZERO.setScale(2)
private val DIAS_POR_MES = BigDecimal("30.44")
private val MULTIPLO = BigDecimal.valueOf(5)
private val PARTE_SIN_FECHA = BigDecimal("0.75")
private val MINIMO_CON_MARGEN = BigDecimal.valueOf(50)
private val PARTE_DEL_INGRESO_SIMBOLICA = BigDecimal("0.05")
private val SIMBOLICO_MINIMO = BigDecimal.valueOf(10)
private val MINIMO_POR_PASO = BigDecimal.valueOf(10)
private const val MAX_PASOS_HECHOS = 3
private const val MAX_PASOS_PENDIENTES = 9
private const val MIN_PASOS_PENDIENTES = 3
private const val PASOS_SIMBOLICOS = 3
private const val DIAS_PARA_MENSUAL = 90L
private const val DIAS_PARA_QUINCENAL = 21L
private const val DIAS_PARA_UNIR_ULTIMO_PASO = 7L

/** Ritmo y calendario elegidos para los pasos que faltan. */
private data class Calendario(
    val estado: EstadoPlan,
    val cadencia: CadenciaPlan,
    val mesesPorPaso: Int,
    val paso: BigDecimal,
    val cortes: List<LocalDate>,
)

/**
 * Arma el camino hasta la meta: hasta 3 pasos ya hechos (ahorros reales) y hasta 9 por delante.
 *
 * El ahorro nunca supera lo que los ingresos permiten: el máximo posible es
 * ingresos esperados − gastos + lo recortable, y cada paso es la parte proporcional de ese máximo
 * según lo que dura. Sin fecha límite propone el 75 % del máximo (o el ritmo real, sin pasarse);
 * con fecha, lo necesario para llegar. Lo que se ahorra de más o de menos reajusta los pasos que faltan.
 */
fun planificarAhorro(
    avance: AvanceMeta,
    aportes: List<AporteMeta>,
    pronosticoIngresos: Pronostico,
    pronosticoGastos: Pronostico,
    desglose: DesgloseGastos?,
    hoy: LocalDate,
): PlanAhorro {
    val faltante = (avance.montoObjetivo - avance.totalAhorrado).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
    if (faltante.signum() == 0) {
        val hechos = pasosHechos(aportes, hoy.plusDays(1))
        return PlanAhorro(EstadoPlan.LOGRADA, null, 1, CERO, marcarMeta(hechos), hoy)
    }

    val ingresos = pronosticoIngresos.esperado
    val sinDatos = pronosticoIngresos.confianza == Confianza.SIN_DATOS ||
        pronosticoGastos.confianza == Confianza.SIN_DATOS ||
        ingresos.signum() <= 0
    if (sinDatos) {
        return PlanAhorro(EstadoPlan.SIN_DATOS, null, 1, CERO, pasosHechos(aportes, hoy.plusDays(1)), null)
    }

    val capacidad = capacidadDeAhorro(ingresos, pronosticoGastos.esperado, desglose)
    val maximo = (capacidad + (desglose?.margenRecortable ?: BigDecimal.ZERO)).max(BigDecimal.ZERO)
    if (maximo < MINIMO_CON_MARGEN) {
        return planSimbolico(aportes, avance, faltante, ingresos, hoy)
    }

    val ahorradoAhora = ahorradoDesde(aportes, inicioDelPeriodo(cadenciaProvisional(avance, faltante, maximo, hoy), hoy), hoy)
    val base = faltante + ahorradoAhora
    val teniaFecha = avance.fechaLimite != null
    val ritmo = if (teniaFecha) maximo else ritmoSinFecha(aportes, maximo)
    val calendario = conFechaLimite(avance.fechaLimite, base, capacidad, maximo, hoy)
        ?: porRitmo(base, ritmo, capacidad, teniaFecha, hoy)

    val ahorradoEnPeriodo = ahorradoDesde(aportes, inicioDelPeriodo(calendario.cadencia, hoy), hoy)
    val hechos = pasosHechos(aportes, inicioDelPeriodo(calendario.cadencia, hoy))

    // Ya cumplió (o pasó) el paso de este periodo: queda hecho con todo lo que ahorró,
    // y lo que falta se reparte de nuevo desde el periodo siguiente.
    val cumplioElPeriodo = ahorradoEnPeriodo.signum() > 0 && ahorradoEnPeriodo >= calendario.paso && calendario.cortes.size > 1
    if (cumplioElPeriodo) {
        val corteActual = calendario.cortes.first()
        val desde = corteActual.plusDays(1)
        val nuevo = conFechaLimite(avance.fechaLimite, faltante, capacidad, maximo, desde)
            ?: porRitmo(faltante, ritmo, capacidad, teniaFecha, desde)
        val acumulado = avance.totalAhorrado.setScale(2, RoundingMode.HALF_UP)
        val hecho = PasoAhorro(hechos.size + 1, corteActual, ahorradoEnPeriodo, acumulado, EstadoPaso.COMPLETADO, esMeta = false)
        val pasos = armarPasos(hechos + hecho, nuevo, faltante, CERO, avance.totalAhorrado)
        return PlanAhorro(nuevo.estado, nuevo.cadencia, nuevo.mesesPorPaso, nuevo.paso, pasos, pasos.lastOrNull()?.fecha)
    }

    val pasos = armarPasos(hechos, calendario, faltante, ahorradoEnPeriodo, avance.totalAhorrado)
    return PlanAhorro(
        estado = calendario.estado,
        cadencia = calendario.cadencia,
        mesesPorPaso = calendario.mesesPorPaso,
        ahorroPorPaso = calendario.paso,
        pasos = pasos,
        fechaEstimada = pasos.lastOrNull()?.fecha,
    )
}

/** Ritmo sin fecha límite: el 75 % del máximo o el ritmo real, sin pasar nunca del máximo. */
private fun ritmoSinFecha(aportes: List<AporteMeta>, maximo: BigDecimal): BigDecimal {
    val real = ritmoPorAportes(aportes)?.first ?: BigDecimal.ZERO
    return (maximo * PARTE_SIN_FECHA).max(real).min(maximo)
}

/** Solo para saber desde cuándo descontar lo ya ahorrado antes de calcular el calendario definitivo. */
private fun cadenciaProvisional(avance: AvanceMeta, faltante: BigDecimal, maximo: BigDecimal, hoy: LocalDate): CadenciaPlan {
    val fecha = avance.fechaLimite
    val dias = if (fecha != null && fecha.isAfter(hoy)) ChronoUnit.DAYS.between(hoy, fecha) else diasHasta(faltante, maximo)
    return cadenciaPara(dias)
}

private fun cadenciaPara(dias: Long): CadenciaPlan = when {
    dias >= DIAS_PARA_MENSUAL -> CadenciaPlan.MENSUAL
    dias >= DIAS_PARA_QUINCENAL -> CadenciaPlan.QUINCENAL
    else -> CadenciaPlan.SEMANAL
}

private fun diasHasta(monto: BigDecimal, ritmoMensual: BigDecimal): Long =
    (monto * DIAS_POR_MES).divide(ritmoMensual, 0, RoundingMode.CEILING).toLong()

/** Null si no tiene fecha, si ya pasó o si no alcanza: entonces se calcula por ritmo. */
private fun conFechaLimite(
    fecha: LocalDate?,
    base: BigDecimal,
    capacidad: BigDecimal,
    maximo: BigDecimal,
    hoy: LocalDate,
): Calendario? {
    if (fecha == null || !fecha.isAfter(hoy)) return null

    val cadencia = cadenciaPara(ChronoUnit.DAYS.between(hoy, fecha))
    val meses = mesesPorPasoHasta(cadencia, fecha, hoy)
    val cortes = cortesHasta(cadencia, meses, fecha, hoy)
    val necesario = base.divide(BigDecimal.valueOf(cortes.size.toLong()), 2, RoundingMode.CEILING)

    if (necesario > montoDelPeriodo(maximo, cadencia, meses)) return null

    val paso = aMultiplo(necesario).max(MULTIPLO)
    val estado = if (necesario <= montoDelPeriodo(capacidad.max(BigDecimal.ZERO), cadencia, meses)) EstadoPlan.ALCANZA else EstadoPlan.CON_RECORTE
    return Calendario(estado, cadencia, meses, paso, cortes)
}

private fun mesesPorPasoHasta(cadencia: CadenciaPlan, fecha: LocalDate, hoy: LocalDate): Int {
    if (cadencia != CadenciaPlan.MENSUAL) return 1
    val meses = ChronoUnit.MONTHS.between(YearMonth.from(hoy), YearMonth.from(fecha)).toInt() + 1
    return Math.ceilDiv(meses, MAX_PASOS_PENDIENTES).coerceAtLeast(1)
}

/** Vencimientos hasta la fecha límite; el último nunca la pasa y un tramo final de pocos días se une al anterior. */
private fun cortesHasta(cadencia: CadenciaPlan, meses: Int, fecha: LocalDate, hoy: LocalDate): List<LocalDate> {
    val cortes = mutableListOf<LocalDate>()
    var corte = primerCorte(cadencia, meses, hoy)
    while (corte < fecha) {
        cortes += corte
        corte = siguienteCorte(cadencia, meses, corte)
    }
    if (cortes.isNotEmpty() && ChronoUnit.DAYS.between(cortes.last(), fecha) < DIAS_PARA_UNIR_ULTIMO_PASO) {
        cortes.removeAt(cortes.lastIndex)
    }
    cortes += fecha
    return cortes
}

private fun porRitmo(
    base: BigDecimal,
    ritmo: BigDecimal,
    capacidad: BigDecimal,
    teniaFecha: Boolean,
    hoy: LocalDate,
): Calendario {
    val cadencia = cadenciaPara(diasHasta(base, ritmo))
    var meses = 1
    var paso = aMultiplo(montoDelPeriodo(ritmo, cadencia, meses)).max(MULTIPLO)
    var cantidad = cantidadDePasos(base, paso)
    while (cantidad > MAX_PASOS_PENDIENTES) {
        meses += 1
        paso = aMultiplo(montoDelPeriodo(ritmo, cadencia, meses))
        cantidad = cantidadDePasos(base, paso)
    }

    val porTercio = base.divide(BigDecimal.valueOf(MIN_PASOS_PENDIENTES.toLong()), 2, RoundingMode.DOWN)
    if (cantidad < MIN_PASOS_PENDIENTES) {
        if (porTercio >= MINIMO_POR_PASO) {
            paso = aMultiplo(porTercio).max(MULTIPLO)
            cantidad = MIN_PASOS_PENDIENTES
        } else {
            paso = base
            cantidad = 1
        }
    }

    val estado = when {
        teniaFecha -> EstadoPlan.NO_LLEGA_A_FECHA
        ritmo <= capacidad -> EstadoPlan.ALCANZA
        else -> EstadoPlan.CON_RECORTE
    }
    val cortes = generateSequence(primerCorte(cadencia, meses, hoy)) { siguienteCorte(cadencia, meses, it) }
        .take(cantidad)
        .toList()
    return Calendario(estado, cadencia, meses, paso, cortes)
}

private fun cantidadDePasos(base: BigDecimal, paso: BigDecimal): Int =
    base.divide(paso, 0, RoundingMode.CEILING).toInt()

/** La parte del ahorro mensual que corresponde a un paso: un paso corto pide proporcionalmente menos. */
private fun montoDelPeriodo(mensual: BigDecimal, cadencia: CadenciaPlan, meses: Int): BigDecimal = when (cadencia) {
    CadenciaPlan.SEMANAL -> (mensual * BigDecimal.valueOf(7)).divide(DIAS_POR_MES, 2, RoundingMode.DOWN)
    CadenciaPlan.QUINCENAL -> (mensual * BigDecimal.valueOf(15)).divide(DIAS_POR_MES, 2, RoundingMode.DOWN)
    CadenciaPlan.MENSUAL -> (mensual * BigDecimal.valueOf(meses.toLong())).setScale(2, RoundingMode.DOWN)
}

private fun aMultiplo(valor: BigDecimal): BigDecimal =
    valor.divide(MULTIPLO, 0, RoundingMode.DOWN).multiply(MULTIPLO).setScale(2)

private fun inicioDelPeriodo(cadencia: CadenciaPlan, hoy: LocalDate): LocalDate = when (cadencia) {
    CadenciaPlan.SEMANAL -> hoy.with(DayOfWeek.MONDAY)
    CadenciaPlan.QUINCENAL -> hoy.withDayOfMonth(if (hoy.dayOfMonth <= 15) 1 else 16)
    CadenciaPlan.MENSUAL -> hoy.withDayOfMonth(1)
}

private fun finDeMes(mes: YearMonth): LocalDate = mes.atEndOfMonth()

private fun primerCorte(cadencia: CadenciaPlan, meses: Int, hoy: LocalDate): LocalDate = when (cadencia) {
    CadenciaPlan.SEMANAL -> hoy.with(DayOfWeek.SUNDAY)
    CadenciaPlan.QUINCENAL -> if (hoy.dayOfMonth <= 15) hoy.withDayOfMonth(15) else finDeMes(YearMonth.from(hoy))
    CadenciaPlan.MENSUAL -> finDeMes(YearMonth.from(hoy).plusMonths(meses - 1L))
}

private fun siguienteCorte(cadencia: CadenciaPlan, meses: Int, corte: LocalDate): LocalDate = when (cadencia) {
    CadenciaPlan.SEMANAL -> corte.plusWeeks(1)
    CadenciaPlan.QUINCENAL ->
        if (corte.dayOfMonth == 15) finDeMes(YearMonth.from(corte)) else corte.plusMonths(1).withDayOfMonth(15)
    CadenciaPlan.MENSUAL -> finDeMes(YearMonth.from(corte).plusMonths(meses.toLong()))
}

/** Ahorros menos retiros desde [desde]; nunca negativo. */
private fun ahorradoDesde(aportes: List<AporteMeta>, desde: LocalDate, hoy: LocalDate): BigDecimal =
    aportes
        .filter { it.fecha >= desde && it.fecha <= hoy }
        .fold(CERO) { suma, a -> suma + a.neto() }
        .max(CERO)

private fun AporteMeta.neto(): BigDecimal = if (tipo == TipoAporte.AHORRO) monto else monto.negate()

/** Los ahorros reales antes de [antesDe], un paso por mes con saldo a favor. Muestra los últimos 3. */
private fun pasosHechos(aportes: List<AporteMeta>, antesDe: LocalDate): List<PasoAhorro> {
    var acumulado = CERO
    val hechos = aportes
        .filter { it.fecha < antesDe }
        .groupBy { YearMonth.from(it.fecha) }
        .toSortedMap()
        .mapNotNull { (_, delMes) ->
            val neto = delMes.fold(CERO) { suma, a -> suma + a.neto() }
            acumulado += neto
            if (neto.signum() > 0) delMes.maxOf { it.fecha } to neto to acumulado else null
        }
        .takeLast(MAX_PASOS_HECHOS)
    return hechos.mapIndexed { i, (fechaYNeto, total) ->
        PasoAhorro(i + 1, fechaYNeto.first, fechaYNeto.second, total, EstadoPaso.COMPLETADO, esMeta = false)
    }
}

private fun marcarMeta(pasos: List<PasoAhorro>): List<PasoAhorro> =
    pasos.mapIndexed { i, paso -> if (i == pasos.lastIndex) paso.copy(esMeta = true) else paso }

private fun armarPasos(
    hechos: List<PasoAhorro>,
    calendario: Calendario,
    faltante: BigDecimal,
    ahorradoEnPeriodo: BigDecimal,
    totalAhorrado: BigDecimal,
): List<PasoAhorro> {
    val pasos = hechos.toMutableList()
    var restante = faltante
    var acumulado = totalAhorrado.setScale(2, RoundingMode.HALF_UP)
    val cortes = calendario.cortes
    val pendienteDelActual = (calendario.paso - ahorradoEnPeriodo).max(CERO)

    cortes.forEachIndexed { i, corte ->
        val esUltimo = i == cortes.lastIndex
        val monto = when {
            esUltimo -> restante
            i == 0 -> pendienteDelActual.min(restante)
            else -> calendario.paso.min(restante)
        }
        if (monto.signum() > 0 || esUltimo) {
            restante -= monto
            acumulado += monto
            pasos += PasoAhorro(
                numero = pasos.size + 1,
                fecha = corte,
                monto = monto,
                acumulado = acumulado,
                estado = if (i == 0) EstadoPaso.ACTUAL else EstadoPaso.PENDIENTE,
                esMeta = esUltimo,
            )
        }
    }
    // Nunca un paso de Bs 0: si lo que falta se cubrió antes, la meta es el último paso con monto.
    val ultimo = pasos.lastOrNull()
    if (ultimo != null && ultimo.estado != EstadoPaso.COMPLETADO && ultimo.monto.signum() == 0) {
        pasos.removeAt(pasos.lastIndex)
        val previo = pasos.lastOrNull()
        if (previo != null && previo.estado != EstadoPaso.COMPLETADO) pasos[pasos.lastIndex] = previo.copy(esMeta = true)
    }
    return pasos
}

/** Sin margen: los próximos 3 meses con un monto simbólico y sin prometer cuándo se llega. */
private fun planSimbolico(
    aportes: List<AporteMeta>,
    avance: AvanceMeta,
    faltante: BigDecimal,
    ingresos: BigDecimal,
    hoy: LocalDate,
): PlanAhorro {
    val paso = aMultiplo((ingresos * PARTE_DEL_INGRESO_SIMBOLICA).min(MINIMO_CON_MARGEN).max(SIMBOLICO_MINIMO)).min(faltante)
    val hechos = pasosHechos(aportes, hoy.withDayOfMonth(1))
    val ahorrado = ahorradoDesde(aportes, hoy.withDayOfMonth(1), hoy)

    var restante = faltante
    var acumulado = avance.totalAhorrado.setScale(2, RoundingMode.HALF_UP)
    var corte = finDeMes(YearMonth.from(hoy))
    val pasos = hechos.toMutableList()
    // Si ya cumplió el paso de este mes, queda hecho con lo que ahorró y el camino sigue desde el próximo.
    val cumplioElMes = ahorrado.signum() > 0 && ahorrado >= paso
    if (cumplioElMes) {
        pasos += PasoAhorro(pasos.size + 1, corte, ahorrado, acumulado, EstadoPaso.COMPLETADO, esMeta = false)
        corte = finDeMes(YearMonth.from(corte).plusMonths(1))
    }
    repeat(PASOS_SIMBOLICOS) { i ->
        val monto = (if (i == 0 && !cumplioElMes) (paso - ahorrado).max(CERO) else paso).min(restante)
        if (monto.signum() > 0) {
            restante -= monto
            acumulado += monto
            pasos += PasoAhorro(
                numero = pasos.size + 1,
                fecha = corte,
                monto = monto,
                acumulado = acumulado,
                estado = if (pasos.none { it.estado == EstadoPaso.ACTUAL }) EstadoPaso.ACTUAL else EstadoPaso.PENDIENTE,
                esMeta = false,
            )
        }
        corte = finDeMes(YearMonth.from(corte).plusMonths(1))
    }
    return PlanAhorro(EstadoPlan.SIN_MARGEN, CadenciaPlan.MENSUAL, 1, paso, pasos, null)
}

// ---------------------------------------------------------------------------------------------
// Camino guardado: se arma una vez, se guarda con la meta y solo se vuelve a estimar cuando la
// persona anota un ahorro o un retiro. Los pasos completados nunca cambian.
// - Con fecha límite: las fechas no se mueven; se reparten de nuevo los montos de los pasos que quedan.
// - Sin fecha: se vuelve a estimar el monto de cada paso y cuántos pasos hacen falta.
// ---------------------------------------------------------------------------------------------

private const val PASOS_VISIBLES_SIMBOLICOS = 6

/** Un paso guardado: hasta cuándo y cuánto. */
data class PasoGuardado(val fecha: LocalDate, val monto: BigDecimal)

data class CaminoGuardado(
    /** Todos los pasos en orden, completados y por cumplir. */
    val pasos: List<PasoGuardado>,
    /** Desde aquí se cuentan los pasos: lo ahorrado menos lo que suman los completados. */
    val ahorradoInicial: BigDecimal,
    /** Lo ahorrado la última vez que se estimó: si cambia, la persona anotó algo. */
    val ahorradoAlEstimar: BigDecimal,
    /** Monto objetivo y fecha límite con que se armó: si la meta se edita, se arma de nuevo. */
    val objetivo: BigDecimal,
    val fechaLimite: LocalDate?,
    val estado: EstadoPlan,
    val cadencia: CadenciaPlan,
    val mesesPorPaso: Int,
    val ahorroPorPaso: BigDecimal,
)

/** El camino sigue sirviendo mientras la meta tenga el mismo objetivo y la misma fecha límite. */
fun CaminoGuardado.vigentePara(avance: AvanceMeta): Boolean =
    objetivo.compareTo(avance.montoObjetivo) == 0 && fechaLimite == avance.fechaLimite

/** Ingresos, gastos y lo recortable con que se estima cuánto puede ahorrar la persona. */
private class Margen(val capacidad: BigDecimal, val maximo: BigDecimal, val ingresos: BigDecimal)

private fun margenDe(pronosticoIngresos: Pronostico, pronosticoGastos: Pronostico, desglose: DesgloseGastos?): Margen? {
    val ingresos = pronosticoIngresos.esperado
    val sinDatos = pronosticoIngresos.confianza == Confianza.SIN_DATOS ||
        pronosticoGastos.confianza == Confianza.SIN_DATOS ||
        ingresos.signum() <= 0
    if (sinDatos) return null
    val capacidad = capacidadDeAhorro(ingresos, pronosticoGastos.esperado, desglose)
    val maximo = (capacidad + (desglose?.margenRecortable ?: BigDecimal.ZERO)).max(BigDecimal.ZERO)
    return Margen(capacidad, maximo, ingresos)
}

/**
 * Arma el camino de una meta. Null si todavía no hay datos de ingresos y gastos o si ya llegó al objetivo.
 * Con fecha límite, el último paso siempre cae en esa fecha.
 */
fun crearCamino(
    avance: AvanceMeta,
    aportes: List<AporteMeta>,
    pronosticoIngresos: Pronostico,
    pronosticoGastos: Pronostico,
    desglose: DesgloseGastos?,
    hoy: LocalDate,
): CaminoGuardado? {
    val total = avance.totalAhorrado.setScale(2, RoundingMode.HALF_UP)
    val faltante = (avance.montoObjetivo - total).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
    if (faltante.signum() == 0) return null
    val margen = margenDe(pronosticoIngresos, pronosticoGastos, desglose) ?: return null
    val fecha = avance.fechaLimite
    val calendario = if (fecha != null) {
        calendarioHastaFecha(fecha, faltante, margen, hoy)
    } else {
        calendarioSinFecha(faltante, aportes, margen, hoy, despuesDe = null)
    }
    return CaminoGuardado(
        pasos = repartir(calendario.cortes, calendario.paso, faltante),
        ahorradoInicial = total,
        ahorradoAlEstimar = total,
        objetivo = avance.montoObjetivo.setScale(2, RoundingMode.HALF_UP),
        fechaLimite = fecha,
        estado = calendario.estado,
        cadencia = calendario.cadencia,
        mesesPorPaso = calendario.mesesPorPaso,
        ahorroPorPaso = calendario.paso,
    )
}

/**
 * Vuelve a estimar el camino solo si la persona anotó un ahorro o un retiro desde la última vez.
 * Lo ahorrado completa los pasos en orden (si sobra, completa los siguientes por adelantado);
 * un retiro vuelve a abrir los últimos completados. Después se reparte lo que falta.
 */
fun CaminoGuardado.reestimar(
    avance: AvanceMeta,
    aportes: List<AporteMeta>,
    pronosticoIngresos: Pronostico,
    pronosticoGastos: Pronostico,
    desglose: DesgloseGastos?,
    hoy: LocalDate,
): CaminoGuardado {
    val total = avance.totalAhorrado.setScale(2, RoundingMode.HALF_UP)
    if (total.compareTo(ahorradoAlEstimar) == 0) return this

    val avanceDesdeInicio = total - ahorradoInicial
    val completados = mutableListOf<PasoGuardado>()
    var suma = BigDecimal.ZERO.setScale(2)
    for (paso in pasos) {
        if (suma + paso.monto > avanceDesdeInicio) break
        completados += paso
        suma += paso.monto
    }
    val faltante = (objetivo - total).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
    val base = copy(ahorradoInicial = total - suma, ahorradoAlEstimar = total)
    if (faltante.signum() == 0) return base.copy(pasos = completados)

    val margen = margenDe(pronosticoIngresos, pronosticoGastos, desglose)
    val fecha = fechaLimite
    val calendario = if (fecha != null) {
        // Las fechas no se mueven: los pasos vencidos sin completar se quitan y su monto pasa a los que vienen.
        val quedan = pasos.drop(completados.size).map { it.fecha }.filter { !it.isBefore(hoy) }.ifEmpty { listOf(fecha) }
        val necesario = faltante.divide(BigDecimal.valueOf(quedan.size.toLong()), 2, RoundingMode.CEILING)
        Calendario(estadoPara(necesario, margen, cadencia, mesesPorPaso), cadencia, mesesPorPaso, aMultiplo(necesario).max(MULTIPLO).min(faltante), quedan)
    } else if (margen != null) {
        calendarioSinFecha(faltante, aportes, margen, hoy, despuesDe = completados.lastOrNull()?.fecha)
    } else {
        // Sin datos de ingresos y gastos: se mantiene el monto por paso y se recalcula cuántos hacen falta.
        val cantidad = faltante.divide(ahorroPorPaso, 0, RoundingMode.CEILING).toInt().coerceAtLeast(1)
        Calendario(estado, cadencia, mesesPorPaso, ahorroPorPaso.min(faltante), cortesDesde(cadencia, mesesPorPaso, hoy, completados.lastOrNull()?.fecha, cantidad))
    }
    return base.copy(
        pasos = completados + repartir(calendario.cortes, calendario.paso, faltante),
        estado = calendario.estado,
        cadencia = calendario.cadencia,
        mesesPorPaso = calendario.mesesPorPaso,
        ahorroPorPaso = calendario.paso,
    )
}

/** Hasta la fecha límite, siempre. El estado dice si alcanza, si hay que recortar o si pide más de lo posible. */
private fun calendarioHastaFecha(fecha: LocalDate, base: BigDecimal, margen: Margen, hoy: LocalDate): Calendario {
    if (!fecha.isAfter(hoy)) return Calendario(EstadoPlan.NO_LLEGA_A_FECHA, CadenciaPlan.SEMANAL, 1, base, listOf(fecha))
    val cadencia = cadenciaPara(ChronoUnit.DAYS.between(hoy, fecha))
    val meses = mesesPorPasoHasta(cadencia, fecha, hoy)
    val cortes = cortesHasta(cadencia, meses, fecha, hoy)
    val necesario = base.divide(BigDecimal.valueOf(cortes.size.toLong()), 2, RoundingMode.CEILING)
    return Calendario(estadoPara(necesario, margen, cadencia, meses), cadencia, meses, aMultiplo(necesario).max(MULTIPLO).min(base), cortes)
}

private fun estadoPara(necesario: BigDecimal, margen: Margen?, cadencia: CadenciaPlan, meses: Int): EstadoPlan = when {
    margen == null -> EstadoPlan.ALCANZA
    necesario <= montoDelPeriodo(margen.capacidad.max(BigDecimal.ZERO), cadencia, meses) -> EstadoPlan.ALCANZA
    necesario <= montoDelPeriodo(margen.maximo, cadencia, meses) -> EstadoPlan.CON_RECORTE
    else -> EstadoPlan.NO_LLEGA_A_FECHA
}

/** Sin fecha: al ritmo posible según los ahorros y el historial; si casi no hay margen, pasos simbólicos. */
private fun calendarioSinFecha(
    faltante: BigDecimal,
    aportes: List<AporteMeta>,
    margen: Margen,
    hoy: LocalDate,
    despuesDe: LocalDate?,
): Calendario {
    if (margen.maximo < MINIMO_CON_MARGEN) {
        val paso = aMultiplo((margen.ingresos * PARTE_DEL_INGRESO_SIMBOLICA).min(MINIMO_CON_MARGEN).max(SIMBOLICO_MINIMO)).min(faltante)
        val cantidad = faltante.divide(paso, 0, RoundingMode.CEILING).toInt().coerceAtLeast(1)
        return Calendario(EstadoPlan.SIN_MARGEN, CadenciaPlan.MENSUAL, 1, paso, cortesDesde(CadenciaPlan.MENSUAL, 1, hoy, despuesDe, cantidad))
    }
    val calendario = porRitmo(faltante, ritmoSinFecha(aportes, margen.maximo), margen.capacidad, false, hoy)
    val cortes = cortesDesde(calendario.cadencia, calendario.mesesPorPaso, hoy, despuesDe, calendario.cortes.size)
    return calendario.copy(cortes = cortes)
}

/** [cantidad] vencimientos desde el periodo actual, siempre después del último paso completado. */
private fun cortesDesde(cadencia: CadenciaPlan, meses: Int, hoy: LocalDate, despuesDe: LocalDate?, cantidad: Int): List<LocalDate> {
    var primero = primerCorte(cadencia, meses, hoy)
    while (despuesDe != null && !primero.isAfter(despuesDe)) primero = siguienteCorte(cadencia, meses, primero)
    return generateSequence(primero) { siguienteCorte(cadencia, meses, it) }.take(cantidad).toList()
}

/** El mismo monto en cada paso y el último completa lo que falta. Nunca un paso de Bs 0. */
private fun repartir(cortes: List<LocalDate>, paso: BigDecimal, faltante: BigDecimal): List<PasoGuardado> {
    var restante = faltante
    val pasos = mutableListOf<PasoGuardado>()
    cortes.forEachIndexed { i, corte ->
        val monto = if (i == cortes.lastIndex) restante else paso.min(restante)
        if (monto.signum() > 0) {
            pasos += PasoGuardado(corte, monto.setScale(2, RoundingMode.HALF_UP))
            restante -= monto
        }
    }
    return pasos
}

/** Los pasos del camino según lo ahorrado: los primeros se llenan antes, como lecciones. */
fun CaminoGuardado.camino(totalAhorrado: BigDecimal, hoy: LocalDate): PlanAhorro {
    val avance = totalAhorrado - ahorradoInicial
    var umbral = BigDecimal.ZERO
    var hayActual = false
    val todos = pasos.mapIndexed { i, guardado ->
        umbral += guardado.monto
        val estadoPaso = when {
            avance >= umbral -> EstadoPaso.COMPLETADO
            !hayActual -> EstadoPaso.ACTUAL.also { hayActual = true }
            else -> EstadoPaso.PENDIENTE
        }
        val monto = if (estadoPaso == EstadoPaso.ACTUAL) (umbral - avance).min(guardado.monto) else guardado.monto
        PasoAhorro(
            numero = i + 1,
            fecha = guardado.fecha,
            monto = monto.setScale(2, RoundingMode.HALF_UP),
            acumulado = (ahorradoInicial + umbral).setScale(2, RoundingMode.HALF_UP),
            estado = estadoPaso,
            esMeta = i == pasos.lastIndex,
        )
    }
    val simbolico = estado == EstadoPlan.SIN_MARGEN && hayActual
    return PlanAhorro(
        estado = if (hayActual) estado else EstadoPlan.LOGRADA,
        cadencia = cadencia,
        mesesPorPaso = mesesPorPaso,
        ahorroPorPaso = ahorroPorPaso,
        pasos = if (simbolico) ventanaSimbolica(todos) else todos,
        fechaEstimada = if (simbolico) null else pasos.lastOrNull()?.fecha,
    )
}

/** Con pasos simbólicos el camino es largo: se muestran los últimos hechos, el actual y los siguientes. */
private fun ventanaSimbolica(pasos: List<PasoAhorro>): List<PasoAhorro> {
    val actual = pasos.indexOfFirst { it.estado == EstadoPaso.ACTUAL }
    val desde = (actual - 2).coerceAtLeast(0)
    return pasos.drop(desde).take(PASOS_VISIBLES_SIMBOLICOS).mapIndexed { i, paso -> paso.copy(numero = i + 1) }
}
