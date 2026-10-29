package org.ucb.cashin.ui.categorias

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.ucb.cashin.R
import org.ucb.cashin.ui.components.PantallaEnConstruccion

/** Marcador. Se reemplaza por la lista de categorías propias: crear y archivar (HU-11, HU-12). */
@Composable
fun CategoriasScreen(onAtras: () -> Unit) {
    PantallaEnConstruccion(
        titulo = stringResource(R.string.perfil_mis_categorias),
        descripcion = stringResource(R.string.categorias_construccion),
        sprint = "Sprint 3",
        historias = "HU-11, HU-12",
        onAtras = onAtras,
    )
}