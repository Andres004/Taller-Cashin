package org.ucb.cashin.prediccion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.ucb.cashin.model.AbonoDeuda
import org.ucb.cashin.model.AumentoDeuda
import org.ucb.cashin.model.EstadoDeuda
import org.ucb.cashin.model.ResumenDeuda
import org.ucb.cashin.model.TipoDeuda
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class DeudasTest {

    private val hoy = LocalDate.of(2026, 11, 11)
    private val inicio = LocalDate.of(2026, 11, 1)
    private fun bd(t: String) = BigDecimal(t)
    private fun dia(d: Int) = LocalDate.of(2026, 11, d)
    private fun hora(s: Long) = Instant.ofEpochSecond(1_800_000_000 + s)

    private fun aumento(id: String, monto: String, d: Int, s: Long = 0) = AumentoDeuda(id, "x", bd(monto), dia(d), null, hora(s))
    private fun abono(id: String, monto: String, d: Int, s: Long = 0) = AbonoDeuda(id, "x", bd(monto), dia(d), null, hora(s))

    /** Amy: le prestaste 40 el 1, 60 más el 5 y te devolvió 30 el 8. */
    private val amy = movimientosDe(bd("40"), inicio, "Almuerzo", listOf(aumento("au1", "60", 5)), listOf(abono("ab1", "30", 8)))

    // ---------- línea de tiempo

    @Test
    fun `la linea de tiempo lleva el saldo despues de cada movimiento`() {
        val linea = lineaDeTiempo(amy)
        assertEquals(listOf(TipoMovimiento.INICIO, TipoMovimiento.AUMENTO, TipoMovimiento.ABONO), linea.map { it.movimiento.tipo })
        assertEquals(listOf(bd("40"), bd("100"), bd("70")), linea.map { it.saldo })
    }

    @Test
    fun `el mismo dia va primero el inicio, luego aumentos y al final abonos`() {
        val movs = movimientosDe(bd("40"), inicio, null, listOf(aumento("au", "10", 1, s = 50)), listOf(abono("ab", "45", 1, s = 10)))
        val linea = lineaDeTiempo(movs)
        assertEquals(listOf(TipoMovimiento.INICIO, TipoMovimiento.AUMENTO, TipoMovimiento.ABONO), linea.map { it.movimiento.tipo })
        assertEquals(bd("5"), linea.last().saldo)
    }

    @Test
    fun `dos abonos el mismo dia se ordenan por cuando se anotaron`() {
        val movs = movimientosDe(bd("100"), inicio, null, emptyList(), listOf(abono("b", "20", 5, s = 90), abono("a", "10", 5, s = 30)))
        assertEquals(listOf(null, "a", "b"), lineaDeTiempo(movs).map { it.movimiento.id })
    }

    @Test
    fun `solo se puede borrar el ultimo movimiento y nunca el inicio`() {
        assertEquals("ab1", ultimoBorrable(lineaDeTiempo(amy))?.id)
        val soloInicio = movimientosDe(bd("40"), inicio, null, emptyList(), emptyList())
        assertNull(ultimoBorrable(lineaDeTiempo(soloInicio)))
    }

    // ---------- abonos

    @Test
    fun `abono valido`() {
        assertTrue(validarAbono(amy, bd("70"), hoy, inicio, hoy).valido)
    }

    @Test
    fun `abono mayor a lo que falta`() {
        val v = validarAbono(amy, bd("90"), hoy, inicio, hoy)
        assertEquals(ProblemaMovimiento.SUPERA_PENDIENTE, v.problema)
        assertEquals(bd("70"), v.limite)
    }

    @Test
    fun `abono en una fecha en la que todavia no se debia tanto`() {
        // El 3 nov solo se debían 40; 50 ese día deja el saldo en -10 aunque al final cuadre.
        val v = validarAbono(amy, bd("50"), dia(3), inicio, hoy)
        assertEquals(ProblemaMovimiento.NO_SE_DEBIA_TANTO, v.problema)
        assertEquals(bd("40"), v.limite)
    }

    @Test
    fun `abono con fecha antes del inicio o futura`() {
        assertEquals(ProblemaMovimiento.FECHA_ANTES_DEL_INICIO, validarAbono(amy, bd("10"), dia(1).minusDays(1), inicio, hoy).problema)
        assertEquals(ProblemaMovimiento.FECHA_FUTURA, validarAbono(amy, bd("10"), hoy.plusDays(1), inicio, hoy).problema)
    }

    @Test
    fun `montos invalidos`() {
        listOf(null, bd("0"), bd("-5"), bd("1.234")).forEach {
            assertEquals(it.toString(), ProblemaMovimiento.MONTO_INVALIDO, validarAbono(amy, it, hoy, inicio, hoy).problema)
        }
        assertTrue(montoValido(bd("12.50")))
        assertTrue(montoValido(bd("12.500")))
    }

    @Test
    fun `aumento valido y con fecha futura`() {
        assertTrue(validarAumento(bd("50"), hoy, inicio, hoy).valido)
        assertEquals(ProblemaMovimiento.FECHA_FUTURA, validarAumento(bd("50"), hoy.plusDays(2), inicio, hoy).problema)
    }

    // ---------- editar la deuda

    @Test
    fun `bajar el monto inicial por debajo de lo que la historia permite`() {
        // Con 5 de inicio: 5 → 65 → 35: cuadra. Con pagos de 100 no.
        assertTrue(validarEdicion(amy, bd("5"), inicio, hoy).problema == null)
        val pagada = movimientosDe(bd("40"), inicio, null, listOf(aumento("au", "60", 5)), listOf(abono("ab", "100", 8)))
        val v = validarEdicion(pagada, bd("30"), inicio, hoy)
        assertEquals(ProblemaEdicion.MENOR_QUE_LO_DEVUELTO, v.problema)
        assertEquals(bd("40"), v.limite)
    }

    @Test
    fun `mover el inicio despues de un movimiento no se puede`() {
        assertEquals(ProblemaEdicion.MOVIMIENTO_ANTES_DEL_INICIO, validarEdicion(amy, bd("40"), dia(6), hoy).problema)
        assertEquals(ProblemaEdicion.INICIO_FUTURO, validarEdicion(amy, bd("40"), hoy.plusDays(1), hoy).problema)
    }

    // ---------- vencimiento

    @Test
    fun `avisos de vencimiento`() {
        assertEquals(Vencimiento(AvisoVencimiento.SIN_FECHA), vencimientoDe(null, false, hoy))
        assertEquals(Vencimiento(AvisoVencimiento.VENCE_HOY), vencimientoDe(hoy, false, hoy))
        assertEquals(Vencimiento(AvisoVencimiento.VENCE_PRONTO, 3), vencimientoDe(dia(14), false, hoy))
        assertEquals(Vencimiento(AvisoVencimiento.VENCE_PRONTO, 7), vencimientoDe(dia(18), false, hoy))
        assertEquals(Vencimiento(AvisoVencimiento.NORMAL, 8), vencimientoDe(dia(19), false, hoy))
        assertEquals(Vencimiento(AvisoVencimiento.VENCIDA, 6), vencimientoDe(dia(5), false, hoy))
        assertNull(vencimientoDe(dia(5), true, hoy))
    }

    // ---------- misma persona

    private fun resumen(id: String, nombre: String, tipo: TipoDeuda, estado: EstadoDeuda = EstadoDeuda.PENDIENTE, pendiente: String = "10") =
        ResumenDeuda(id, "u", tipo, nombre, bd("100"), null, estado, bd(pendiente), fechaInicio = inicio)

    @Test
    fun `reconoce el mismo nombre escrito distinto`() {
        assertEquals("amy r.", normalizarNombre("  Ámy   R. "))
        val deudas = listOf(resumen("1", "Amy", TipoDeuda.ME_DEBEN))
        assertEquals("1", deudaAbiertaCon("amy ", TipoDeuda.ME_DEBEN, deudas)?.deudaId)
        assertEquals("1", deudaAbiertaCon("ÁMY", TipoDeuda.ME_DEBEN, deudas)?.deudaId)
    }

    @Test
    fun `no sugiere deudas pagadas, de otro tipo, ella misma ni nombres vacios`() {
        val deudas = listOf(
            resumen("1", "Amy", TipoDeuda.ME_DEBEN, EstadoDeuda.PAGADA),
            resumen("2", "Amy", TipoDeuda.DEBO),
        )
        assertNull(deudaAbiertaCon("Amy", TipoDeuda.ME_DEBEN, deudas))
        assertNull(deudaAbiertaCon("Amy", TipoDeuda.DEBO, deudas, excluirId = "2"))
        assertNull(deudaAbiertaCon("   ", TipoDeuda.DEBO, deudas))
    }

    @Test
    fun `avisa la deuda con la misma persona en el otro sentido`() {
        val meDebe = resumen("1", "Amy", TipoDeuda.ME_DEBEN)
        val leDebo = resumen("2", "amy", TipoDeuda.DEBO, pendiente = "20")
        assertEquals("2", deudaContraria(meDebe, listOf(meDebe, leDebo))?.deudaId)
    }

    @Test
    fun `total pendiente solo cuenta las abiertas del tipo`() {
        val deudas = listOf(
            resumen("1", "A", TipoDeuda.DEBO, pendiente = "200"),
            resumen("2", "B", TipoDeuda.DEBO, pendiente = "20"),
            resumen("3", "C", TipoDeuda.DEBO, EstadoDeuda.PAGADA, pendiente = "0"),
            resumen("4", "D", TipoDeuda.ME_DEBEN, pendiente = "70"),
        )
        assertEquals(bd("220"), totalPendiente(deudas, TipoDeuda.DEBO))
        assertEquals(bd("70"), totalPendiente(deudas, TipoDeuda.ME_DEBEN))
    }
}
