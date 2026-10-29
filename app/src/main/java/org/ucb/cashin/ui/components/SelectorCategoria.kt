package org.ucb.cashin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.ucb.cashin.model.Categoria
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.ui.theme.CashinTheme
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

/** Cuántas categorías entran por fila, como en el Figma. */
private const val CATEGORIAS_POR_FILA = 4

/**
 * Grilla de categorías para elegir una con un toque (HU-06/HU-07): 4 por fila, todas del mismo ancho.
 * Recibe la lista ya filtrada por tipo y sin archivadas.
 * Con [conError] = true los bordes se pintan de rojo para marcar que falta elegir una.
 */
@Composable
fun SelectorCategoria(
    categorias: List<Categoria>,
    seleccionadaId: String?,
    onSeleccionar: (Categoria) -> Unit,
    modifier: Modifier = Modifier,
    conError: Boolean = false,
    habilitado: Boolean = true,
) {
    // Column + Row (y no una LazyGrid) porque la pantalla Anotar ya se desplaza entera.
    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(Espacio.s),
    ) {
        categorias.chunked(CATEGORIAS_POR_FILA).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                fila.forEach { categoria ->
                    BotonCategoria(
                        categoria = categoria,
                        seleccionada = categoria.id == seleccionadaId,
                        conError = conError,
                        habilitado = habilitado,
                        onClick = { onSeleccionar(categoria) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // La última fila puede quedar incompleta: se rellena para que no se estiren.
                repeat(CATEGORIAS_POR_FILA - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun BotonCategoria(
    categoria: Categoria,
    seleccionada: Boolean,
    conError: Boolean,
    habilitado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = colorCategoria(categoria.color, MaterialTheme.colorScheme.primary)
    val borde = when {
        seleccionada -> BorderStroke(2.dp, color)
        conError -> BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    val forma = MaterialTheme.shapes.small

    Surface(
        shape = forma,
        color = if (seleccionada) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainer,
        border = borde,
        modifier = modifier
            .height(76.dp)
            .clip(forma)
            .selectable(
                selected = seleccionada,
                enabled = habilitado,
                role = Role.RadioButton,
                onClick = onClick,
            ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        ) {
            Icon(iconoCategoria(categoria.icono), contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
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

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun SelectorCategoriaPreview() = CashinTheme {
    SelectorCategoria(
        categorias = listOf(
            Categoria(id = "1", nombre = "Alimentación", tipo = TipoRegistro.GASTO, icono = "restaurant", color = "#4CAF50"),
            Categoria(id = "2", nombre = "Transporte", tipo = TipoRegistro.GASTO, icono = "bus", color = "#2E7D32"),
            Categoria(id = "3", nombre = "Vivienda", tipo = TipoRegistro.GASTO, icono = "home", color = "#81C784"),
            Categoria(id = "4", nombre = "Salud", tipo = TipoRegistro.GASTO, icono = "health", color = "#FF8C32"),
            Categoria(id = "5", nombre = "Educación", tipo = TipoRegistro.GASTO, icono = "school", color = "#1F6E8C"),
        ),
        seleccionadaId = "1",
        onSeleccionar = {},
        modifier = Modifier.fillMaxWidth(),
    )
}