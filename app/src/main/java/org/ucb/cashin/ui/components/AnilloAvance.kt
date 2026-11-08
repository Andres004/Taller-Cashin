package org.ucb.cashin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.ucb.cashin.R
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Verde
import org.ucb.cashin.ui.theme.VerdeClaro

/** Anillo con el porcentaje de avance en el centro. [progreso] va de 0 a 1. */
@Composable
fun AnilloAvance(
    progreso: Float,
    porcentaje: Int,
    modifier: Modifier = Modifier,
    tamano: Dp = 64.dp,
) {
    val descripcion = stringResource(R.string.metas_porcentaje_avance, porcentaje)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(tamano)
            .semantics { contentDescription = descripcion },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grosor = 9.dp.toPx()
            val esquina = Offset(grosor / 2, grosor / 2)
            val area = Size(size.width - grosor, size.height - grosor)
            drawArc(VerdeClaro, 0f, 360f, false, esquina, area, style = Stroke(grosor))
            drawArc(
                color = Verde,
                startAngle = -90f,
                sweepAngle = 360f * progreso.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = esquina,
                size = area,
                style = Stroke(grosor, cap = StrokeCap.Round),
            )
        }
        Text(
            text = "$porcentaje%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFCF3)
@Composable
private fun AnilloAvancePreview() = CashinTheme {
    AnilloAvance(progreso = 0.42f, porcentaje = 42)
}
