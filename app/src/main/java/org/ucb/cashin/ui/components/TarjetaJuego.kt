package org.ucb.cashin.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.ucb.cashin.ui.theme.BordeCampo
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.CremaTarjeta
import org.ucb.cashin.ui.theme.Espacio

private val FORMA_BORDE = RoundedCornerShape(20.dp)
private val FORMA_INTERIOR = RoundedCornerShape(18.dp)

/**
 * Tarjeta "de juego": borde de 2 dp con relieve de 5 dp abajo.
 * Si se pasa [onClick], toda la tarjeta es tocable.
 */
@Composable
fun TarjetaJuego(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val interior: @Composable () -> Unit = {
        Surface(
            shape = FORMA_INTERIOR,
            color = CremaTarjeta,
            modifier = Modifier.padding(start = 2.dp, top = 2.dp, end = 2.dp, bottom = 5.dp),
        ) {
            Column(modifier = Modifier.padding(Espacio.m), content = contenido)
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = FORMA_BORDE,
            color = BordeCampo,
            content = interior,
        )
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = FORMA_BORDE,
            color = BordeCampo,
            content = interior,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun TarjetaJuegoPreview() = CashinTheme {
    TarjetaJuego(modifier = Modifier.padding(Espacio.m)) {
        Text("Celular nuevo", style = MaterialTheme.typography.titleMedium)
    }
}
