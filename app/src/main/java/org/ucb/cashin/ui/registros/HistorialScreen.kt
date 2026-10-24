package org.ucb.cashin.ui.registros

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.ucb.cashin.R
import org.ucb.cashin.ui.components.PantallaEnConstruccion

/** Marcador. Se reemplaza en el Sprint 2 por el historial agrupado por día (HU-08, HU-09) y filtros (HU-10). */
@Composable
fun HistorialScreen() {
    PantallaEnConstruccion(
        titulo = stringResource(R.string.nav_historial),
        descripcion = stringResource(R.string.historial_construccion),
        sprint = "Sprint 2",
        historias = "HU-08, HU-09, HU-10",
    )
}
