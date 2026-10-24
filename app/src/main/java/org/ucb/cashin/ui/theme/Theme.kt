package org.ucb.cashin.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val esquemaClaro = lightColorScheme(
    primary = Verde,
    onPrimary = Blanco,
    primaryContainer = VerdeClaro,
    onPrimaryContainer = VerdeOscuro,
    secondary = VerdeOscuro,
    onSecondary = Blanco,
    secondaryContainer = VerdeCategoria,
    onSecondaryContainer = VerdeOscuro,
    tertiary = Naranja,
    onTertiary = Blanco,
    tertiaryContainer = NaranjaClaro,
    onTertiaryContainer = VerdeOscuro,
    background = Crema,
    onBackground = VerdeOscuro,
    surface = Crema,
    onSurface = VerdeOscuro,
    surfaceVariant = CremaCampo,
    onSurfaceVariant = GrisTexto,
    surfaceContainer = CremaTarjeta,
    surfaceContainerLow = CremaTarjeta,
    surfaceContainerHigh = CremaCampo,
    outline = BordeCampo,
    outlineVariant = BordeCampo,
    error = RojoGasto,
    onError = Blanco,
)

val CashinShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),   // campos de texto y tarjetas
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/** Espaciados estándar: usar estos valores en vez de números sueltos. */
object Espacio {
    val xs = 4.dp
    val s = 8.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    /** Margen lateral de todas las pantallas. */
    val pantalla = 24.dp
}

/** Cashin usa solo tema claro (crema) en esta versión. */
@Composable
fun CashinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = esquemaClaro,
        typography = CashinTypography,
        shapes = CashinShapes,
        content = content,
    )
}
