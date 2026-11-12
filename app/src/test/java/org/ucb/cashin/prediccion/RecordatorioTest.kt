package org.ucb.cashin.prediccion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class RecordatorioTest {

    // jueves 12 de noviembre de 2026
    private val jueves = LocalDate.of(2026, 11, 12)
    private val activo = AjusteRecordatorio(activo = true, hora = 21, minuto = 0)

    @Test
    fun `apagado no programa nada`() {
        assertNull(proximoAviso(jueves.atTime(10, 0), AjusteRecordatorio(activo = false)))
    }

    @Test
    fun `antes de la hora avisa hoy`() {
        assertEquals(jueves.atTime(21, 0), proximoAviso(jueves.atTime(10, 0), activo))
    }

    @Test
    fun `a la hora exacta o despues pasa a manana`() {
        assertEquals(jueves.plusDays(1).atTime(21, 0), proximoAviso(jueves.atTime(21, 0), activo))
        assertEquals(jueves.plusDays(1).atTime(21, 0), proximoAviso(jueves.atTime(23, 30), activo))
    }

    @Test
    fun `solo laborables salta el fin de semana`() {
        val laborables = activo.copy(soloLaborables = true)
        val viernesNoche = LocalDateTime.of(2026, 11, 13, 22, 0)
        assertEquals(LocalDate.of(2026, 11, 16).atTime(21, 0), proximoAviso(viernesNoche, laborables))
        val sabado = LocalDateTime.of(2026, 11, 14, 8, 0)
        assertEquals(LocalDate.of(2026, 11, 16).atTime(21, 0), proximoAviso(sabado, laborables))
    }

    @Test
    fun `respeta los minutos elegidos`() {
        assertEquals(jueves.atTime(20, 45), proximoAviso(jueves.atTime(20, 44), activo.copy(hora = 20, minuto = 45)))
    }

    @Test
    fun `no avisa si ya anoto hoy, si esta apagado o si es fin de semana con solo laborables`() {
        assertTrue(debeAvisar(jueves, jueves.minusDays(1), activo))
        assertTrue(debeAvisar(jueves, null, activo))
        assertFalse(debeAvisar(jueves, jueves, activo))
        assertFalse(debeAvisar(jueves, null, activo.copy(activo = false)))
        assertFalse(debeAvisar(LocalDate.of(2026, 11, 14), null, activo.copy(soloLaborables = true)))
    }

    @Test
    fun `el mensaje nombra la racha solo si sigue viva`() {
        assertEquals(MensajeRecordatorio(TipoAviso.RACHA_EN_RIESGO, 4), mensajeRecordatorio(jueves, jueves.minusDays(1), 4))
        assertEquals(TipoAviso.SIN_RACHA, mensajeRecordatorio(jueves, jueves.minusDays(3), 4).tipo)
        assertEquals(TipoAviso.SIN_RACHA, mensajeRecordatorio(jueves, jueves.minusDays(1), 0).tipo)
        assertEquals(TipoAviso.SIN_RACHA, mensajeRecordatorio(jueves, null, 0).tipo)
    }
}
