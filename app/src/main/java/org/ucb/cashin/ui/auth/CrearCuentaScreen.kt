package org.ucb.cashin.ui.auth

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.ui.components.AvatarBurbuja
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinCampoTexto
import org.ucb.cashin.ui.components.Emocion
import org.ucb.cashin.ui.components.VarianteBoton
import org.ucb.cashin.ui.theme.CashinTheme

@Composable
fun CrearCuentaScreen(
    onIrALogin: () -> Unit,
    onAtras: () -> Unit,
    viewModel: CrearCuentaViewModel = viewModel(factory = CrearCuentaViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    CrearCuentaContenido(
        estado = estado,
        onNombreCambio = viewModel::onNombreCambio,
        onCorreoCambio = viewModel::onCorreoCambio,
        onContrasenaCambio = viewModel::onContrasenaCambio,
        onConfirmarCambio = viewModel::onConfirmarCambio,
        onCrear = viewModel::crearCuenta,
        onIrALogin = onIrALogin,
        onAtras = onAtras,
    )
}

@Composable
fun CrearCuentaContenido(
    estado: CrearCuentaUiState,
    onNombreCambio: (String) -> Unit,
    onCorreoCambio: (String) -> Unit,
    onContrasenaCambio: (String) -> Unit,
    onConfirmarCambio: (String) -> Unit,
    onCrear: () -> Unit,
    onIrALogin: () -> Unit,
    onAtras: () -> Unit,
) {
    AuthLayout(
        titulo = stringResource(R.string.registro_titulo),
        subtitulo = stringResource(R.string.registro_subtitulo),
        snackbar = remember { SnackbarHostState() },
        error = estado.error,
        onAtras = onAtras,
    ) {
        // Cuenta creada pero falta confirmar el correo: se reemplaza el formulario.
        val correoEnviado = estado.correoConfirmacion
        if (correoEnviado != null) {
            AvatarBurbuja(
                mensaje = stringResource(R.string.registro_revisa_correo, correoEnviado),
                emocion = Emocion.CELEBRANDO,
            )
            CashinBoton(texto = stringResource(R.string.registro_ir_login), onClick = onIrALogin)
        } else {
            CashinCampoTexto(
                etiqueta = stringResource(R.string.campo_nombre),
                valor = estado.nombre,
                onValorCambio = onNombreCambio,
                error = estado.errorNombre?.let { stringResource(it) },
                capitalizar = true,
                habilitado = !estado.cargando,
            )
            CashinCampoTexto(
                etiqueta = stringResource(R.string.campo_correo),
                valor = estado.correo,
                onValorCambio = onCorreoCambio,
                error = estado.errorCorreo?.let { stringResource(it) },
                tipoTeclado = KeyboardType.Email,
                habilitado = !estado.cargando,
            )
            CashinCampoTexto(
                etiqueta = stringResource(R.string.campo_contrasena),
                valor = estado.contrasena,
                onValorCambio = onContrasenaCambio,
                error = estado.errorContrasena?.let { stringResource(it) },
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
                onAccionIme = onCrear,
                habilitado = !estado.cargando,
            )
            CashinBoton(texto = stringResource(R.string.registro_boton), onClick = onCrear, cargando = estado.cargando)
            CashinBoton(
                texto = stringResource(R.string.registro_ya_tengo),
                onClick = onIrALogin,
                variante = VarianteBoton.TEXTO,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CrearCuentaPreview() = CashinTheme {
    CrearCuentaContenido(
        estado = CrearCuentaUiState(nombre = "Ana", errorContrasena = R.string.validacion_contrasena),
        onNombreCambio = {}, onCorreoCambio = {}, onContrasenaCambio = {},
        onConfirmarCambio = {}, onCrear = {}, onIrALogin = {}, onAtras = {},
    )
}