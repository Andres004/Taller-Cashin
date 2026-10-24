package org.ucb.cashin.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.ucb.cashin.ui.components.esMontoParcialValido

class ValidacionesTest {

    @Test fun correos() {
        assertTrue(Validaciones.correoValido("ana@correo.com"))
        assertTrue(Validaciones.correoValido("  carlos.p@ucb.edu.bo "))
        assertFalse(Validaciones.correoValido("ana@"))
        assertFalse(Validaciones.correoValido("ana correo.com"))
    }

    @Test fun contrasenasYCodigos() {
        assertTrue(Validaciones.contrasenaValida("12345678"))
        assertFalse(Validaciones.contrasenaValida("1234567"))
        assertTrue(Validaciones.codigoValido("123456"))
        assertFalse(Validaciones.codigoValido("12345"))
        assertFalse(Validaciones.codigoValido("12a456"))
    }

    @Test fun montoMientrasSeEscribe() {
        listOf("", "2", "20", "20,", "20,5", "20.50").forEach { assertTrue(it, esMontoParcialValido(it)) }
        listOf("a", "20,555", "1,2,3", "-5").forEach { assertFalse(it, esMontoParcialValido(it)) }
    }
}
