package org.ucb.cashin.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta de Cashin (tomada de los prototipos del Figma "Cashin-Mobile").
// REGLA: ninguna pantalla escribe Color(0x...) directamente; usa MaterialTheme.colorScheme
// o CashinColores (abajo). Si falta un color, se agrega aquí en un PR.

val Crema = Color(0xFFFCF7EA)          // fondo general
val CremaCampo = Color(0xFFFDF9EE)     // fondo de campos de texto
val CremaTarjeta = Color(0xFFFFFCF3)   // tarjetas sobre el fondo crema
val BordeCampo = Color(0xFFCCC8BE)

val VerdeOscuro = Color(0xFF2B5333)    // textos y títulos
val VerdeProfundo = Color(0xFF075D36)  // encabezados destacados
val Verde = Color(0xFF6D9A5A)          // botones principales
val VerdeMedio = Color(0xFF56A34D)
val VerdeClaro = Color(0xFFB4D5AA)     // fondos suaves, barras de progreso
val VerdeCategoria = Color(0xFF8DBE84)

val Naranja = Color(0xFFE08A2C)        // acento (avatar, XP, rachas)
val NaranjaClaro = Color(0xFFFDE7CF)

val RojoGasto = Color(0xFFC0473A)      // montos de gasto y errores
val VerdeIngreso = Color(0xFF2E7D32)   // montos de ingreso

val GrisTexto = Color(0xFF6B6F68)      // textos secundarios
val Blanco = Color(0xFFFFFFFF)
val Negro = Color(0xFF1C1C1A)

/** Colores con significado propio de Cashin que no existen en Material. */
object CashinColores {
    val gasto = RojoGasto
    val ingreso = VerdeIngreso
    val xp = Naranja
    val racha = Naranja
    val fondoAvatar = NaranjaClaro
    val progreso = VerdeClaro
    val progresoFondo = CremaTarjeta
}
