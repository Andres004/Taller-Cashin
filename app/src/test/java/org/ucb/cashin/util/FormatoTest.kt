package org.ucb.cashin.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class FormatoTest {

    @Test fun `formatea montos en bolivianos`() {
        assertEquals("Bs 1.250,50", Formato.monto(BigDecimal("1250.5")))
        assertEquals("Bs 20,00", Formato.monto(BigDecimal("20")))
        assertEquals("Bs 0,99", Formato.monto(BigDecimal("0.99")))
    }

    @Test fun `formatea dolares y signo`() {
        assertEquals("\$us 10,00", Formato.monto(BigDecimal("10"), "USD"))
        assertEquals("- Bs 20,00", Formato.montoConSigno(BigDecimal("20"), esGasto = true))
        assertEquals("+ Bs 20,00", Formato.montoConSigno(BigDecimal("20"), esGasto = false))
    }

    @Test fun `parsea lo que escribe el usuario`() {
        assertEquals(BigDecimal("20"), Formato.parsearMonto("20"))
        assertEquals(BigDecimal("20.5"), Formato.parsearMonto("20,5"))
        assertEquals(BigDecimal("20.50"), Formato.parsearMonto("20.50"))
        assertEquals(BigDecimal("1250.50"), Formato.parsearMonto("1.250,50"))
        assertEquals(BigDecimal("1250000"), Formato.parsearMonto("1.250.000"))
    }

    @Test fun `rechaza montos invalidos`() {
        assertNull(Formato.parsearMonto(""))
        assertNull(Formato.parsearMonto("abc"))
        assertNull(Formato.parsearMonto("20,555"))
    }
}
