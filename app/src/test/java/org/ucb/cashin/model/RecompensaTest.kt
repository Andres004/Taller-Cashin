package org.ucb.cashin.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecompensaTest {
    // Domingo 8 de noviembre de 2026.
    private val hoy = LocalDate.of(2026, 11, 8)

    private fun perfil(xp: Int, racha: Int, ultimo: LocalDate?) =
        Perfil(id = "u", nombre = "Demo", xp = xp, rachaActual = racha, ultimoRegistro = ultimo)

    @Test
    fun `la primera anotacion del dia avanza la racha`() {
        val r = calcularRecompensa(perfil(100, 3, hoy.minusDays(1)), perfil(110, 4, hoy), hoy, TipoRegistro.GASTO, ganoXp = true)
        assertTrue(r.primeraDelDia)
        assertEquals(10, r.xpGanado)
        assertEquals(4, r.racha)
    }

    @Test
    fun `una segunda anotacion no repite la celebracion de racha`() {
        val r = calcularRecompensa(perfil(110, 4, hoy), perfil(120, 4, hoy), hoy, TipoRegistro.INGRESO, ganoXp = true)
        assertFalse(r.primeraDelDia)
        assertEquals(10, r.xpGanado)
    }

    @Test
    fun `despues de cinco anotaciones ya no hay XP`() {
        val r = calcularRecompensa(perfil(150, 4, hoy), perfil(150, 4, hoy), hoy, TipoRegistro.GASTO, ganoXp = false)
        assertEquals(0, r.xpGanado)
    }

    @Test
    fun `sin perfil usa el respaldo y no festeja la racha`() {
        val r = calcularRecompensa(null, null, hoy, TipoRegistro.GASTO, ganoXp = true)
        assertEquals(XP_POR_REGISTRO, r.xpGanado)
        assertFalse(r.primeraDelDia)
        assertEquals(0, r.racha)
    }

    @Test
    fun `la semana marca solo los dias de la racha`() {
        val semana = semanaDeRacha(hoy, racha = 3, anotoHoy = true)
        assertEquals(7, semana.size)
        assertEquals(LocalDate.of(2026, 11, 2), semana.first().fecha)
        assertEquals(hoy, semana.last().fecha)
        assertEquals(
            listOf(
                EstadoDiaRacha.SIN_ANOTAR, EstadoDiaRacha.SIN_ANOTAR, EstadoDiaRacha.SIN_ANOTAR, EstadoDiaRacha.SIN_ANOTAR,
                EstadoDiaRacha.LOGRADO, EstadoDiaRacha.LOGRADO, EstadoDiaRacha.LOGRADO,
            ),
            semana.map { it.estado },
        )
    }

    @Test
    fun `si aun no anoto hoy, hoy queda pendiente y ayer sigue en la racha`() {
        val miercoles = LocalDate.of(2026, 11, 4)
        val semana = semanaDeRacha(miercoles, racha = 2, anotoHoy = false)
        assertEquals(
            listOf(
                EstadoDiaRacha.LOGRADO, EstadoDiaRacha.LOGRADO, EstadoDiaRacha.PENDIENTE_HOY, EstadoDiaRacha.FUTURO,
                EstadoDiaRacha.FUTURO, EstadoDiaRacha.FUTURO, EstadoDiaRacha.FUTURO,
            ),
            semana.map { it.estado },
        )
    }

    @Test
    fun `sin racha no se marca ningun dia`() {
        val semana = semanaDeRacha(hoy, racha = 0, anotoHoy = false)
        assertTrue(semana.none { it.estado == EstadoDiaRacha.LOGRADO })
        assertEquals(EstadoDiaRacha.PENDIENTE_HOY, semana.last().estado)
    }
}
