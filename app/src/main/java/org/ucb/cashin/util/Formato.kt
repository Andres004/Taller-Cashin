package org.ucb.cashin.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Zona horaria oficial del proyecto: todo cálculo por día o por mes usa la hora de Bolivia. */
val ZONA_BOLIVIA: ZoneId = ZoneId.of("America/La_Paz")

private val LOCALE_BO: Locale = Locale.forLanguageTag("es-BO")

/** Formatos de montos y fechas. Usar SIEMPRE estas funciones en la UI. */
object Formato {

    private val simbolos = DecimalFormatSymbols(LOCALE_BO).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    /** 1250.5 → "Bs 1.250,50" ; moneda "USD" → "$us 1.250,50". */
    fun monto(valor: BigDecimal, moneda: String = "BOB"): String {
        val patron = DecimalFormat("#,##0.00", simbolos)
        val texto = patron.format(valor.setScale(2, RoundingMode.HALF_UP))
        return "${simboloMoneda(moneda)} $texto"
    }

    /** Monto con signo para el historial: gasto "- Bs 20,00", ingreso "+ Bs 20,00". */
    fun montoConSigno(valor: BigDecimal, esGasto: Boolean, moneda: String = "BOB"): String =
        (if (esGasto) "- " else "+ ") + monto(valor, moneda)

    fun simboloMoneda(moneda: String): String = if (moneda == "USD") "\$us" else "Bs"

    /**
     * Convierte lo que el usuario escribe ("20", "20,5", "1.250,50", "20.5") a BigDecimal.
     * Devuelve null si no es un número válido. Nunca usa Double.
     */
    fun parsearMonto(texto: String): BigDecimal? {
        val limpio = texto.trim().replace(" ", "")
        if (limpio.isEmpty()) return null
        val normalizado = when {
            // "1.250,50" → "1250.50"
            limpio.contains(',') -> limpio.replace(".", "").replace(',', '.')
            // "1.250.000" (solo separadores de miles) → "1250000"
            limpio.count { it == '.' } > 1 -> limpio.replace(".", "")
            else -> limpio
        }
        // Máximo 2 decimales, como NUMERIC(14,2) en la base de datos.
        return normalizado.toBigDecimalOrNull()?.takeIf { it.stripTrailingZeros().scale() <= 2 }
    }

    // ---------------------------------------------------------------- Fechas

    private val fechaCorta = DateTimeFormatter.ofPattern("d MMM", LOCALE_BO)
    private val fechaLarga = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCALE_BO)
    private val mesAnio = DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE_BO)
    private val hora = DateTimeFormatter.ofPattern("HH:mm", LOCALE_BO)

    fun hoy(): LocalDate = LocalDate.now(ZONA_BOLIVIA)

    fun fechaDe(instante: Instant): LocalDate = instante.atZone(ZONA_BOLIVIA).toLocalDate()

    /** "24 oct." */
    fun fechaCorta(fecha: LocalDate): String = fecha.format(fechaCorta)

    /** "Hoy", "Ayer" o "sábado 24 de octubre" (para agrupar el historial por día). */
    fun encabezadoDia(fecha: LocalDate): String = when (fecha) {
        hoy() -> "Hoy"
        hoy().minusDays(1) -> "Ayer"
        else -> fecha.format(fechaLarga).replaceFirstChar { it.uppercase() }
    }

    /** "Octubre 2026" */
    fun mes(fecha: LocalDate): String = fecha.format(mesAnio).replaceFirstChar { it.uppercase() }

    /** "14:35" en hora de Bolivia. */
    fun hora(instante: Instant): String = instante.atZone(ZONA_BOLIVIA).format(hora)
}
