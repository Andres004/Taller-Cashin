package org.ucb.cashin.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.util.Formato

/**
 * Campo grande para escribir un monto ("Bs 20,50"). Pensado para el registro rápido (HU-06).
 * Solo acepta dígitos y un separador decimal (coma o punto) con hasta 2 decimales.
 * El ViewModel convierte el texto con `Formato.parsearMonto(texto)`.
 */
@Composable
fun CashinCampoMonto(
    valor: String,
    onValorCambio: (String) -> Unit,
    modifier: Modifier = Modifier,
    moneda: String = "BOB",
    error: String? = null,
    colorMonto: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onBackground,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = Formato.simboloMoneda(moneda) + " ",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BasicTextField(
                value = valor,
                onValueChange = { nuevo -> if (esMontoParcialValido(nuevo)) onValorCambio(nuevo) },
                singleLine = true,
                textStyle = MaterialTheme.typography.displayMedium.copy(color = colorMonto, textAlign = TextAlign.Start),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                decorationBox = { campo ->
                    if (valor.isEmpty()) {
                        Text("0", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.outline)
                    }
                    campo()
                },
            )
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
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
    CashinCampoMonto(valor = "20,50", onValorCambio = {})
}
