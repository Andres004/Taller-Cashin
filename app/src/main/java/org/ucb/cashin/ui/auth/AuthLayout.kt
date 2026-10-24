package org.ucb.cashin.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.ucb.cashin.R
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.Espacio

/**
 * Estructura común de las pantallas de cuenta: logo, título, subtítulo, error general
 * y el formulario. Desplazable y se ajusta cuando aparece el teclado.
 */
@Composable
fun AuthLayout(
    titulo: String,
    subtitulo: String,
    snackbar: SnackbarHostState,
    error: ErrorApp?,
    onAtras: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { if (onAtras != null) CashinTopBar(titulo = "", onAtras = onAtras, dejarEspacioBarraEstado = true) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Espacio.pantalla, vertical = Espacio.l),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Espacio.m),
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_cashin),
                    contentDescription = stringResource(R.string.logo_descripcion),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(width = 170.dp, height = 145.dp),
                )
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (error != null) {
                    Text(
                        text = error.mensaje(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                contenido()
            }
        }
    }
}
