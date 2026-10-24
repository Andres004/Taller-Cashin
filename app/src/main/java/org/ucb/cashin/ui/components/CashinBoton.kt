package org.ucb.cashin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.ucb.cashin.ui.theme.CashinTheme

enum class VarianteBoton { PRINCIPAL, SECUNDARIO, TEXTO }

/**
 * Botón estándar de Cashin (píldora verde de 54 dp).
 * - PRINCIPAL: acción principal de la pantalla (una sola por pantalla).
 * - SECUNDARIO: con borde, para acciones alternativas ("Continuar con Google").
 * - TEXTO: enlaces ("¿Olvidaste tu contraseña?").
 * Mientras [cargando] es true se muestra un indicador y el botón no responde.
 */
@Composable
fun CashinBoton(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variante: VarianteBoton = VarianteBoton.PRINCIPAL,
    cargando: Boolean = false,
    habilitado: Boolean = true,
    icono: ImageVector? = null,
) {
    val activo = habilitado && !cargando
    val forma = RoundedCornerShape(50)
    val contenido: @Composable () -> Unit = {
        if (cargando) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp),
                color = if (variante == VarianteBoton.PRINCIPAL) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.primary,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icono != null) {
                    Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(texto, style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    when (variante) {
        VarianteBoton.PRINCIPAL -> Button(
            onClick = onClick,
            enabled = activo,
            shape = forma,
            modifier = modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) { contenido() }

        VarianteBoton.SECUNDARIO -> OutlinedButton(
            onClick = onClick,
            enabled = activo,
            shape = forma,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
            modifier = modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
        ) { contenido() }

        VarianteBoton.TEXTO -> TextButton(
            onClick = onClick,
            enabled = activo,
            modifier = modifier,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
        ) { contenido() }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun CashinBotonPreview() = CashinTheme {
    androidx.compose.foundation.layout.Column {
        CashinBoton("Iniciar sesión", onClick = {})
        CashinBoton("Guardando", onClick = {}, cargando = true)
        CashinBoton("Continuar con Google", onClick = {}, variante = VarianteBoton.SECUNDARIO)
        CashinBoton("¿Olvidaste tu contraseña?", onClick = {}, variante = VarianteBoton.TEXTO)
    }
}
