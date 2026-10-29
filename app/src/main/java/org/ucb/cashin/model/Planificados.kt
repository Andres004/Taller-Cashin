@file:UseSerializers(LocalDateSerializer::class)

package org.ucb.cashin.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.ucb.cashin.util.LocalDateSerializer
import java.time.LocalDate

/**
 * Cuántos días antes de la fecha esperada ya se puede marcar "Ya lo pagué / cobré".
 * Es menor que el periodo más corto (semanal = 7 días), así que después de marcar
 * un pago el siguiente nunca queda disponible enseguida: no se puede pagar dos veces el mismo periodo.
 */
const val DIAS_ANTES_PARA_MARCAR = 3L

/**
 * HU-16: fecha del siguiente pago o cobro según la frecuencia.
 * Devuelve null para UNICA: ya no hay otra fecha y el planificado se cierra.
 * Se calcula desde la fecha esperada (no desde hoy) para que un pago atrasado
 * no se salte un periodo.
 */
fun Frecuencia.siguienteFecha(desde: LocalDate): LocalDate? = when (this) {
    Frecuencia.UNICA -> null
    Frecuencia.SEMANAL -> desde.plusWeeks(1)
    Frecuencia.QUINCENAL -> desde.plusWeeks(2)
    Frecuencia.MENSUAL -> desde.plusMonths(1)
    Frecuencia.ANUAL -> desde.plusYears(1)
}

/** Si la fecha esperada ya pasó y todavía no se marcó como pagado. */
fun RegistroPlanificado.estaVencido(hoy: LocalDate): Boolean = proximaFecha.isBefore(hoy)

/** Desde qué día se puede marcar como pagado o cobrado. */
fun RegistroPlanificado.disponibleDesde(): LocalDate = proximaFecha.minusDays(DIAS_ANTES_PARA_MARCAR)

/** Si ya toca (o está por tocar) y se puede marcar "Ya lo pagué / cobré". */
fun RegistroPlanificado.sePuedeMarcar(hoy: LocalDate): Boolean = !hoy.isBefore(disponibleDesde())

/**
 * Lo que cambia en `registro_planificado` después de "Ya lo pagué / cobré":
 * la próxima fecha avanza, o el estado pasa a CANCELADO si era UNICA.
 */
@Serializable
data class AvancePlanificado(
    @SerialName("proxima_fecha") val proximaFecha: LocalDate,
    val estado: EstadoPlanificado,
)

/** Para quitar un planificado sin borrar las anotaciones que ya generó. */
@Serializable
data class EstadoDePlanificado(
    val estado: EstadoPlanificado,
)