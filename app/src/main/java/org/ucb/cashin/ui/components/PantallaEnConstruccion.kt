package org.ucb.cashin.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.ucb.cashin.R

/**
 * Marcador temporal para pestañas que aún no se construyen.
 * El dueño del módulo lo reemplaza por la pantalla real en su sprint.
 */
@Composable
fun PantallaEnConstruccion(
    titulo: String,
    descripcion: String,
    sprint: String,
    historias: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        CashinTopBar(titulo = titulo)
        EstadoVacio(
            titulo = stringResource(R.string.en_construccion_titulo, titulo),
            mensaje = stringResource(R.string.en_construccion_mensaje, descripcion, sprint, historias),
            emocion = Emocion.NEUTRAL,
        )
    }
}
