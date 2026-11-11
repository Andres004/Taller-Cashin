package org.ucb.cashin.prediccion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.ucb.cashin.model.AporteMeta
import org.ucb.cashin.model.AvanceMeta
import org.ucb.cashin.model.EstadoMeta
import org.ucb.cashin.model.TipoAporte
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

    private fun aporte(fecha: LocalDate, monto: String) =
        AporteMeta(id = fecha.toString() + monto, metaId = "m", tipo = TipoAporte.AHORRO, monto = bd(monto), fecha = fecha)

    private fun pronostico(esperado: String) = Pronostico(bd(esperado), bd(esperado), bd(esperado), Confianza.MEDIA, 4)

    private val ingresos = pronostico("3200")
    private val gastos = pronostico("2350")

    private fun crear(m: AvanceMeta, ing: Pronostico = ingresos, gas: Pronostico = gastos) =
        requireNotNull(crearCamino(m, emptyList(), ing, gas, null, hoy))

    private fun CaminoGuardado.anotar(m: AvanceMeta, dia: LocalDate = hoy, aportes: List<AporteMeta> = emptyList()) =
        reestimar(m, aportes, ingresos, gastos, null, dia)

    private fun pendientes(p: PlanAhorro) = p.pasos.filter { it.estado != EstadoPaso.COMPLETADO }

    private fun revisar(p: PlanAhorro, faltante: String) {
        assertEquals(bd(faltante).setScale(2), pendientes(p).fold(BigDecimal.ZERO.setScale(2)) { s, paso -> s + paso.monto })
        assertTrue(p.pasos.none { it.monto.signum() == 0 })
    }

    private fun actual(p: PlanAhorro) = p.pasos.first { it.estado == EstadoPaso.ACTUAL }

    @Test fun `sin anotar nada el camino no cambia aunque pase el tiempo`() {
        val camino = crear(meta("2400", "0", d(5, 1, 2027)))

        assertSame(camino, camino.anotar(meta("2400", "0", d(5, 1, 2027)), dia = d(3, 1, 2027)))
    }

    @Test fun `con fecha pasarse completa pasos por adelantado y baja los que quedan sin mover fechas`() {
        val fecha = d(5, 1, 2027)
        val camino = crear(meta("2400", "0", fecha))
        val antes = camino.camino(bd("0"), hoy)
        val ahorro = camino.ahorroPorPaso * BigDecimal("1.5")

        val nuevo = camino.anotar(meta("2400", ahorro.toPlainString(), fecha))
        val despues = nuevo.camino(ahorro, hoy)

        assertEquals(1, despues.pasos.count { it.estado == EstadoPaso.COMPLETADO })
        assertTrue(antes.pasos.map { it.fecha }.containsAll(despues.pasos.map { it.fecha }))
        assertEquals(fecha, despues.pasos.last().fecha)
        assertTrue(pendientes(despues).first().monto < camino.ahorroPorPaso)
        revisar(despues, (bd("2400") - ahorro).toPlainString())
    }

    @Test fun `con fecha ahorrar mucho completa varios pasos de una`() {
        val fecha = d(5, 1, 2027)
        val camino = crear(meta("2400", "0", fecha))
        val ahorro = camino.ahorroPorPaso * BigDecimal(3)

        val despues = camino.anotar(meta("2400", ahorro.toPlainString(), fecha)).camino(ahorro, hoy)

        assertEquals(3, despues.pasos.count { it.estado == EstadoPaso.COMPLETADO })
        assertEquals(4, actual(despues).numero)
    }

    @Test fun `con fecha un paso vencido solo se reparte cuando se vuelve a anotar`() {
        val fecha = d(5, 1, 2027)
        val camino = crear(meta("2400", "0", fecha))
        val tarde = d(2, 10, 2027)

        assertSame(camino, camino.anotar(meta("2400", "0", fecha), dia = tarde))
        val despues = camino.anotar(meta("2400", "100", fecha), dia = tarde).camino(bd("100"), tarde)

        assertTrue(pendientes(despues).none { it.fecha.isBefore(tarde) })
        assertEquals(fecha, despues.pasos.last().fecha)
        revisar(despues, "2300")
    }

    @Test fun `con fecha un retiro vuelve a abrir pasos`() {
        val fecha = d(5, 1, 2027)
        val camino = crear(meta("2400", "0", fecha))
        val dos = camino.ahorroPorPaso * BigDecimal(2)
        val conDos = camino.anotar(meta("2400", dos.toPlainString(), fecha))

        val retiro = conDos.anotar(meta("2400", camino.ahorroPorPaso.toPlainString(), fecha))
        val despues = retiro.camino(camino.ahorroPorPaso, hoy)

        assertEquals(1, despues.pasos.count { it.estado == EstadoPaso.COMPLETADO })
        revisar(despues, (bd("2400") - camino.ahorroPorPaso).toPlainString())
    }

    @Test fun `con fecha que no alcanza igual termina en la fecha y avisa`() {
        val p = crear(meta("20000", "0", d(12, 31))).camino(bd("0"), hoy)

        assertEquals(EstadoPlan.NO_LLEGA_A_FECHA, p.estado)
        assertEquals(d(12, 31), p.pasos.last().fecha)
        revisar(p, "20000")
    }

    @Test fun `sin fecha ahorrar de mas quita pasos`() {
        val camino = crear(meta("10000", "0"))
        val antes = camino.camino(bd("0"), hoy)

        val aportes = listOf(aporte(d(11, 8), "4000"))
        val despues = camino.anotar(meta("10000", "4000"), aportes = aportes).camino(bd("4000"), hoy)

        assertTrue(pendientes(despues).size < pendientes(antes).size)
        revisar(despues, "6000")
    }

    @Test fun `sin fecha atrasarse agrega pasos hacia adelante y conserva lo hecho`() {
        val camino = crear(meta("10000", "0"))
        val paso = camino.ahorroPorPaso
        val primero = camino.anotar(meta("10000", paso.toPlainString()), aportes = listOf(aporte(hoy, paso.toPlainString())))
        val hecho = primero.pasos.first()
        val tarde = d(6, 10, 2027)

        val despues = primero.anotar(meta("10000", (paso + bd("10")).toPlainString()), dia = tarde)
            .camino(paso + bd("10"), tarde)

        assertEquals(hecho.fecha, despues.pasos.first().fecha)
        assertEquals(EstadoPaso.COMPLETADO, despues.pasos.first().estado)
        assertFalse(actual(despues).fecha.isBefore(tarde))
        revisar(despues, (bd("10000") - paso - bd("10")).toPlainString())
    }

    @Test fun `al llegar al objetivo todo queda completado`() {
        val camino = crear(meta("2400", "0"))

        val p = camino.anotar(meta("2400", "2400")).camino(bd("2400"), hoy)

        assertEquals(EstadoPlan.LOGRADA, p.estado)
        assertTrue(p.pasos.all { it.estado == EstadoPaso.COMPLETADO })
    }

    @Test fun `si se edita el objetivo o la fecha el camino ya no sirve`() {
        val camino = crear(meta("2400", "0"))

        assertTrue(camino.vigentePara(meta("2400", "300")))
        assertFalse(camino.vigentePara(meta("3000", "300")))
        assertFalse(camino.vigentePara(meta("2400", "300", d(12, 31))))
    }

    @Test fun `sin margen muestra una ventana corta del camino`() {
        val camino = crear(meta("2400", "0"), ing = pronostico("2000"), gas = pronostico("2000"))
        val p = camino.camino(camino.ahorroPorPaso * BigDecimal(5), hoy)

        assertEquals(EstadoPlan.SIN_MARGEN, p.estado)
        assertTrue(p.pasos.size <= 6)
        assertEquals(1, p.pasos.first().numero)
        assertNull(p.fechaEstimada)
    }

    @Test fun `sin datos no se arma camino`() {
        assertNull(crearCamino(meta("2400", "0"), emptyList(), Pronostico.SIN_DATOS, pronostico("100"), null, hoy))
    }
}
