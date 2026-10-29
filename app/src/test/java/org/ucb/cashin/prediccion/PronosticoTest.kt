package org.ucb.cashin.prediccion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class PronosticoTest {

    /** Serie desde enero 2026 con los valores dados, uno por mes. */
    private fun serie(vararg valores: Double): List<PuntoMensual> =
        valores.mapIndexed { i, v -> PuntoMensual(YearMonth.of(2026, 1).plusMonths(i.toLong()), v) }

    private fun bd(texto: String) = BigDecimal(texto)

    // ---------------------------------------------------------- pronosticarSiguienteMes

    @Test fun `sin meses no hay pronostico`() {
        val p = pronosticarSiguienteMes(emptyList())

        assertEquals(Confianza.SIN_DATOS, p.confianza)
        assertEquals(bd("0.00"), p.esperado)
        assertEquals(0, p.mesesUsados)
    }

    @Test fun `con un mes repite ese valor con margen de 25 por ciento`() {
        val p = pronosticarSiguienteMes(serie(500.0))

        assertEquals(Confianza.BAJA, p.confianza)
        assertEquals(bd("500.00"), p.esperado)
        assertEquals(bd("375.00"), p.minimo)
        assertEquals(bd("625.00"), p.maximo)
    }

    @Test fun `con dos meses usa el promedio`() {
        val p = pronosticarSiguienteMes(serie(400.0, 600.0))

        assertEquals(Confianza.BAJA, p.confianza)
        assertEquals(bd("500.00"), p.esperado)
        assertEquals(bd("400.00"), p.minimo)
        assertEquals(bd("600.00"), p.maximo)
    }

    @Test fun `con seis meses la confianza es alta`() {
        val p = pronosticarSiguienteMes(serie(900.0, 950.0, 1000.0, 980.0, 1020.0, 1010.0))

        assertEquals(Confianza.ALTA, p.confianza)
        assertEquals(6, p.mesesUsados)
    }

    @Test fun `con tres a cinco meses la confianza es media`() {
        assertEquals(Confianza.MEDIA, pronosticarSiguienteMes(serie(1.0, 2.0, 3.0)).confianza)
        assertEquals(Confianza.MEDIA, pronosticarSiguienteMes(serie(1.0, 2.0, 3.0, 4.0, 5.0)).confianza)
    }

    @Test fun `una serie constante pronostica el mismo valor`() {
        val p = pronosticarSiguienteMes(serie(1000.0, 1000.0, 1000.0, 1000.0, 1000.0, 1000.0))

        assertEquals(bd("1000.00"), p.esperado)
        // Sin variación el margen es el mínimo: 10 % del esperado.
        assertEquals(bd("900.00"), p.minimo)
        assertEquals(bd("1100.00"), p.maximo)
    }

    @Test fun `una serie creciente no baja del ultimo valor`() {
        // Regresión → 500 y promedio ponderado → 300: la mezcla 50/50 da 400.
        val p = pronosticarSiguienteMes(serie(100.0, 200.0, 300.0, 400.0))

        assertTrue(p.esperado >= bd("400.00"))
    }

    @Test fun `una serie creciente pronostica mas que su promedio`() {
        // Sigue la tendencia: no se queda en el promedio simple (350).
        val p = pronosticarSiguienteMes(serie(100.0, 200.0, 300.0, 400.0, 500.0, 600.0))

        assertTrue(p.esperado > bd("350.00"))
    }

    @Test fun `el esperado nunca es negativo`() {
        val p = pronosticarSiguienteMes(serie(1000.0, 500.0, 0.0))

        assertEquals(bd("0.00"), p.esperado)
        assertEquals(bd("0.00"), p.minimo)
    }

    @Test fun `solo usa los ultimos seis meses`() {
        val p = pronosticarSiguienteMes(serie(9000.0, 9000.0, 1000.0, 1000.0, 1000.0, 1000.0, 1000.0, 1000.0))

        assertEquals(6, p.mesesUsados)
        assertEquals(bd("1000.00"), p.esperado)
    }

    // ---------------------------------------------------------- proyectarMesActual

    private val pronosticoDe1000 = Pronostico(
        esperado = bd("1000.00"),
        minimo = bd("900.00"),
        maximo = bd("1100.00"),
        confianza = Confianza.MEDIA,
        mesesUsados = 4,
    )

    @Test fun `el primer dia del mes manda la historia`() {
        val proyeccion = proyectarMesActual(bd("10"), LocalDate.of(2026, 10, 1), pronosticoDe1000)

        // peso = 1/31 → casi todo viene del pronóstico.
        assertTrue((proyeccion - bd("1000")).abs() < bd("40"))
    }

    @Test fun `el ultimo dia del mes manda el ritmo real`() {
        val proyeccion = proyectarMesActual(bd("1200"), LocalDate.of(2026, 10, 31), pronosticoDe1000)

        assertEquals(bd("1200.00"), proyeccion)
    }

    @Test fun `sin pronostico usa solo el ritmo`() {
        // 300 en 10 días de un mes de 30 → 900.
        val proyeccion = proyectarMesActual(bd("300"), LocalDate.of(2026, 11, 10), Pronostico.SIN_DATOS)

        assertEquals(bd("900.00"), proyeccion)
    }

    // ---------------------------------------------------------- errorMedioAbsoluto

    @Test fun `con menos de cuatro meses no se puede evaluar`() {
        assertNull(errorMedioAbsoluto(serie(1.0, 2.0, 3.0)))
    }

    @Test fun `una serie constante no tiene error`() {
        assertEquals(0.0, errorMedioAbsoluto(serie(100.0, 100.0, 100.0, 100.0))!!, 0.001)
    }

    @Test fun `calcula el error medio de una serie conocida`() {
        // Mes 3: con [100, 200] pronostica 150 y fue 300 → error 150.
        // Mes 4: con [100, 200, 300] pronostica 316,67 y fue 400 → error 83,33.
        // Promedio: (150 + 83,33) / 2 = 116,67.
        assertEquals(116.67, errorMedioAbsoluto(serie(100.0, 200.0, 300.0, 400.0))!!, 0.01)
    }
}