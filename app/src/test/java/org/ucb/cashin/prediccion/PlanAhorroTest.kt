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

class PlanAhorroTest {

    private val hoy = LocalDate.of(2026, 11, 8)
    private fun bd(texto: String) = BigDecimal(texto)
    private fun d(mes: Int, dia: Int, anio: Int = 2026) = LocalDate.of(anio, mes, dia)

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

    private fun pronostico(esperado: String, confianza: Confianza = Confianza.MEDIA) =
        Pronostico(bd(esperado), bd(esperado), bd(esperado), confianza, 4)

    /** Alquiler y WiFi fijos 1.250, comida 800 (3/5) y salidas 300 (1/5): capacidad 850 y recorte 270. */
    private val desglose = DesgloseGastos(
        listOf(
            DesgloseCategoria("viv", bd("1250.00"), bd("0.00"), 4, bd("0.00")),
            DesgloseCategoria("com", bd("0.00"), bd("800.00"), 3, bd("120.00")),
            DesgloseCategoria("sal", bd("0.00"), bd("300.00"), 1, bd("150.00")),
        ),
    )

    private val octubre = listOf(aporte(d(10, 20), "500"))

    private fun plan(
        m: AvanceMeta,
        aportes: List<AporteMeta> = octubre,
        ingresos: Pronostico = pronostico("3200"),
        gastos: Pronostico = pronostico("2350"),
        dg: DesgloseGastos? = desglose,
        dia: LocalDate = hoy,
    ) = planificarAhorro(m, aportes, ingresos, gastos, dg, dia)

    @Test fun `sin fecha limite usa el 75 por ciento del maximo posible`() {
        val p = plan(meta("2400", "500"))

        assertEquals(EstadoPlan.ALCANZA, p.estado)
        assertEquals(CadenciaPlan.QUINCENAL, p.cadencia)
        assertEquals(bd("410.00"), p.ahorroPorPaso)
        assertEquals(listOf(d(10, 20), d(11, 15), d(11, 30), d(12, 15), d(12, 31), d(1, 15, 2027)), p.pasos.map { it.fecha })
        assertEquals(listOf(500, 410, 410, 410, 410, 260), p.pasos.map { it.monto.toInt() })
        assertEquals(bd("2400.00"), p.pasos.last().acumulado)
        assertEquals(d(1, 15, 2027), p.fechaEstimada)
    }

    @Test fun `los estados de los pasos van hecho, actual y pendientes`() {
        val p = plan(meta("2400", "500"))

        assertEquals(
            listOf(EstadoPaso.COMPLETADO, EstadoPaso.ACTUAL, EstadoPaso.PENDIENTE, EstadoPaso.PENDIENTE, EstadoPaso.PENDIENTE, EstadoPaso.PENDIENTE),
            p.pasos.map { it.estado },
        )
        assertTrue(p.pasos.last().esMeta)
        assertFalse(p.pasos.dropLast(1).any { it.esMeta })
    }

    @Test fun `con fecha que le alcanza con recorte pide justo lo necesario`() {
        val p = plan(meta("2400", "500", d(12, 31)))

        assertEquals(EstadoPlan.CON_RECORTE, p.estado)
        assertEquals(bd("475.00"), p.ahorroPorPaso)
        assertEquals(d(12, 31), p.fechaEstimada)
        assertEquals(4, p.pasos.count { it.estado != EstadoPaso.COMPLETADO })
    }

    @Test fun `una fecha limite pegada al ultimo corte se une a ese paso`() {
        val p = plan(meta("2500", "1051", d(5, 1, 2027)))

        assertEquals(
            listOf(d(11, 30), d(12, 31), d(1, 31, 2027), d(2, 28, 2027), d(3, 31, 2027), d(5, 1, 2027)),
            p.pasos.filter { it.estado != EstadoPaso.COMPLETADO }.map { it.fecha },
        )
    }

    @Test fun `con pasos semanales una fecha en el dia de corte conserva su paso anterior`() {
        val p = plan(meta("200", "0", d(11, 22)), aportes = emptyList())

        assertEquals(CadenciaPlan.SEMANAL, p.cadencia)
        assertEquals(listOf(d(11, 8), d(11, 15), d(11, 22)), p.pasos.map { it.fecha })
    }

    @Test fun `el paso actual es el primero sin cumplir`() {
        val p = plan(meta("2400", "500"))

        assertEquals(d(11, 15), p.pasoActual?.fecha)
    }

    @Test fun `si ni con el maximo llega avisa la fecha real`() {
        val p = plan(meta("2400", "500", d(12, 15)))

        assertEquals(EstadoPlan.NO_LLEGA_A_FECHA, p.estado)
        assertEquals(bd("550.00"), p.ahorroPorPaso)
        assertEquals(d(12, 31), p.fechaEstimada)
    }

    @Test fun `si cobra 100 a la semana ningun paso supera lo que recibe en ese periodo`() {
        val p = plan(meta("300", "0"), aportes = emptyList(), ingresos = pronostico("433.33"), gastos = pronostico("200"), dg = null)

        assertEquals(CadenciaPlan.QUINCENAL, p.cadencia)
        assertEquals(bd("85.00"), p.ahorroPorPaso)
        assertTrue(p.pasos.all { it.monto <= bd("200") })
    }

