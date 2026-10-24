package org.ucb.cashin.ui.metas

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.ucb.cashin.R
import org.ucb.cashin.ui.components.PantallaEnConstruccion

/** Marcador. Se reemplaza por la lista de metas con v_avance_meta y BarraProgreso (HU-17 a HU-19). */
@Composable
fun MetasScreen() {
    PantallaEnConstruccion(
        titulo = stringResource(R.string.nav_metas),
        descripcion = stringResource(R.string.metas_construccion),
        sprint = "Sprint 3",
        historias = "HU-17, HU-18, HU-19",
    )
}
