package org.ucb.cashin.prediccion

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class MensajesAvatarTest {

    private fun estimacion(
        alcanzada: Boolean = false,
        fuente: FuenteAhorro = FuenteAhorro.CAPACIDAD,
        meses: Int? = 5,
        llegaAntes: Boolean? = null,
    ) = EstimacionMeta(
        alcanzada = alcanzada,
        faltante = BigDecimal("2200.00"),
        ahorroMensual = BigDecimal("440.00"),
        fuente = fuente,
        meses = meses,
        fechaEstimada = meses?.let { LocalDate.of(2027, 3, 15) },
        llegaAntesDeLaFecha = llegaAntes,
        ahorroNecesarioPorMes = llegaAntes?.let { BigDecimal("420.00") },
        confianza = Confianza.MEDIA,
    )

    private val pronostico = Pronostico(
        esperado = BigDecimal("1000.00"),
        minimo = BigDecimal("900.00"),
        maximo = BigDecimal("1100.00"),
        confianza = Confianza.MEDIA,
        mesesUsados = 4,
    )

    @Test fun `meta alcanzada celebra`() {
        val m = mensajeParaMeta(estimacion(alcanzada = true))
        assertEquals(TipoMensaje.META_ALCANZADA, m.tipo)
        assertEquals(Animo.CELEBRANDO, m.animo)
    }

    @Test fun `meta alcanzable a tiempo esta feliz`() {
        val m = mensajeParaMeta(estimacion(llegaAntes = true))
        assertEquals(TipoMensaje.META_ALCANZABLE, m.tipo)
        assertEquals(Animo.FELIZ, m.animo)
        assertEquals(5, m.meses)
    }

    @Test fun `meta que no llega a la fecha dice cuanto hace falta`() {
        val m = mensajeParaMeta(estimacion(llegaAntes = false))
        assertEquals(TipoMensaje.META_NO_LLEGA, m.tipo)
        assertEquals(Animo.PREOCUPADO, m.animo)
        assertEquals(BigDecimal("420.00"), m.monto)
    }

    @Test fun `ritmo negativo preocupa`() {
        assertEquals(TipoMensaje.META_RITMO_NEGATIVO, mensajeParaMeta(estimacion(meses = null)).tipo)
    }

    @Test fun `sin datos pide anotar mas`() {
        val m = mensajeParaMeta(estimacion(fuente = FuenteAhorro.SIN_DATOS, meses = null))
        assertEquals(TipoMensaje.SIN_DATOS, m.tipo)
        assertEquals(Animo.NEUTRAL, m.animo)
    }

    @Test fun `gastar mas que el maximo pronosticado preocupa`() {
        val m = mensajeParaProyeccion(BigDecimal("1200.00"), pronostico)
        assertEquals(TipoMensaje.GASTO_MAS_RAPIDO, m.tipo)
        assertEquals(Animo.PREOCUPADO, m.animo)
    }

    @Test fun `gastar dentro de lo normal esta bien`() {
        assertEquals(TipoMensaje.GASTO_NORMAL, mensajeParaProyeccion(BigDecimal("1050.00"), pronostico).tipo)
    }
}