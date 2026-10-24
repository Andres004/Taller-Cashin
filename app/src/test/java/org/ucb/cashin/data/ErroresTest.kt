package org.ucb.cashin.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.ucb.cashin.model.ErrorApp
import java.io.IOException

class ErroresTest {

    @Test fun traduceMensajesDeSupabase() {
        assertEquals(ErrorApp.CREDENCIALES_INVALIDAS, traducirError(RuntimeException("invalid_credentials: Invalid login credentials")))
        assertEquals(ErrorApp.CORREO_YA_REGISTRADO, traducirError(RuntimeException("User already registered")))
        assertEquals(ErrorApp.CORREO_NO_CONFIRMADO, traducirError(RuntimeException("Email not confirmed")))
        assertEquals(ErrorApp.CODIGO_INVALIDO, traducirError(RuntimeException("Token has expired or is invalid")))
        assertEquals(ErrorApp.SIN_PERMISO, traducirError(RuntimeException("new row violates row-level security policy")))
        assertEquals(ErrorApp.DATO_INVALIDO, traducirError(RuntimeException("El retiro supera lo ahorrado en la meta")))
    }

    @Test fun detectaFaltaDeConexion() {
        assertEquals(ErrorApp.SIN_CONEXION, traducirError(RuntimeException("fallo", IOException("Unable to resolve host"))))
    }

    @Test fun desconocidoPorDefecto() {
        assertEquals(ErrorApp.DESCONOCIDO, traducirError(RuntimeException("algo raro")))
    }
}
