package org.ucb.cashin.prediccion

import org.ucb.cashin.model.AvanceMeta
import org.ucb.cashin.model.Desafio
import org.ucb.cashin.model.GastoCategoriaMes
import org.ucb.cashin.model.ProgresoDesafio
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/** Un desafío con límite está "en riesgo" desde el 80 % gastado. */
private const val UMBRAL_DESAFIO = 0.80

/** Una categoría "sube" si al ritmo del mes cierra 30 % o más arriba de su promedio. */
private const val UMBRAL_CATEGORIA = 1.30

/** Una meta está "cerca" desde el 90 % ahorrado. */
private val UMBRAL_META = BigDecimal("0.90")

/**
 * HU-21: consejo del día de Cashi en Inicio. Se elige por reglas, en este orden
 * (gana la primera que se cumpla):
 *
 * 1. No anotó hoy y tiene racha de 2+ días → RACHA_EN_RIESGO.
 * 2. Un desafío con límite ya va en 80 % o más → DESAFIO_EN_RIESGO, con lo que queda.
 * 3. Una categoría, proyectada al ritmo del mes, supera en 30 % o más su promedio de los
 *    últimos 3 meses completos → CATEGORIA_SUBE (la de mayor exceso).
 * 4. La proyección del mes supera el máximo pronosticado → GASTO_MAS_RAPIDO.
 * 5. Una meta entre 90 % y 100 % → META_CERCA, con lo que falta.
 * 6. Racha de 3+ días → RACHA_BUENA.
 * 7. Sin datos → SIN_DATOS; si no → GASTO_NORMAL.
 */
fun consejoDelDia(
    hoy: LocalDate,
    anotoHoy: Boolean,
    racha: Int,
    gastoPorCategoria: List<GastoCategoriaMes>,
    proyeccionDelMes: BigDecimal?,
    pronosticoDelMes: Pronostico,
    desafioActivo: Desafio?, progreso: ProgresoDesafio?,
    metaMasCercana: AvanceMeta?,
): MensajeAvatar {
    if (!anotoHoy && racha >= 2) {
        return MensajeAvatar(TipoMensaje.RACHA_EN_RIESGO, Animo.PREOCUPADO, dias = racha)
    }

    desafioEnRiesgo(desafioActivo, progreso)?.let { return it }

    categoriaQueSube(gastoPorCategoria, hoy)?.let { return it }

    if (proyeccionDelMes != null && pronosticoDelMes.confianza != Confianza.SIN_DATOS &&
        proyeccionDelMes > pronosticoDelMes.maximo
    ) {
        return MensajeAvatar(TipoMensaje.GASTO_MAS_RAPIDO, Animo.PREOCUPADO, monto = proyeccionDelMes)
    }

    metaCerca(metaMasCercana)?.let { return it }

    if (racha >= 3) {
        return MensajeAvatar(TipoMensaje.RACHA_BUENA, Animo.FELIZ, dias = racha)
    }

    val sinDatos = pronosticoDelMes.confianza == Confianza.SIN_DATOS && gastoPorCategoria.isEmpty()
    return if (sinDatos) {
        MensajeAvatar(TipoMensaje.SIN_DATOS, Animo.NEUTRAL)
    } else {
        MensajeAvatar(TipoMensaje.GASTO_NORMAL, Animo.FELIZ, monto = proyeccionDelMes)
    }
}

/**
 * Todo lo que Cashi puede decir ahora, para no repetir siempre lo mismo. El primero es el
 * [principal]. Si la racha está por perderse, solo se avisa eso: es lo único que no puede esperar.
 */
