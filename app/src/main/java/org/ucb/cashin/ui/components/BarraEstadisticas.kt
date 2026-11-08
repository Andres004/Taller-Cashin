package org.ucb.cashin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.ucb.cashin.R
import org.ucb.cashin.ui.theme.Blanco
import org.ucb.cashin.ui.theme.BordeCampo
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.CremaTarjeta
import org.ucb.cashin.ui.theme.Naranja
import org.ucb.cashin.ui.theme.NaranjaRelieve

private val FORMA_CHIP = RoundedCornerShape(15.dp)

/** Racha y nivel, siempre a la vista. [resaltarNivel] pinta el chip de naranja al subir de nivel. */
@Composable
fun BarraEstadisticas(
    racha: Int,
    rachaEnRiesgo: Boolean,
    nivel: Int,
    xp: Int,
    modifier: Modifier = Modifier,
    resaltarNivel: Boolean = false,
) {
    val descripcionRiesgo = stringResource(R.string.inicio_racha_en_riesgo)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        Chip(
            texto = stringResource(R.string.inicio_chip_racha, racha),
            colorTexto = if (rachaEnRiesgo) CashinColores.gasto else NaranjaRelieve,
            fondo = CremaTarjeta,
            borde = if (rachaEnRiesgo) CashinColores.gasto else BordeCampo,
            modifier = if (rachaEnRiesgo) Modifier.semantics { contentDescription = descripcionRiesgo } else Modifier,
        )
        Chip(
            texto = stringResource(R.string.inicio_chip_nivel, nivel, xp),
            colorTexto = if (resaltarNivel) Blanco else NaranjaRelieve,
            fondo = if (resaltarNivel) Naranja else CremaTarjeta,
            borde = if (resaltarNivel) Naranja else BordeCampo,
            icono = true,
        )
    }
}

@Composable
private fun Chip(
    texto: String,
    colorTexto: Color,
    fondo: Color,
    borde: Color,
    modifier: Modifier = Modifier,
    icono: Boolean = false,
) {
    Surface(shape = FORMA_CHIP, color = fondo, border = BorderStroke(1.5.dp, borde), modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .height(30.dp)
                .padding(horizontal = 9.dp),
        ) {
            if (icono) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (colorTexto == Blanco) Blanco else Naranja,
                    modifier = Modifier.size(15.dp),
                )
            }
            Text(
                text = texto,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = colorTexto,
                maxLines = 1,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun BarraEstadisticasPreview() = CashinTheme {
    BarraEstadisticas(racha = 4, rachaEnRiesgo = false, nivel = 2, xp = 230)
}
