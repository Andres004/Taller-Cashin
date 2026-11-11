package org.ucb.cashin.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio

/** Un mes del gráfico: sus dos barras y el texto de abajo. [destacado] va en negrita. */
data class BarraMes(
    val etiqueta: String,
    val ingresos: Float,
    val gastos: Float,
    val destacado: Boolean = false,
)

/** Cuánto se atrasa cada mes respecto al anterior en la animación (fracción del total). */
private const val RETRASO_POR_MES = 0.08f

/**
 * Barras de ingresos y gastos por mes, dibujadas con Canvas. Al aparecer crecen desde abajo,
 * un mes después del otro. [claveAnimacion]: cuando cambia, vuelven a crecer.
 */
@Composable
fun GraficoBarras(
    barras: List<BarraMes>,
    descripcion: String,
    modifier: Modifier = Modifier,
    colorIngreso: Color = MaterialTheme.colorScheme.primary,
    colorGasto: Color = CashinColores.gasto,
    alto: Dp = 120.dp,
    claveAnimacion: Any? = null,
) {
    val progreso = remember { Animatable(0f) }
    LaunchedEffect(claveAnimacion) {
        progreso.snapTo(0f)
        progreso.animateTo(1f, tween(durationMillis = 700, easing = FastOutSlowInEasing))
    }
    val maximo = barras.maxOfOrNull { maxOf(it.ingresos, it.gastos) }?.takeIf { it > 0f } ?: 1f

    Column(modifier = modifier.semantics { contentDescription = descripcion }) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(alto),
        ) {
            if (barras.isEmpty()) return@Canvas
            val anchoGrupo = size.width / barras.size
            val anchoBarra = minOf(16.dp.toPx(), anchoGrupo * 0.32f)
            val separacion = 3.dp.toPx()
            val radio = CornerRadius(6.dp.toPx())
            val tramo = 1f - RETRASO_POR_MES * (barras.size - 1)

            barras.forEachIndexed { i, barra ->
                val avance = ((progreso.value - i * RETRASO_POR_MES) / tramo).coerceIn(0f, 1f)
                val inicioX = anchoGrupo * i + (anchoGrupo - anchoBarra * 2 - separacion) / 2f
                listOf(barra.ingresos to colorIngreso, barra.gastos to colorGasto).forEachIndexed { j, (valor, color) ->
                    val altoBarra = size.height * (valor / maximo) * avance
                    if (altoBarra > 0f) {
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(inicioX + j * (anchoBarra + separacion), size.height - altoBarra),
                            size = Size(anchoBarra, altoBarra),
                            cornerRadius = radio,
                        )
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = Espacio.xs)) {
            barras.forEach { barra ->
                Text(
                    text = barra.etiqueta,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (barra.destacado) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFCF3)
@Composable
private fun GraficoBarrasPreview() = CashinTheme {
    GraficoBarras(
        barras = listOf(
            BarraMes("Jun", 3000f, 2650f),
            BarraMes("Jul", 3000f, 2820f),
            BarraMes("Ago", 3200f, 2300f),
            BarraMes("Sep", 3000f, 2850f),
            BarraMes("Oct", 3000f, 2212f),
            BarraMes("Nov", 3200f, 2350f, destacado = true),
        ),
        descripcion = "Ingresos y gastos de los últimos 6 meses",
        modifier = Modifier.padding(Espacio.m),
    )
}
