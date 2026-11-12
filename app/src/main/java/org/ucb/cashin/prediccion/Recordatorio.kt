package org.ucb.cashin.prediccion

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/*
 * Recordatorio diario (Sprint 5, Perfil): un aviso a la hora elegida si ese día todavía no se anotó nada.
 * Kotlin puro: aquí solo se decide CUÁNDO avisar y QUÉ decir; la alarma y la notificación
 * están en el paquete notificaciones/.
 */

/** Lo que eligió la persona. Se guarda en el teléfono (la alarma vive en cada teléfono). */
data class AjusteRecordatorio(
    val activo: Boolean = false,
    val hora: Int = HORA_POR_DEFECTO,
    val minuto: Int = 0,
    /** true = solo de lunes a viernes. */
    val soloLaborables: Boolean = false,
) {
    val horario: LocalTime get() = LocalTime.of(hora.coerceIn(0, 23), minuto.coerceIn(0, 59))
}

/** A las 21:00: al final del día, cuando ya se sabe qué se gastó. */
const val HORA_POR_DEFECTO = 21

private fun esFinDeSemana(fecha: LocalDate): Boolean =
    fecha.dayOfWeek == DayOfWeek.SATURDAY || fecha.dayOfWeek == DayOfWeek.SUNDAY

private fun tocaAvisar(fecha: LocalDate, ajuste: AjusteRecordatorio): Boolean =
    !ajuste.soloLaborables || !esFinDeSemana(fecha)

/**
 * El próximo momento de aviso estrictamente después de [ahora]: hoy a la hora elegida si todavía
 * no pasó; si no, el siguiente día que corresponda. Null si el recordatorio está apagado.
 */
fun proximoAviso(ahora: LocalDateTime, ajuste: AjusteRecordatorio): LocalDateTime? {
    if (!ajuste.activo) return null
    var dia = ahora.toLocalDate()
    if (!dia.atTime(ajuste.horario).isAfter(ahora)) dia = dia.plusDays(1)
    while (!tocaAvisar(dia, ajuste)) dia = dia.plusDays(1)
    return dia.atTime(ajuste.horario)
}

/** Avisar solo si está activo, hoy toca y todavía no se anotó nada hoy. */
fun debeAvisar(hoy: LocalDate, ultimaAnotacion: LocalDate?, ajuste: AjusteRecordatorio): Boolean =
    ajuste.activo && tocaAvisar(hoy, ajuste) && ultimaAnotacion != hoy

/** Qué texto lleva el aviso. Cada valor tiene su texto en strings.xml. */
enum class TipoAviso {
    /** Ayer anotó: si hoy no anota, pierde la racha. [MensajeRecordatorio.racha] = los días. */
    RACHA_EN_RIESGO,
    /** No tiene racha que perder: solo una invitación a anotar. */
    SIN_RACHA,
}

data class MensajeRecordatorio(val tipo: TipoAviso, val racha: Int = 0)

/** [racha] es la última racha que la app conoció; solo sigue viva si la última anotación fue ayer. */
fun mensajeRecordatorio(hoy: LocalDate, ultimaAnotacion: LocalDate?, racha: Int): MensajeRecordatorio =
    if (racha > 0 && ultimaAnotacion == hoy.minusDays(1)) MensajeRecordatorio(TipoAviso.RACHA_EN_RIESGO, racha)
    else MensajeRecordatorio(TipoAviso.SIN_RACHA)
