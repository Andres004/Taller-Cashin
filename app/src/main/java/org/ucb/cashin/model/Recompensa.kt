package org.ucb.cashin.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** XP que da cada una de las primeras anotaciones del día (lo decide la base de datos). */
const val XP_POR_REGISTRO = 10

/** Lo que se ganó al guardar una anotación nueva. */
data class RecompensaAnotar(
    val tipo: TipoRegistro,
    val xpGanado: Int,
    /** Racha después de anotar. */
    val racha: Int,
    /** Es la primera anotación del día: la racha avanzó y merece su momento. */
    val primeraDelDia: Boolean,
)

/**
 * Compara el perfil de antes y de después de anotar. Si no se pudo leer alguno de los dos,
 * usa [ganoXp] para el XP y deja la celebración corta: mejor no festejar una racha sin saberla.
 */
fun calcularRecompensa(
    antes: Perfil?,
    despues: Perfil?,
    hoy: LocalDate,
    tipo: TipoRegistro,
    ganoXp: Boolean,
): RecompensaAnotar {
    val xp = if (antes != null && despues != null) {
        (despues.xp - antes.xp).coerceAtLeast(0)
    } else {
        if (ganoXp) XP_POR_REGISTRO else 0
    }
    return RecompensaAnotar(
        tipo = tipo,
        xpGanado = xp,
        racha = despues?.rachaVigente(hoy) ?: 0,
        primeraDelDia = antes != null && despues != null && !antes.anotoHoy(hoy) && despues.anotoHoy(hoy),
    )
}

enum class EstadoDiaRacha { LOGRADO, PENDIENTE_HOY, SIN_ANOTAR, FUTURO }

data class DiaRacha(val fecha: LocalDate, val estado: EstadoDiaRacha)

/** La semana actual, de lunes a domingo, marcando qué días entran en la racha. */
fun semanaDeRacha(hoy: LocalDate, racha: Int, anotoHoy: Boolean): List<DiaRacha> {
    val lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val ultimoDia = if (anotoHoy) hoy else hoy.minusDays(1)
    val primerDia = ultimoDia.minusDays((racha - 1).toLong())
    return List(7) { i ->
        val dia = lunes.plusDays(i.toLong())
        val estado = when {
            racha > 0 && !dia.isBefore(primerDia) && !dia.isAfter(ultimoDia) -> EstadoDiaRacha.LOGRADO
            dia == hoy -> EstadoDiaRacha.PENDIENTE_HOY
            dia.isAfter(hoy) -> EstadoDiaRacha.FUTURO
            else -> EstadoDiaRacha.SIN_ANOTAR
        }
        DiaRacha(dia, estado)
    }
}
