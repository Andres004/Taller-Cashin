package org.ucb.cashin.prediccion

import org.junit.Assert.assertEquals
import org.junit.Test

class ElegirConsejoTest {
    private val xp = MensajeAvatar(TipoMensaje.XP_PARA_NIVEL, Animo.FELIZ, dias = 80)
    private val normal = MensajeAvatar(TipoMensaje.GASTO_NORMAL, Animo.FELIZ)
    private val ingresos = MensajeAvatar(TipoMensaje.INGRESOS_DEL_MES, Animo.CELEBRANDO)

    @Test
    fun `cada visita muestra un mensaje distinto y luego vuelve a empezar`() {
        val lista = listOf(normal, ingresos, xp)
        assertEquals(listOf(normal, ingresos, xp, normal), (0..3).map { elegirConsejo(lista, it, entroIngreso = false) })
    }

    @Test
    fun `si entro un ingreso Cashi reacciona a eso`() {
        assertEquals(ingresos, elegirConsejo(listOf(normal, xp, ingresos), turno = 0, entroIngreso = true))
    }

    @Test
    fun `sin mensaje de ingresos sigue el turno normal`() {
        assertEquals(xp, elegirConsejo(listOf(normal, xp), turno = 1, entroIngreso = true))
    }
}
