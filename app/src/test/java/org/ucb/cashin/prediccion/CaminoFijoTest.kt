package org.ucb.cashin.prediccion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.ucb.cashin.model.AvanceMeta
import org.ucb.cashin.model.EstadoMeta
import java.math.BigDecimal
import java.time.LocalDate

class CaminoFijoTest {

    private val hoy = LocalDate.of(2026, 11, 8)
    private fun bd(texto: String) = BigDecimal(texto)
    private fun d(mes: Int, dia: Int, anio: Int = 2026) = LocalDate.of(anio, mes, dia)

    private fun meta(objetivo: String, ahorrado: String, fecha: LocalDate? = null) = AvanceMeta(
        metaId = "m", usuarioId = "u", nombre = "Play",
        montoObjetivo = bd(objetivo), fechaLimite = fecha, estado = EstadoMeta.ACTIVA, totalAhorrado = bd(ahorrado),
    )

    private fun pronostico(esperado: String) = Pronostico(bd(esperado), bd(esperado), bd(esperado), Confianza.MEDIA, 4)

    private fun crear(m: AvanceMeta, ingresos: String = "3200", gastos: String = "2350") =
        crearCamino(m, emptyList(), pronostico(ingresos), pronostico(gastos), null, hoy)

    private fun revisar(p: PlanAhorro, faltante: BigDecimal) {
        val porCumplir = p.pasos.filter { it.estado != EstadoPaso.COMPLETADO }
        assertEquals(faltante.setScale(2), porCumplir.fold(BigDecimal.ZERO.setScale(2)) { s, paso -> s + paso.monto })
        assertTrue(p.pasos.none { it.monto.signum() == 0 })
    }

    private fun actual(p: PlanAhorro) = p.pasos.first { it.estado == EstadoPaso.ACTUAL }

    @Test fun `los ahorros llenan los pasos en orden sin cambiar fechas ni montos`() {
        val camino = assertNotNullYDevolver(crear(meta("10000", "0")))
        val inicial = camino.camino(bd("0"), hoy)
        val paso = camino.paso

        var anterior = 0
        for (ahorrado in listOf("1000", "2400", "2480", "6000")) {
            val p = camino.camino(bd(ahorrado), hoy)
            assertEquals(inicial.pasos.map { it.fecha }, p.pasos.map { it.fecha })
            revisar(p, bd("10000") - bd(ahorrado))
            assertTrue(actual(p).numero >= anterior)
            anterior = actual(p).numero
        }
        val dosPasos = camino.camino(paso * BigDecimal(2), hoy)
        assertEquals(2, dosPasos.pasos.count { it.estado == EstadoPaso.COMPLETADO })
        assertEquals(3, actual(dosPasos).numero)
    }

    @Test fun `ahorrar de mas completa por adelantado los pasos siguientes`() {
        val camino = assertNotNullYDevolver(crear(meta("2400", "0")))
        val p = camino.camino(camino.paso * BigDecimal("2.5"), hoy)

        assertEquals(2, p.pasos.count { it.estado == EstadoPaso.COMPLETADO })
        assertEquals((camino.paso.divide(BigDecimal(2))).setScale(2), actual(p).monto)
    }

    @Test fun `con fecha limite el camino siempre termina en la fecha`() {
        val camino = assertNotNullYDevolver(crear(meta("2400", "0", d(5, 1, 2027))))
        val p = camino.camino(bd("0"), hoy)

        assertEquals(d(5, 1, 2027), p.pasos.last().fecha)
        assertEquals(d(5, 1, 2027), p.fechaEstimada)
        revisar(p, bd("2400"))
    }

    @Test fun `con fecha limite que no alcanza igual termina en la fecha y avisa`() {
        val camino = assertNotNullYDevolver(crear(meta("20000", "0", d(12, 31))))
        val p = camino.camino(bd("0"), hoy)

        assertEquals(EstadoPlan.NO_LLEGA_A_FECHA, p.estado)
        assertEquals(d(12, 31), p.pasos.last().fecha)
        revisar(p, bd("20000"))
    }

    @Test fun `con fecha limite atrasarse no mueve las fechas`() {
        val camino = assertNotNullYDevolver(crear(meta("2400", "0", d(5, 1, 2027))))
        val mucho = d(2, 10, 2027)

        assertEquals(camino, camino.alDia(bd("0"), mucho))
        assertEquals(d(5, 1, 2027), camino.camino(bd("0"), mucho).pasos.last().fecha)
    }

    @Test fun `sin fecha atrasarse corre las fechas desde el paso pendiente y conserva lo hecho`() {
        val camino = assertNotNullYDevolver(crear(meta("10000", "0")))
        val ahorrado = camino.paso
        val antes = camino.camino(ahorrado, hoy)
        val tarde = d(4, 10, 2027)

        val corrido = camino.alDia(ahorrado, tarde)
        val despues = corrido.camino(ahorrado, tarde)

        assertEquals(antes.pasos.first().fecha, despues.pasos.first().fecha)
        assertEquals(EstadoPaso.COMPLETADO, despues.pasos.first().estado)
        assertFalse(actual(despues).fecha.isBefore(tarde))
        assertEquals(antes.pasos.map { it.monto }, despues.pasos.map { it.monto })
        assertEquals(corrido, corrido.alDia(ahorrado, tarde))
    }

    @Test fun `sin fecha al dia no cambia nada`() {
        val camino = assertNotNullYDevolver(crear(meta("10000", "0")))

        assertEquals(camino, camino.alDia(bd("500"), hoy))
    }

    @Test fun `un retiro vuelve a abrir los pasos`() {
        val camino = assertNotNullYDevolver(crear(meta("2400", "0")))
        val con = camino.camino(camino.paso * BigDecimal(2), hoy)
        val sin = camino.camino(camino.paso, hoy)

        assertTrue(actual(sin).numero < actual(con).numero)
    }

    @Test fun `al llegar al objetivo todo queda completado`() {
        val camino = assertNotNullYDevolver(crear(meta("2400", "0")))
        val p = camino.camino(bd("2400"), hoy)

        assertEquals(EstadoPlan.LOGRADA, p.estado)
        assertTrue(p.pasos.all { it.estado == EstadoPaso.COMPLETADO })
    }

    @Test fun `si se edita el objetivo o la fecha el camino ya no sirve`() {
        val camino = assertNotNullYDevolver(crear(meta("2400", "0")))

        assertTrue(camino.vigentePara(meta("2400", "300")))
        assertFalse(camino.vigentePara(meta("3000", "300")))
        assertFalse(camino.vigentePara(meta("2400", "300", d(12, 31))))
    }

    @Test fun `sin margen muestra una ventana corta del camino`() {
        val camino = assertNotNullYDevolver(crear(meta("2400", "0"), ingresos = "2000", gastos = "2000"))
        val p = camino.camino(camino.paso * BigDecimal(5), hoy)

        assertEquals(EstadoPlan.SIN_MARGEN, p.estado)
        assertTrue(p.pasos.size <= 6)
        assertEquals(1, p.pasos.first().numero)
        assertNull(p.fechaEstimada)
    }

    @Test fun `sin datos no se arma camino`() {
        assertNull(crearCamino(meta("2400", "0"), emptyList(), Pronostico.SIN_DATOS, pronostico("100"), null, hoy))
    }

    private fun assertNotNullYDevolver(c: CaminoGuardado?): CaminoGuardado {
        assertNotNull(c)
        return c!!
    }
}
