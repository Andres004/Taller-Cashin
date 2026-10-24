package org.ucb.cashin.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.ucb.cashin.R
import org.ucb.cashin.ui.theme.Espacio

/**
 * Barra superior simple: botón atrás opcional, título y acciones a la derecha.
 * [dejarEspacioBarraEstado] = true solo cuando se usa en el slot topBar de un Scaffold
 * (el contenido de un Scaffold ya recibe ese espacio en su padding).
 */
@Composable
fun CashinTopBar(
    titulo: String,
    modifier: Modifier = Modifier,
    onAtras: (() -> Unit)? = null,
    dejarEspacioBarraEstado: Boolean = false,
    acciones: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (dejarEspacioBarraEstado) Modifier.statusBarsPadding() else Modifier)
            .height(64.dp)
            .padding(horizontal = Espacio.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onAtras != null) {
            IconButton(onClick = onAtras) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.accion_atras),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        } else {
            Spacer(Modifier.width(Espacio.m))
        }
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        acciones()
    }
}