    @Test fun `el ritmo real alto nunca pasa del maximo que permiten los ingresos`() {
        val muchos = listOf(aporte(d(9, 10), "3000"), aporte(d(10, 10), "3000"))
        val p = plan(meta("20000", "6000"), aportes = muchos)

        val porMes = p.ahorroPorPaso.divide(BigDecimal.valueOf(p.mesesPorPaso.toLong()))
        assertTrue("$porMes", porMes <= bd("1120"))
    }

    @Test fun `el ritmo real se usa cuando es mayor que el 75 por ciento`() {
        val aportes = listOf(aporte(d(9, 10), "1000"), aporte(d(10, 10), "1000"))
        val p = plan(meta("9000", "2000"), aportes = aportes)

        assertEquals(CadenciaPlan.MENSUAL, p.cadencia)
        assertEquals(EstadoPlan.CON_RECORTE, p.estado)
        assertEquals(bd("1000.00"), p.ahorroPorPaso)
    }

    @Test fun `lo ahorrado en el periodo se descuenta del paso actual`() {
        val p = plan(meta("2400", "600"), aportes = octubre + aporte(d(11, 3), "100"))

        val actual = p.pasos.first { it.estado == EstadoPaso.ACTUAL }
        assertEquals(bd("310.00"), actual.monto)
        assertEquals(bd("910.00"), actual.acumulado)
    }

    @Test fun `si ya cumplio el paso del periodo el actual es el siguiente`() {
        val p = plan(meta("2400", "1000"), aportes = octubre + aporte(d(11, 3), "500"))

        val actual = p.pasos.first { it.estado == EstadoPaso.ACTUAL }
        assertEquals(d(11, 30), actual.fecha)
        assertEquals(bd("410.00"), actual.monto)
    }

    @Test fun `ahorrar de mas acorta el camino`() {
        val normal = plan(meta("2400", "500"))
        val deMas = plan(meta("2400", "1300"), aportes = listOf(aporte(d(10, 20), "1300")))

        assertTrue(deMas.pasos.count { it.estado != EstadoPaso.COMPLETADO } < normal.pasos.count { it.estado != EstadoPaso.COMPLETADO })
    }

    @Test fun `un retiro baja el acumulado y se recalcula`() {
        val aportes = listOf(aporte(d(9, 10), "800"), aporte(d(10, 10), "300", TipoAporte.RETIRO))
        val p = plan(meta("2400", "500"), aportes = aportes)

        assertEquals(1, p.pasos.count { it.estado == EstadoPaso.COMPLETADO })
        assertEquals(bd("2400.00"), p.pasos.last().acumulado)
    }

    @Test fun `solo se ven los ultimos 3 pasos hechos`() {
        val aportes = (6..10).map { aporte(d(it, 5), "100") }
        val p = plan(meta("2400", "500"), aportes = aportes)

        assertEquals(3, p.pasos.count { it.estado == EstadoPaso.COMPLETADO })
    }

    @Test fun `una meta muy larga agrupa los pasos y no pasa de 9 por delante`() {
        val p = plan(meta("50000", "0"), aportes = emptyList())

        assertTrue(p.pasos.size <= 9)
        assertTrue(p.mesesPorPaso > 1)
        assertEquals(bd("50000.00"), p.pasos.last().acumulado)
    }

    @Test fun `una meta corta tiene al menos 3 pasos`() {
        val p = plan(meta("300", "0"), aportes = emptyList())

        assertEquals(3, p.pasos.size)
        assertEquals(bd("300.00"), p.pasos.last().acumulado)
    }

    @Test fun `si falta muy poco es un solo paso`() {
        val p = plan(meta("20", "0"), aportes = emptyList())

        assertEquals(1, p.pasos.size)
        assertEquals(bd("20.00"), p.pasos.single().monto)
    }

    @Test fun `sin margen propone pasos simbolicos y no promete fecha`() {
        val p = plan(meta("2400", "0"), aportes = emptyList(), ingresos = pronostico("2000"), gastos = pronostico("2000"), dg = null)

        assertEquals(EstadoPlan.SIN_MARGEN, p.estado)
        assertEquals(bd("50.00"), p.ahorroPorPaso)
        assertEquals(3, p.pasos.size)
        assertNull(p.fechaEstimada)
        assertFalse(p.pasos.any { it.esMeta })
    }

    @Test fun `con ingresos bajos el paso simbolico es proporcional`() {
        val p = plan(meta("2400", "0"), aportes = emptyList(), ingresos = pronostico("433"), gastos = pronostico("433"), dg = null)

        assertEquals(bd("20.00"), p.ahorroPorPaso)
    }

    @Test fun `sin datos de ingresos no pide ningun monto`() {
        val p = plan(meta("2400", "500"), ingresos = Pronostico.SIN_DATOS)

        assertEquals(EstadoPlan.SIN_DATOS, p.estado)
        assertTrue(p.pasos.none { it.estado != EstadoPaso.COMPLETADO })
        assertNull(p.fechaEstimada)
    }

