package org.ucb.cashin.ui.categorias

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.ucb.cashin.R
import org.ucb.cashin.model.Categoria
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.colorCategoria
import org.ucb.cashin.ui.components.iconoCategoria
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.ui.theme.VerdeProfundo

@StringRes
private fun nombreImportancia(importancia: Int): Int = when (importancia) {
    5 -> R.string.importancia_5_nombre
    4 -> R.string.importancia_4_nombre
    3 -> R.string.importancia_3_nombre
    2 -> R.string.importancia_2_nombre
    else -> R.string.importancia_1_nombre
}

@StringRes
private fun mensajeImportancia(importancia: Int): Int = when (importancia) {
    5 -> R.string.importancia_5_mensaje
    4 -> R.string.importancia_4_mensaje
    3 -> R.string.importancia_3_mensaje
    2 -> R.string.importancia_2_mensaje
    else -> R.string.importancia_1_mensaje
}

/** Una categoría de gasto: toda la tarjeta se toca para cambiar su importancia (si no está archivada). */
@Composable
internal fun FilaGasto(
    categoria: Categoria,
    onTocar: () -> Unit,
    onArchivar: () -> Unit,
    onDesarchivar: () -> Unit,
) {
    val color = colorCategoria(categoria.color, MaterialTheme.colorScheme.primary)
    val forma = MaterialTheme.shapes.medium
    val origen = stringResource(
        when {
            categoria.archivada -> R.string.categorias_archivada
            categoria.esDelSistema -> R.string.categorias_del_sistema
            else -> R.string.categorias_tuya
        },
    )

    // La base del mismo color del borde da el relieve de 4 dp en el lado de abajo.
    Surface(
        shape = forma,
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (categoria.archivada) 0.55f else 1f),
    ) {
        Surface(
            onClick = onTocar,
            enabled = !categoria.archivada,
            shape = forma,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.padding(bottom = 2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .background(color.copy(alpha = 0.15f), CircleShape),
                ) {
                    Icon(iconoCategoria(categoria.icono), contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = categoria.nombre,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(
                            R.string.categorias_importancia_detalle,
                            stringResource(nombreImportancia(categoria.importancia)),
                            origen,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                when {
                    categoria.esDelSistema -> Unit
                    categoria.archivada -> TextButton(onClick = onDesarchivar) {
                        Text(stringResource(R.string.categorias_desarchivar))
                    }
                    else -> TextButton(onClick = onArchivar) {
                        Text(stringResource(R.string.categorias_archivar), color = MaterialTheme.colorScheme.error)
                    }
                }
                InsigniaImportancia(categoria.importancia)
            }
        }
    }
}

/** "3/5" dentro de un círculo naranja. */
@Composable
private fun InsigniaImportancia(importancia: Int) {
    val descripcion = stringResource(R.string.categorias_importancia_descripcion, importancia)
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .size(48.dp)
            .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)
            .border(2.dp, CashinColores.xp, CircleShape)
            .clearAndSetSemantics { contentDescription = descripcion },
    ) {
        Text(
            text = importancia.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.alignByBaseline(),
        )
        Text(
            text = stringResource(R.string.categorias_importancia_sobre),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

/** Hoja opcional para cambiar la importancia de una categoría de gasto. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HojaImportancia(
    categoria: Categoria,
    elegida: Int,
    guardando: Boolean,
    error: ErrorApp?,
    onElegir: (Int) -> Unit,
    onGuardar: () -> Unit,
    onCerrar: () -> Unit,
) {
    val color = colorCategoria(categoria.color, MaterialTheme.colorScheme.primary)
    val guardandoAhora by rememberUpdatedState(guardando)
    val estadoHoja = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || !guardandoAhora },
    )

    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = estadoHoja,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = Espacio.m),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .background(color.copy(alpha = 0.15f), CircleShape),
                ) {
                    Icon(iconoCategoria(categoria.icono), contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
                }
                Text(
                    text = categoria.nombre,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Text(
                text = stringResource(R.string.categorias_importancia_pregunta),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
            ) {
                (1..5).forEach { numero ->
                    BotonImportancia(
                        numero = numero,
                        elegido = numero == elegida,
                        habilitado = !guardando,
                        onClick = { onElegir(numero) },
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Espacio.s),
            ) {
                Text(
                    text = stringResource(R.string.categorias_importancia_minimo),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.categorias_importancia_maximo),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = Espacio.s),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                    Text(
                        text = stringResource(nombreImportancia(elegida)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(mensajeImportancia(elegida)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            Text(
                text = stringResource(R.string.categorias_importancia_nota),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            if (error != null) {
                Text(
                    text = error.mensaje(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }

            CashinBoton(
                texto = stringResource(if (guardando) R.string.categorias_guardando else R.string.categorias_guardar),
                onClick = onGuardar,
                habilitado = !guardando,
            )
        }
    }
}

/** Botón redondo de 56 dp con relieve; el elegido se ve lleno y con anillo naranja. */
@Composable
private fun BotonImportancia(
    numero: Int,
    elegido: Boolean,
    habilitado: Boolean,
    onClick: () -> Unit,
) {
    val hapticos = LocalHapticFeedback.current
    val colorAnillo = CashinColores.xp
    val colorBorde = if (elegido) VerdeProfundo else MaterialTheme.colorScheme.outlineVariant
    val descripcion = stringResource(R.string.categorias_importancia_descripcion, numero)

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .size(56.dp)
            .drawBehind {
                if (elegido) {
                    drawCircle(
                        color = colorAnillo,
                        radius = size.minDimension / 2 + 3.dp.toPx(),
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }
            }
            .background(colorBorde, CircleShape)
            .clip(CircleShape)
            .selectable(
                selected = elegido,
                enabled = habilitado,
                role = Role.RadioButton,
                onClick = {
                    hapticos.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            )
            .clearAndSetSemantics { contentDescription = descripcion },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            // 2 dp de borde arriba y 4 dp abajo, que son el relieve.
            modifier = Modifier
                .padding(top = 2.dp)
                .size(width = 52.dp, height = 50.dp)
                .background(
                    if (elegido) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceContainer,
                    CircleShape,
                ),
        ) {
            Text(
                text = numero.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (elegido) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
