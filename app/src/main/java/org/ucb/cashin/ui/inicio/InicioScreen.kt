package org.ucb.cashin.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.ucb.cashin.R
import org.ucb.cashin.ui.components.AvatarBurbuja
import org.ucb.cashin.ui.components.CashinTarjeta
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.Emocion
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio

/**
 * Pantalla de Inicio — versión Sprint 1 (solo bienvenida).
 * Sprint 2 (HU-13): saludo con el nombre del perfil, totales del mes (v_totales_mes),
 * racha, XP y últimas 5 anotaciones. Sprint 4 (HU-21): consejo del día de la IA.
 */
@Composable
fun InicioScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(titulo = stringResource(R.string.nav_inicio))
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.pantalla, vertical = Espacio.s),
            verticalArrangement = Arrangement.spacedBy(Espacio.m),
        ) {
            Text(
                text = stringResource(R.string.inicio_saludo),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            AvatarBurbuja(
                mensaje = stringResource(R.string.inicio_avatar_bienvenida),
                emocion = Emocion.FELIZ,
            )
            CashinTarjeta {
                Text(
                    text = stringResource(R.string.en_construccion_mensaje, stringResource(R.string.inicio_construccion), "Sprint 2", "HU-13"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun InicioPreview() = CashinTheme { InicioScreen() }
