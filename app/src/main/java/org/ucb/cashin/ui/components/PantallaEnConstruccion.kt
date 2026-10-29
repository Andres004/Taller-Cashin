package org.ucb.cashin.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.ucb.cashin.R

/**
 * Marcador temporal para pantallas que aún no se construyen.
 * El dueño del módulo lo reemplaza por la pantalla real en su sprint.
 * Con [onAtras] muestra la flecha "Volver" (para pantallas que se abren desde otra, como el Perfil).
 */
@Composable
fun PantallaEnConstruccion(
    titulo: String,
    descripcion: String,
    sprint: String,
    historias: String,
    modifier: Modifier = Modifier,
    onAtras: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxSize()) {
        CashinTopBar(titulo = titulo, onAtras = onAtras)
        EstadoVacio(
            titulo = stringResource(R.string.en_construccion_titulo, titulo),
            mensaje = stringResource(R.string.en_construccion_mensaje, descripcion, sprint, historias),
            emocion = Emocion.NEUTRAL,
        )
    }
}