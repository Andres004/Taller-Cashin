package org.ucb.cashin.ui.desafios

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.ucb.cashin.R
import org.ucb.cashin.ui.components.PantallaEnConstruccion

/** Marcador. Se reemplaza por los desafíos que propone la IA, aceptar uno y ver si se cumplió (HU-22, HU-23). */
@Composable
fun DesafiosScreen(onAtras: () -> Unit) {
    PantallaEnConstruccion(
        titulo = stringResource(R.string.perfil_desafios),
        descripcion = stringResource(R.string.desafios_construccion),
        sprint = "Sprint 4",
        historias = "HU-22, HU-23",
        onAtras = onAtras,
    )
}