package org.ucb.cashin.ui.deudas

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.ucb.cashin.R
import org.ucb.cashin.ui.components.PantallaEnConstruccion

/** Marcador. Se reemplaza por "Debo" y "Me deben" con sus abonos y lo pendiente (HU-25, HU-26). */
@Composable
fun DeudasScreen(onAtras: () -> Unit) {
    PantallaEnConstruccion(
        titulo = stringResource(R.string.perfil_deudas),
        descripcion = stringResource(R.string.deudas_construccion),
        sprint = "Sprint 5",
        historias = "HU-25, HU-26",
        onAtras = onAtras,
    )
}