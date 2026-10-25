package org.ucb.cashin.util

/** Reglas de validación compartidas por todos los formularios (mismas reglas que la base de datos). */
object Validaciones {

    const val MIN_CONTRASENA = 8
    const val MAX_NOMBRE = 80

    const val MIN_CODIGO = 6
    const val MAX_CODIGO = 10

    private val regexCorreo = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun correoValido(correo: String): Boolean = regexCorreo.matches(correo.trim())

    fun contrasenaValida(contrasena: String): Boolean = contrasena.length >= MIN_CONTRASENA

    fun nombreValido(nombre: String): Boolean = nombre.trim().length in 1..MAX_NOMBRE

    fun codigoValido(codigo: String): Boolean {
        val limpio = codigo.trim()
        return limpio.length in MIN_CODIGO..MAX_CODIGO && limpio.all { it.isDigit() }
    }
}
