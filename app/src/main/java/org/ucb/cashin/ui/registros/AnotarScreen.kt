package org.ucb.cashin.ui.registros

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.ucb.cashin.R
import org.ucb.cashin.ui.components.PantallaEnConstruccion

/**
 * Marcador. Se reemplaza en el Sprint 2 por el formulario de registro rápido (HU-06, HU-07):
 * CashinCampoMonto + SelectorCategoria + medio de pago + nota, usando NuevoRegistro.
 */
@Composable
fun AnotarScreen() {
    PantallaEnConstruccion(
        titulo = stringResource(R.string.nav_anotar),
        descripcion = stringResource(R.string.anotar_construccion),
        sprint = "Sprint 2",
        historias = "HU-06, HU-07",
    )
}
