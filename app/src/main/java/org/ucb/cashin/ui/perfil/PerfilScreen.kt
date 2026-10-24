package org.ucb.cashin.ui.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.ui.components.AvatarImagen
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinTarjeta
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.Emocion
import org.ucb.cashin.ui.components.VarianteBoton
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio

@Composable
fun PerfilScreen(viewModel: PerfilViewModel = viewModel(factory = PerfilViewModel.Factory)) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    PerfilContenido(estado = estado, onCerrarSesion = viewModel::cerrarSesion)
}

@Composable
fun PerfilContenido(estado: PerfilUiState, onCerrarSesion: () -> Unit) {
    var confirmar by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(titulo = stringResource(R.string.nav_perfil))
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.pantalla, vertical = Espacio.s),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espacio.m),
        ) {
            AvatarImagen(emocion = Emocion.COOL, tamano = 140.dp)
            CashinTarjeta {
                Text(
                    text = stringResource(R.string.perfil_sesion_iniciada),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = estado.correo,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.perfil_construccion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Espacio.s),
                )
            }
            estado.error?.let {
                Text(it.mensaje(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            CashinBoton(
                texto = stringResource(R.string.perfil_cerrar_sesion),
                onClick = { confirmar = true },
                variante = VarianteBoton.SECUNDARIO,
                icono = Icons.AutoMirrored.Outlined.Logout,
                cargando = estado.cerrandoSesion,
            )
        }
    }

    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text(stringResource(R.string.perfil_cerrar_titulo)) },
            text = { Text(stringResource(R.string.perfil_cerrar_mensaje)) },
            confirmButton = {
                TextButton(onClick = { confirmar = false; onCerrarSesion() }) {
                    Text(stringResource(R.string.perfil_cerrar_sesion))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmar = false }) { Text(stringResource(R.string.accion_cancelar)) }
            },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun PerfilPreview() = CashinTheme {
    PerfilContenido(estado = PerfilUiState(correo = "ana@correo.com"), onCerrarSesion = {})
}
