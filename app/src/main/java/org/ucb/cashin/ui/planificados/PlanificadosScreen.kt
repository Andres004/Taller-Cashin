package org.ucb.cashin.ui.planificados

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.ucb.cashin.R
import org.ucb.cashin.ui.components.PantallaEnConstruccion

/** Marcador. Se reemplaza por los pagos e ingresos esperados y "convertir en registro" (HU-15, HU-16). */
@Composable
fun PlanificadosScreen(onAtras: () -> Unit) {
    PantallaEnConstruccion(
        titulo = stringResource(R.string.perfil_planificados),
        descripcion = stringResource(R.string.planificados_construccion),
        sprint = "Sprint 3",
        historias = "HU-15, HU-16",
        onAtras = onAtras,
    )
}