package org.ucb.cashin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.ucb.cashin.R
import org.ucb.cashin.model.Categoria
import org.ucb.cashin.model.Registro
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.util.Formato
import java.math.BigDecimal
import java.time.Instant

/**
 * Una anotación en una lista: ícono de la categoría, nombre, hora, nota y monto.
 * Se usa en el Historial (HU-09) y en las últimas anotaciones de Inicio (HU-13).
 * Si recibe [onClick], la fila se puede tocar (HU-08: editar).
 */
@Composable
fun FilaRegistro(
    registro: Registro,
    categoria: Categoria?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val esGasto = registro.tipo == TipoRegistro.GASTO
    val colorDeCategoria = colorCategoria(categoria?.color, MaterialTheme.colorScheme.primary)
    val hora = Formato.hora(registro.fechaHora)

    CashinTarjeta(modifier = modifier, onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacio.m),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colorDeCategoria.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconoCategoria(categoria?.icono),
                    contentDescription = null,
                    tint = colorDeCategoria,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoria?.nombre ?: stringResource(R.string.historial_sin_categoria),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = registro.nota?.let { stringResource(R.string.historial_hora_nota, hora, it) } ?: hora,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = Formato.montoConSigno(registro.monto, esGasto),
                style = MaterialTheme.typography.titleMedium,
                color = if (esGasto) CashinColores.gasto else CashinColores.ingreso,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun FilaRegistroPreview() = CashinTheme {
    FilaRegistro(
        registro = Registro(
            id = "1",
            usuarioId = "u",
            categoriaId = "c",
            tipo = TipoRegistro.GASTO,
            monto = BigDecimal("20.00"),
            fechaHora = Instant.now(),
            nota = "Almuerzo",
        ),
        categoria = Categoria(id = "c", nombre = "Alimentación", tipo = TipoRegistro.GASTO, icono = "restaurant", color = "#4CAF50"),
    )
}