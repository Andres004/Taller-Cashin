package org.ucb.cashin.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecursosCashiTest {

    private val nombres = RecursoCashi.entries.map { it.archivoAnimado }

    @Test
    fun `cada momento tiene su propio archivo`() {
        assertEquals(nombres.size, nombres.toSet().size)
    }

    @Test
    fun `los nombres son validos para un recurso de Android`() {
        val valido = Regex("[a-z][a-z0-9_]*")
        nombres.forEach { assertTrue("Nombre inválido: $it", valido.matches(it)) }
    }
}
