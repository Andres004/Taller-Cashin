package org.ucb.cashin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.ucb.cashin.R
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio

/**
 * Indicador de avance para formularios en varios pasos ("1 Correo ── 2 Código").
 * - Paso ya hecho: círculo verde claro con un check.
 * - Paso actual: círculo verde oscuro con su número.
 * - Paso pendiente: círculo con borde y su número.
 * [pasoActual] empieza en 0. Para TalkBack se lee como "Paso 2 de 2: Código".
 */
@Composable
fun IndicadorPasos(
    pasos: List<String>,
    pasoActual: Int,
    modifier: Modifier = Modifier,
) {
    val descripcion = stringResource(
        R.string.indicador_pasos_descripcion,
        pasoActual + 1,
        pasos.size,
        pasos.getOrElse(pasoActual) { "" },
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = descripcion },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.s),
    ) {
        pasos.forEachIndexed { indice, nombre ->
            val hecho = indice < pasoActual
            val actual = indice == pasoActual

            CirculoPaso(numero = indice + 1, hecho = hecho, actual = actual)
            Text(
                text = nombre,
                style = MaterialTheme.typography.labelMedium,
                color = if (actual) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Línea entre un paso y el siguiente: verde si ese tramo ya se completó.
            if (indice < pasos.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 2.dp,
                    color = if (hecho) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

@Composable
private fun CirculoPaso(numero: Int, hecho: Boolean, actual: Boolean) {
    val fondo = when {
        hecho -> MaterialTheme.colorScheme.primaryContainer
        actual -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.background
    }
    val contenido = when {
        hecho -> MaterialTheme.colorScheme.onPrimaryContainer
        actual -> MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = CircleShape,
        color = fondo,
        contentColor = contenido,
        border = if (hecho || actual) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.size(28.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (hecho) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            } else {
                Text(text = numero.toString(), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun IndicadorPasosPreview() = CashinTheme {
    IndicadorPasos(pasos = listOf("Correo", "Código"), pasoActual = 1)
}