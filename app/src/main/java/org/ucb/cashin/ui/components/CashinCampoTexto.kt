package org.ucb.cashin.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.ucb.cashin.R
import org.ucb.cashin.ui.theme.CashinTheme

/**
 * Campo de texto de Cashin: etiqueta arriba, borde redondeado y mensaje de error abajo.
 * Con [esContrasena] = true oculta el texto y muestra el botón del ojo.
 */
@Composable
fun CashinCampoTexto(
    etiqueta: String,
    valor: String,
    onValorCambio: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    placeholder: String? = null,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    accionIme: ImeAction = ImeAction.Next,
    onAccionIme: (() -> Unit)? = null,
    esContrasena: Boolean = false,
    habilitado: Boolean = true,
    capitalizar: Boolean = false,
) {
    var mostrar by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
        )
        OutlinedTextField(
            value = valor,
            onValueChange = onValorCambio,
            singleLine = true,
            enabled = habilitado,
            isError = error != null,
            placeholder = if (placeholder != null) {
                { Text(placeholder, style = MaterialTheme.typography.bodyLarge) }
            } else null,
            shape = MaterialTheme.shapes.medium,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (esContrasena) KeyboardType.Password else tipoTeclado,
                imeAction = accionIme,
                capitalization = if (capitalizar) KeyboardCapitalization.Words else KeyboardCapitalization.None,
                autoCorrectEnabled = !esContrasena && tipoTeclado == KeyboardType.Text,
            ),
            keyboardActions = KeyboardActions(onAny = { onAccionIme?.invoke() }),
            visualTransformation = if (esContrasena && !mostrar) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (esContrasena) {
                {
                    IconButton(onClick = { mostrar = !mostrar }) {
                        Icon(
                            imageVector = if (mostrar) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = stringResource(
                                if (mostrar) R.string.campo_ocultar_contrasena else R.string.campo_mostrar_contrasena,
                            ),
                        )
                    }
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.secondary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
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

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun CashinCampoTextoPreview() = CashinTheme {
    Column {
        CashinCampoTexto("Correo electrónico", "ana@correo.com", {})
        CashinCampoTexto("Contraseña", "123", {}, esContrasena = true, error = "Mínimo 8 caracteres")
    }
}
