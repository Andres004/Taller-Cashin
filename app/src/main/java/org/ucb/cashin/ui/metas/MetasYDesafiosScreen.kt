package org.ucb.cashin.ui.metas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import org.ucb.cashin.R
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.desafios.DesafiosScreen

/** Las dos pestañas de arriba. */
private enum class PestanaMetas(val texto: Int, val icono: ImageVector) {
    METAS(R.string.metas_pestana_metas, Icons.Outlined.Savings),
    DESAFIOS(R.string.metas_pestana_desafios, Icons.Outlined.EmojiEvents),
}

/**
 * Pestaña "Metas" de la barra inferior: arriba elige entre las metas de ahorro y los desafíos.
 * Así los desafíos están a un toque y no escondidos en el Perfil.
 * La pestaña elegida se recuerda al abrir una meta y volver.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetasYDesafiosScreen(
    onNueva: () -> Unit,
    onMetaClick: (metaId: String) -> Unit,
) {
    var seleccionada by rememberSaveable { mutableIntStateOf(PestanaMetas.METAS.ordinal) }

    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(titulo = stringResource(R.string.nav_metas))
        PrimaryTabRow(
            selectedTabIndex = seleccionada,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.secondary,
        ) {
            PestanaMetas.entries.forEach { pestana ->
                Tab(
                    selected = seleccionada == pestana.ordinal,
                    onClick = { seleccionada = pestana.ordinal },
                    text = { Text(stringResource(pestana.texto), style = MaterialTheme.typography.titleSmall) },
                    icon = { Icon(imageVector = pestana.icono, contentDescription = null) },
                    selectedContentColor = MaterialTheme.colorScheme.secondary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when (PestanaMetas.entries[seleccionada]) {
            PestanaMetas.METAS -> MetasScreen(onNueva = onNueva, onMetaClick = onMetaClick, mostrarTitulo = false)
            PestanaMetas.DESAFIOS -> DesafiosScreen(onAtras = null)
        }
    }
}
