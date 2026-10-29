package org.ucb.cashin.prediccion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.ucb.cashin.model.AporteMeta
import org.ucb.cashin.model.AvanceMeta
import org.ucb.cashin.model.EstadoMeta
import org.ucb.cashin.model.TipoAporte
import java.math.BigDecimal
import java.time.LocalDate

class EstimadorMetaTest {

    private val hoy = LocalDate.of(2026, 10, 15)

    private fun bd(texto: String) = BigDecimal(texto)

    private fun meta(objetivo: String, ahorrado: String, fechaLimite: LocalDate? = null) = AvanceMeta(
        metaId = "m",
        usuarioId = "u",
        nombre = "Celular",
        montoObjetivo = bd(objetivo),
        fechaLimite = fechaLimite,
        estado = EstadoMeta.ACTIVA,
        totalAhorrado = bd(ahorrado),
    )

    private fun aporte(fecha: LocalDate, monto: String, tipo: TipoAporte = TipoAporte.AHORRO) = AporteMeta(
        id = fecha.toString() + monto,
        metaId = "m",
        tipo = tipo,
        monto = bd(monto),
        fecha = fecha,
    )

    private fun pronostico(esperado: String, confianza: Confianza = Confianza.MEDIA) = Pronostico(
        esperado = bd(esperado),
        minimo = bd(esperado),
        maximo = bd(esperado),
        confianza = confianza,
        mesesUsados = 4,
    )

    private val ingresos3200 = pronostico("3200.00")
    private val gastos2850 = pronostico("2850.00")

    @Test fun `una meta que ya llego al objetivo esta alcanzada`() {
        val e = estimarMeta(meta("2500", "2500"), emptyList(), ingresos3200, gastos2850, hoy)

        assertTrue(e.alcanzada)
        assertEquals(bd("0.00"), e.faltante)
        assertEquals(0, e.meses)
    }

    @Test fun `con aportes en dos meses usa el ritmo de los aportes`() {
        val aportes = listOf(
            aporte(LocalDate.of(2026, 8, 10), "200"),
            aporte(LocalDate.of(2026, 9, 10), "100"),
        )
        val e = estimarMeta(meta("2500", "300"), aportes, ingresos3200, gastos2850, hoy)

        assertEquals(FuenteAhorro.APORTES, e.fuente)
        assertEquals(bd("150.00"), e.ahorroMensual)
        // 2200 / 150 = 14,67 → 15 meses.
        assertEquals(15, e.meses)
        assertEquals(LocalDate.of(2028, 1, 15), e.fechaEstimada)
    }

    @Test fun `los retiros restan del ritmo de los aportes`() {
        val aportes = listOf(
            aporte(LocalDate.of(2026, 8, 10), "300"),
            aporte(LocalDate.of(2026, 9, 10), "300"),
            aporte(LocalDate.of(2026, 9, 20), "100", TipoAporte.RETIRO),
        )
        val e = estimarMeta(meta("2500", "500"), aportes, ingresos3200, gastos2850, hoy)

        // (300 + 200) / 2 = 250 por mes.
        assertEquals(bd("250.00"), e.ahorroMensual)
    }

    @Test fun `sin aportes en dos meses usa la capacidad de ahorro`() {
        val aportes = listOf(aporte(LocalDate.of(2026, 10, 1), "300"))
        val e = estimarMeta(meta("2500", "300"), aportes, ingresos3200, gastos2850, hoy)

        assertEquals(FuenteAhorro.CAPACIDAD, e.fuente)
        assertEquals(bd("350.00"), e.ahorroMensual)
        // 2200 / 350 = 6,29 → 7 meses.
        assertEquals(7, e.meses)
    }

    @Test fun `la confianza por capacidad es la menor de los dos pronosticos`() {
        val e = estimarMeta(meta("2500", "0"), emptyList(), pronostico("3200", Confianza.ALTA), pronostico("2850", Confianza.BAJA), hoy)

        assertEquals(Confianza.BAJA, e.confianza)
    }

    @Test fun `con ritmo negativo la meta no se alcanza`() {
        val e = estimarMeta(meta("2500", "0"), emptyList(), pronostico("2000"), pronostico("2500"), hoy)

        assertNull(e.meses)
        assertNull(e.fechaEstimada)
    }

    @Test fun `sin aportes ni pronosticos no hay datos`() {
        val e = estimarMeta(meta("2500", "0"), emptyList(), Pronostico.SIN_DATOS, Pronostico.SIN_DATOS, hoy)

        assertEquals(FuenteAhorro.SIN_DATOS, e.fuente)
        assertEquals(Confianza.SIN_DATOS, e.confianza)
        assertNull(e.meses)
    }

    @Test fun `con fecha limite cercana avisa que no llega y cuanto haria falta`() {
        // Al ritmo de 350 necesita 7 meses, pero la fecha está a 5.
        val e = estimarMeta(meta("2500", "300", LocalDate.of(2027, 3, 15)), emptyList(), ingresos3200, gastos2850, hoy)

        assertEquals(false, e.llegaAntesDeLaFecha)
        // 2200 / 5 = 440.
        assertEquals(bd("440.00"), e.ahorroNecesarioPorMes)
    }

    @Test fun `con fecha limite lejana llega a tiempo`() {
        val e = estimarMeta(meta("2500", "300", LocalDate.of(2027, 12, 31)), emptyList(), ingresos3200, gastos2850, hoy)

        assertEquals(true, e.llegaAntesDeLaFecha)
    }

    @Test fun `sin fecha limite no compara fechas`() {
        val e = estimarMeta(meta("2500", "300"), emptyList(), ingresos3200, gastos2850, hoy)

        assertNull(e.llegaAntesDeLaFecha)
        assertNull(e.ahorroNecesarioPorMes)
        assertFalse(e.alcanzada)
    }
}