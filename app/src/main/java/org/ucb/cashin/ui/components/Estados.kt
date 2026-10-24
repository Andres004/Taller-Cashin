package org.ucb.cashin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.ucb.cashin.R
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio

/*
 * Los 4 estados obligatorios de cada pantalla (Maestro §9): cargando, vacío, con datos y error.
 * "Con datos" es el contenido propio de cada pantalla; los otros tres están aquí.
 */

@Composable
fun EstadoCargando(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun EstadoVacio(
    titulo: String,
    mensaje: String,
    modifier: Modifier = Modifier,
    emocion: Emocion = Emocion.NEUTRAL,
    textoAccion: String? = null,
    onAccion: (() -> Unit)? = null,
) {
    EstadoBase(modifier, emocion, titulo, mensaje, textoAccion, onAccion)
}

@Composable
fun EstadoError(
    mensaje: String,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EstadoBase(
        modifier = modifier,
        emocion = Emocion.PREOCUPADO,
        titulo = stringResource(R.string.estado_error_titulo),
        mensaje = mensaje,
        textoAccion = stringResource(R.string.estado_reintentar),
        onAccion = onReintentar,
    )
}

@Composable
private fun EstadoBase(
    modifier: Modifier,
    emocion: Emocion,
    titulo: String,
    mensaje: String,
    textoAccion: String?,
    onAccion: (() -> Unit)?,
) {
    Box(modifier = modifier.fillMaxSize().padding(Espacio.pantalla), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espacio.s),
            modifier = Modifier.widthIn(max = 360.dp),
        ) {
            AvatarImagen(emocion = emocion, tamano = 140.dp)
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (textoAccion != null && onAccion != null) {
                CashinBoton(texto = textoAccion, onClick = onAccion, modifier = Modifier.padding(top = Espacio.s))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun EstadoVacioPreview() = CashinTheme {
    EstadoVacio(titulo = "Aún no hay anotaciones", mensaje = "Toca + para anotar tu primer gasto.")
}
