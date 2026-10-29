package org.ucb.cashin.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinCampoTexto
import org.ucb.cashin.ui.components.Emocion
import org.ucb.cashin.ui.components.IndicadorPasos
import org.ucb.cashin.ui.components.VarianteBoton
import org.ucb.cashin.ui.theme.CashinTheme

@Composable
fun RecuperarScreen(
    onAtras: () -> Unit,
    viewModel: RecuperarViewModel = viewModel(factory = RecuperarViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val textoMensaje = estado.mensaje?.let { stringResource(it) }

    LaunchedEffect(textoMensaje) {
        if (textoMensaje != null) {
            snackbar.showSnackbar(textoMensaje)
            viewModel.mensajeMostrado()
        }
    }

    RecuperarContenido(
        estado = estado,
        snackbar = snackbar,
        onAtras = if (estado.paso == PasoRecuperar.CODIGO) viewModel::volverAlCorreo else onAtras,
        onCorreoCambio = viewModel::onCorreoCambio,
        onCodigoCambio = viewModel::onCodigoCambio,
        onNuevaCambio = viewModel::onNuevaCambio,
        onConfirmarCambio = viewModel::onConfirmarCambio,
        onEnviarCodigo = viewModel::enviarCodigo,
        onCambiar = viewModel::cambiarContrasena,
    )
}

/**
 * HU-03 con el diseño del Figma: Cashi pensando junto al título, el indicador
 * "Correo → Código" y debajo el formulario del paso en el que se está.
 */
@Composable
fun RecuperarContenido(
    estado: RecuperarUiState,
    snackbar: SnackbarHostState,
    onAtras: () -> Unit,
    onCorreoCambio: (String) -> Unit,
    onCodigoCambio: (String) -> Unit,
    onNuevaCambio: (String) -> Unit,
    onConfirmarCambio: (String) -> Unit,
    onEnviarCodigo: () -> Unit,
    onCambiar: () -> Unit,
) {
    val correo = estado.correo.trim()
    val instruccion = when (estado.paso) {
        PasoRecuperar.CORREO -> AnnotatedString(stringResource(R.string.recuperar_paso1))
        PasoRecuperar.CODIGO -> conCorreoResaltado(
            texto = stringResource(R.string.recuperar_paso2, correo),
            correo = correo,
        )
    }

    AuthLayout(
        titulo = stringResource(R.string.recuperar_titulo),
        subtitulo = null,
        snackbar = snackbar,
        error = estado.error,
        onAtras = onAtras,
        emocionAvatar = Emocion.NEUTRAL,
    ) {
        IndicadorPasos(
            pasos = listOf(
                stringResource(R.string.recuperar_paso_correo),
                stringResource(R.string.recuperar_paso_codigo),
            ),
            pasoActual = estado.paso.ordinal,
        )

        Text(
            text = instruccion,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        when (estado.paso) {
            PasoRecuperar.CORREO -> {
                CashinCampoTexto(
                    etiqueta = stringResource(R.string.campo_correo),
                    valor = estado.correo,
                    onValorCambio = onCorreoCambio,
                    error = estado.errorCorreo?.let { stringResource(it) },
                    tipoTeclado = KeyboardType.Email,
                    accionIme = ImeAction.Done,
                    onAccionIme = onEnviarCodigo,
                    habilitado = !estado.cargando,
                )
                CashinBoton(
                    texto = stringResource(R.string.recuperar_enviar),
                    onClick = onEnviarCodigo,
                    cargando = estado.cargando,
                )
            }

            PasoRecuperar.CODIGO -> {
                CashinCampoTexto(
                    etiqueta = stringResource(R.string.campo_codigo),
                    valor = estado.codigo,
                    onValorCambio = onCodigoCambio,
                    error = estado.errorCodigo?.let { stringResource(it) },
                    tipoTeclado = KeyboardType.NumberPassword,
                    habilitado = !estado.cargando,
                )
                CashinCampoTexto(
                    etiqueta = stringResource(R.string.campo_nueva_contrasena),
                    valor = estado.nueva,
                    onValorCambio = onNuevaCambio,
                    error = estado.errorNueva?.let { stringResource(it) },
                    placeholder = stringResource(R.string.validacion_contrasena),
                    esContrasena = true,
                    habilitado = !estado.cargando,
                )
                CashinCampoTexto(
                    etiqueta = stringResource(R.string.campo_confirmar_contrasena),
                    valor = estado.confirmar,
                    onValorCambio = onConfirmarCambio,
                    error = estado.errorConfirmar?.let { stringResource(it) },
                    esContrasena = true,
                    accionIme = ImeAction.Done,
                    onAccionIme = onCambiar,
                    habilitado = !estado.cargando,
                )
                CashinBoton(
                    texto = stringResource(R.string.recuperar_cambiar),
                    onClick = onCambiar,
                    cargando = estado.cargando,
                )
                CashinBoton(
                    texto = stringResource(R.string.recuperar_reenviar),
                    onClick = onEnviarCodigo,
                    variante = VarianteBoton.TEXTO,
                    habilitado = !estado.cargando,
                )
            }
        }
    }
}

/** Pone en negrita el correo dentro de la instrucción, para que se vea a dónde llegó el código. */
@Composable
private fun conCorreoResaltado(texto: String, correo: String): AnnotatedString {
    val colorCorreo = MaterialTheme.colorScheme.onBackground
    return buildAnnotatedString {
        append(texto)
        val inicio = texto.indexOf(correo)
        if (correo.isNotEmpty() && inicio >= 0) {
            addStyle(
                style = SpanStyle(fontWeight = FontWeight.Medium, color = colorCorreo),
                start = inicio,
                end = inicio + correo.length,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RecuperarCorreoPreview() = CashinTheme {
    RecuperarContenido(
        estado = RecuperarUiState(paso = PasoRecuperar.CORREO),
        snackbar = remember { SnackbarHostState() },
        onAtras = {}, onCorreoCambio = {}, onCodigoCambio = {}, onNuevaCambio = {},
        onConfirmarCambio = {}, onEnviarCodigo = {}, onCambiar = {},
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RecuperarCodigoPreview() = CashinTheme {
    RecuperarContenido(
        estado = RecuperarUiState(paso = PasoRecuperar.CODIGO, correo = "ana@correo.com"),
        snackbar = remember { SnackbarHostState() },
        onAtras = {}, onCorreoCambio = {}, onCodigoCambio = {}, onNuevaCambio = {},
        onConfirmarCambio = {}, onEnviarCodigo = {}, onCambiar = {},
    )
}