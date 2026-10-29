package org.ucb.cashin.prediccion

import java.math.BigDecimal
import java.time.LocalDate

/*
 * Mensajes del avatar a partir de plantillas (no es IA generativa).
 * Para que este paquete siga sin depender de Android, aquí solo se decide QUÉ mensaje
 * corresponde y con qué datos; la pantalla lo traduce a un texto de strings.xml y a una
 * pose de Cashi (Emocion) con mensajeTexto() / emocionDe() del paquete ui.
 */

/** Ánimo del avatar. La UI lo convierte en la pose de Cashi. */
enum class Animo { FELIZ, NEUTRAL, PREOCUPADO, CELEBRANDO }

/** Qué plantilla de mensaje corresponde. Cada valor tiene su texto en strings.xml. */
enum class TipoMensaje {
    /** La meta ya llegó al objetivo. */
    META_ALCANZADA,
    /** Al ritmo actual se alcanza (y si tiene fecha límite, llega a tiempo). */
    META_ALCANZABLE,
    /** Al ritmo actual no llega a la fecha límite. */
    META_NO_LLEGA,
    /** El ritmo de ahorro es 0 o negativo. */
    META_RITMO_NEGATIVO,
    /** No hay historia suficiente para estimar. */
    SIN_DATOS,
    /** El mes va con más gasto que el máximo pronosticado. */
    GASTO_MAS_RAPIDO,
    /** El gasto del mes va dentro de lo normal. */
    GASTO_NORMAL,
}

/**
 * Un mensaje listo para mostrarse: la plantilla, el ánimo y los datos que van en el texto.
 * [monto] y [meses]/[fecha] se usan según la plantilla.
 */
data class MensajeAvatar(
    val tipo: TipoMensaje,
    val animo: Animo,
    val monto: BigDecimal? = null,
    val meses: Int? = null,
    val fecha: LocalDate? = null,
)

/**
 * Mensaje del avatar para la estimación de una meta (HU-20).
 * - Alcanzada → CELEBRANDO.
 * - Sin datos → NEUTRAL: "Anota tus gastos unas semanas más…".
 * - Ritmo ≤ 0 → PREOCUPADO: "Este mes tus gastos superan tus ingresos…".
 * - No llega a la fecha → PREOCUPADO con el ahorro necesario por mes.
 * - Alcanzable → FELIZ con el faltante, los meses y la fecha aproximada.
 */
fun mensajeParaMeta(estimacion: EstimacionMeta): MensajeAvatar = when {
    estimacion.alcanzada ->
        MensajeAvatar(TipoMensaje.META_ALCANZADA, Animo.CELEBRANDO)

    estimacion.fuente == FuenteAhorro.SIN_DATOS ->
        MensajeAvatar(TipoMensaje.SIN_DATOS, Animo.NEUTRAL)

    estimacion.meses == null ->
        MensajeAvatar(TipoMensaje.META_RITMO_NEGATIVO, Animo.PREOCUPADO)

    estimacion.llegaAntesDeLaFecha == false ->
        MensajeAvatar(TipoMensaje.META_NO_LLEGA, Animo.PREOCUPADO, monto = estimacion.ahorroNecesarioPorMes)

    else ->
        MensajeAvatar(
            TipoMensaje.META_ALCANZABLE,
            Animo.FELIZ,
            monto = estimacion.faltante,
            meses = estimacion.meses,
            fecha = estimacion.fechaEstimada,
        )
}

/**
 * Mensaje del avatar para el pronóstico del mes en Inicio.
 * Si la proyección del mes supera el máximo pronosticado → PREOCUPADO ("Vas gastando más rápido…").
 */
fun mensajeParaProyeccion(proyeccion: BigDecimal, pronostico: Pronostico): MensajeAvatar = when {
    pronostico.confianza == Confianza.SIN_DATOS ->
        MensajeAvatar(TipoMensaje.SIN_DATOS, Animo.NEUTRAL)

    proyeccion > pronostico.maximo ->
        MensajeAvatar(TipoMensaje.GASTO_MAS_RAPIDO, Animo.PREOCUPADO, monto = proyeccion)

    else ->
        MensajeAvatar(TipoMensaje.GASTO_NORMAL, Animo.FELIZ, monto = proyeccion)
}