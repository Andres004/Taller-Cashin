package org.ucb.cashin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.ucb.cashin.model.Categoria
import org.ucb.cashin.ui.theme.Espacio

/**
 * Ícono para el texto guardado en `categoria.icono`. Para agregar íconos nuevos
 * (HU-11, categorías propias) se agrega una línea aquí y se usa el mismo texto en la BD.
 */
fun iconoCategoria(nombre: String?): ImageVector = when (nombre) {
    "restaurant" -> Icons.Outlined.Restaurant
    "bus" -> Icons.Outlined.DirectionsBus
    "home" -> Icons.Outlined.Home
    "health" -> Icons.Outlined.LocalHospital
    "school" -> Icons.Outlined.School
    "movie" -> Icons.Outlined.Movie
    "work" -> Icons.Outlined.Work
    "store" -> Icons.Outlined.Storefront
    "gift" -> Icons.Outlined.CardGiftcard
    "more" -> Icons.Outlined.MoreHoriz
    else -> Icons.Outlined.Category
}

/** Convierte "#4CAF50" en Color; usa [porDefecto] si el texto no es válido. */
fun colorCategoria(hex: String?, porDefecto: Color): Color {
    if (hex.isNullOrBlank()) return porDefecto
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(porDefecto)
}

/**
 * Grilla de categorías para elegir una con un toque (HU-06/HU-07).
 * Recibe la lista ya filtrada por tipo y sin archivadas.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectorCategoria(
    categorias: List<Categoria>,
    seleccionadaId: String?,
    onSeleccionar: (Categoria) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Espacio.s),
        verticalArrangement = Arrangement.spacedBy(Espacio.s),
    ) {
        categorias.forEach { categoria ->
            val seleccionada = categoria.id == seleccionadaId
            val color = colorCategoria(categoria.color, MaterialTheme.colorScheme.primary)
            Surface(
                onClick = { onSeleccionar(categoria) },
                shape = MaterialTheme.shapes.small,
                color = if (seleccionada) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(if (seleccionada) 2.dp else 1.dp, if (seleccionada) color else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.width(76.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                ) {
                    Icon(iconoCategoria(categoria.icono), contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
                    Text(
                        text = categoria.nombre,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