fun consejosDisponibles(
    principal: MensajeAvatar,
    hoy: LocalDate,
    racha: Int,
    gastoPorCategoria: List<GastoCategoriaMes>,
    desafioActivo: Desafio?,
    progreso: ProgresoDesafio?,
    metaMasCercana: AvanceMeta?,
    totalGastos: BigDecimal,
    totalIngresos: BigDecimal,
    xpParaNivel: Int,
): List<MensajeAvatar> {
    if (principal.tipo == TipoMensaje.RACHA_EN_RIESGO) return listOf(principal)
    return buildList {
        add(principal)
        desafioEnRiesgo(desafioActivo, progreso)?.let(::add)
        categoriaQueSube(gastoPorCategoria, hoy)?.let(::add)
        metaCerca(metaMasCercana)?.let(::add)
        if (racha >= 3) add(MensajeAvatar(TipoMensaje.RACHA_BUENA, Animo.FELIZ, dias = racha))
        if (totalIngresos.signum() > 0) {
            add(MensajeAvatar(TipoMensaje.INGRESOS_DEL_MES, Animo.CELEBRANDO, monto = totalIngresos))
        }
        if (totalIngresos > totalGastos) {
            add(MensajeAvatar(TipoMensaje.INGRESOS_A_FAVOR, Animo.FELIZ, monto = totalIngresos - totalGastos))
        }
        metaMasCercana?.let { meta ->
            add(MensajeAvatar(TipoMensaje.META_AVANCE, Animo.FELIZ, monto = meta.montoObjetivo - meta.totalAhorrado, nombre = meta.nombre))
        }
        add(MensajeAvatar(TipoMensaje.XP_PARA_NIVEL, Animo.FELIZ, dias = xpParaNivel))
    }.distinctBy { it.tipo }
}

/**
 * Cambia de mensaje en cada visita a Inicio con [turno]. Si [entroIngreso], Cashi reacciona a
 * ese ingreso antes que nada.
 */
fun elegirConsejo(consejos: List<MensajeAvatar>, turno: Int, entroIngreso: Boolean): MensajeAvatar {
    if (entroIngreso) consejos.firstOrNull { it.tipo == TipoMensaje.INGRESOS_DEL_MES }?.let { return it }
    return consejos[turno.mod(consejos.size)]
}

/** Regla 2: el desafío activo con límite ya gastó el 80 % o más. [monto] = lo que queda. */
private fun desafioEnRiesgo(desafio: Desafio?, progreso: ProgresoDesafio?): MensajeAvatar? {
    val limite = desafio?.montoLimite ?: return null
    if (progreso == null || progreso.desafioId != desafio.id || limite.signum() <= 0) return null
    if (progreso.gastado.toDouble() < limite.toDouble() * UMBRAL_DESAFIO) return null

    val queda = (limite - progreso.gastado).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
    return MensajeAvatar(TipoMensaje.DESAFIO_EN_RIESGO, Animo.PREOCUPADO, monto = queda, nombre = desafio.titulo)
}

/**
 * Regla 3: proyecta el gasto de cada categoría del mes actual (gasto ÷ día del mes × días del mes)
 * y lo compara con su promedio de los últimos 3 meses completos. [monto] = lo proyectado.
 */
private fun categoriaQueSube(gastoPorCategoria: List<GastoCategoriaMes>, hoy: LocalDate): MensajeAvatar? {
    val promedios = promediosPorCategoria(gastoPorCategoria, hoy).associateBy { it.categoriaId }
    val mesActual = YearMonth.from(hoy)

    val peor = gastoPorCategoria
        .filter { YearMonth.from(it.mes) == mesActual }
        .mapNotNull { fila ->
            val promedio = promedios[fila.categoriaId]?.promedio ?: return@mapNotNull null
            if (promedio <= 0) return@mapNotNull null
            val proyectado = fila.total.toDouble() / hoy.dayOfMonth * hoy.lengthOfMonth()
            if (proyectado < promedio * UMBRAL_CATEGORIA) null else Triple(fila, proyectado, proyectado - promedio)
        }
        .maxByOrNull { it.third }
        ?: return null

    return MensajeAvatar(
        TipoMensaje.CATEGORIA_SUBE,
        Animo.PREOCUPADO,
        monto = aMonto(peor.second),
        nombre = peor.first.nombre,
    )
}

/** Regla 5: la meta ya va entre 90 % y 100 %. [monto] = lo que falta. */
private fun metaCerca(meta: AvanceMeta?): MensajeAvatar? {
    if (meta == null || meta.montoObjetivo.signum() <= 0) return null
    val ahorrado = meta.totalAhorrado
    if (ahorrado < meta.montoObjetivo * UMBRAL_META || ahorrado >= meta.montoObjetivo) return null

    val falta = (meta.montoObjetivo - ahorrado).setScale(2, RoundingMode.HALF_UP)
    return MensajeAvatar(TipoMensaje.META_CERCA, Animo.CELEBRANDO, monto = falta, nombre = meta.nombre)
}
