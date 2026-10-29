package org.ucb.cashin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.util.Formato

/**
 * Campo grande para escribir un monto ("Bs 20,50"), con el mismo marco que los demás campos.
 * Pensado para el registro rápido (HU-06).
 * Solo acepta dígitos y un separador decimal (coma o punto) con hasta 2 decimales.
 * El ViewModel convierte el texto con `Formato.parsearMonto(texto)`.
 */
@Composable
fun CashinCampoMonto(
    valor: String,
    onValorCambio: (String) -> Unit,
    modifier: Modifier = Modifier,
    etiqueta: String? = null,
    moneda: String = "BOB",
    error: String? = null,
    colorMonto: Color = MaterialTheme.colorScheme.onBackground,
    habilitado: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (etiqueta != null) {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
            )
        }

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
                width = if (error != null) 1.5.dp else 1.dp,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacio.s),
                modifier = Modifier.padding(horizontal = 18.dp),
            ) {
                Text(
                    text = Formato.simboloMoneda(moneda),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BasicTextField(
                    value = valor,
                    onValueChange = { nuevo -> if (esMontoParcialValido(nuevo)) onValorCambio(nuevo) },
                    enabled = habilitado,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.displaySmall.copy(color = colorMonto),
                    cursorBrush = SolidColor(colorMonto),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    decorationBox = { campo ->
                        if (valor.isEmpty()) {
                            Text("0,00", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        campo()
                    },
                )
            }
        }

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
            )
        }
    }
}

/** Acepta "", "20", "20,", "20,5", "20.50"; rechaza letras, dos separadores o 3 decimales. */
internal fun esMontoParcialValido(texto: String): Boolean =
    texto.length <= 12 && Regex("^\\d*([.,]\\d{0,2})?$").matches(texto)

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun CashinCampoMontoPreview() = CashinTheme {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        CashinCampoMonto(valor = "20,50", onValorCambio = {}, etiqueta = "Monto")
        CashinCampoMonto(valor = "", onValorCambio = {}, etiqueta = "Monto", error = "Escribe un monto mayor a 0")
    }
}