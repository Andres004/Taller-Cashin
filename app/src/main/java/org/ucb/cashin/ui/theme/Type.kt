package org.ucb.cashin.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.ucb.cashin.R

/** Poppins (res/font). Si el Figma usa otra fuente para títulos, se agrega aquí. */
val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

private fun estilo(tamano: Int, peso: FontWeight, alto: Int) = TextStyle(
    fontFamily = Poppins,
    fontWeight = peso,
    fontSize = tamano.sp,
    lineHeight = alto.sp,
)

/**
 * Escala tipográfica. Uso:
 *  - displaySmall: montos grandes (formulario de anotar, total del mes)
 *  - headlineMedium: título de pantalla
 *  - titleLarge / titleMedium: títulos de tarjetas y secciones
 *  - bodyLarge / bodyMedium: texto normal
 *  - labelLarge: botones · labelMedium: etiquetas de campos
 */
val CashinTypography = Typography(
    displayLarge = estilo(48, FontWeight.Bold, 56),
    displayMedium = estilo(40, FontWeight.Bold, 48),
    displaySmall = estilo(34, FontWeight.Bold, 42),
    headlineLarge = estilo(28, FontWeight.Bold, 36),
    headlineMedium = estilo(24, FontWeight.Bold, 32),
    headlineSmall = estilo(20, FontWeight.Bold, 28),
    titleLarge = estilo(18, FontWeight.Bold, 26),
    titleMedium = estilo(16, FontWeight.Medium, 24),
    titleSmall = estilo(14, FontWeight.Medium, 20),
    bodyLarge = estilo(16, FontWeight.Normal, 24),
    bodyMedium = estilo(14, FontWeight.Normal, 20),
    bodySmall = estilo(12, FontWeight.Normal, 16),
    labelLarge = estilo(16, FontWeight.Medium, 22),
    labelMedium = estilo(14, FontWeight.Medium, 18),
    labelSmall = estilo(11, FontWeight.Medium, 14),
)