    @Test fun `meta lograda no tiene pasos pendientes`() {
        val p = plan(meta("500", "500"))

        assertEquals(EstadoPlan.LOGRADA, p.estado)
        assertTrue(p.pasos.all { it.estado == EstadoPaso.COMPLETADO })
        assertTrue(p.pasos.last().esMeta)
    }

    /** Lo que se pide desde el paso actual suma justo lo que falta, sin pasos de Bs 0. */
    private fun revisar(p: PlanAhorro, faltante: BigDecimal, caso: String) {
        val porCumplir = p.pasos.filter { it.estado != EstadoPaso.COMPLETADO }
        assertEquals(caso, faltante.setScale(2), porCumplir.fold(BigDecimal.ZERO.setScale(2)) { s, paso -> s + paso.monto })
        assertTrue(caso, p.pasos.none { it.monto.signum() == 0 })
        assertEquals(caso, 1, p.pasos.count { it.estado == EstadoPaso.ACTUAL })
        assertTrue(caso, p.pasos.last().esMeta)
    }

    private fun numeroActual(p: PlanAhorro) = p.pasos.first { it.estado == EstadoPaso.ACTUAL }.numero

    @Test fun `varios ahorros el mismo dia avanzan el camino y nunca lo hacen retroceder`() {
        var anterior = 0
        for (acumulado in listOf("1000", "2400", "2480", "4000")) {
            val aportes = listOf(aporte(d(11, 8), acumulado))
            val p = plan(meta("10000", acumulado), aportes = aportes)
            revisar(p, bd("10000") - bd(acumulado), "ahorrado $acumulado")
            val actual = numeroActual(p)
            assertTrue("ahorrado $acumulado: $actual < $anterior", actual >= anterior)
            anterior = actual
        }
    }

    @Test fun `pasarse del paso lo deja hecho con todo lo ahorrado y reparte lo que falta`() {
        val p = plan(meta("2400", "1400"), aportes = octubre + aporte(d(11, 3), "900"))

        val hecho = p.pasos.first { it.fecha == d(11, 15) }
        assertEquals(EstadoPaso.COMPLETADO, hecho.estado)
        assertEquals(bd("900.00"), hecho.monto)
        assertEquals(d(11, 30), p.pasos.first { it.estado == EstadoPaso.ACTUAL }.fecha)
        revisar(p, bd("1000"), "sin fecha")
    }

    @Test fun `con fecha limite pasarse del paso baja lo que piden los pasos que quedan`() {
        val normal = plan(meta("2400", "500", d(12, 31)))
        val deMas = plan(meta("2400", "1400", d(12, 31)), aportes = octubre + aporte(d(11, 3), "900"))

        val pendienteNormal = normal.pasos.filter { it.estado == EstadoPaso.PENDIENTE }.maxOf { it.monto }
        val pendienteDeMas = deMas.pasos.filter { it.estado == EstadoPaso.PENDIENTE }.maxOf { it.monto }
        assertTrue("$pendienteDeMas < $pendienteNormal", pendienteDeMas < pendienteNormal)
        assertEquals(d(12, 31), deMas.pasos.last().fecha)
        revisar(deMas, bd("1000"), "con fecha")
    }

    @Test fun `distintos montos ahorrados en el periodo dejan siempre un camino valido`() {
        var anterior = 0
        for (extra in listOf("0", "200", "410", "600", "900", "1500", "1890")) {
            val aportes = if (extra == "0") octubre else octubre + aporte(d(11, 3), extra)
            val total = bd("500") + bd(extra)
            val p = plan(meta("2400", total.toPlainString()), aportes = aportes)
            revisar(p, bd("2400") - total, "extra $extra")
            val actual = numeroActual(p)
            assertTrue("extra $extra: $actual < $anterior", actual >= anterior)
            anterior = actual
        }
    }

    @Test fun `un retiro en el mismo periodo vuelve a abrir el paso`() {
        val aportes = octubre + aporte(d(11, 3), "600") + aporte(d(11, 4), "400", TipoAporte.RETIRO)
        val p = plan(meta("2400", "700"), aportes = aportes)

        val actual = p.pasos.first { it.estado == EstadoPaso.ACTUAL }
        assertEquals(d(11, 15), actual.fecha)
        assertEquals(bd("210.00"), actual.monto)
        revisar(p, bd("1700"), "retiro")
    }

    @Test fun `sin margen cumplir el paso del mes lo deja hecho y sigue el mes siguiente`() {
        val p = plan(
            meta("2400", "60"), aportes = listOf(aporte(d(11, 3), "60")),
            ingresos = pronostico("2000"), gastos = pronostico("2000"), dg = null,
        )

        assertEquals(EstadoPlan.SIN_MARGEN, p.estado)
        assertEquals(EstadoPaso.COMPLETADO, p.pasos.first { it.fecha == d(11, 30) }.estado)
        assertEquals(d(12, 31), p.pasos.first { it.estado == EstadoPaso.ACTUAL }.fecha)
        assertTrue(p.pasos.none { it.monto.signum() == 0 })
    }
}
