package org.ucb.cashin.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.ucb.cashin.ui.theme.CashinColores

/** Barra de avance (metas, desafíos, XP del nivel). [progreso] va de 0 a 1. */
@Composable
fun BarraProgreso(
    progreso: Float,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { progreso.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(50)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = CashinColores.progreso,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}
