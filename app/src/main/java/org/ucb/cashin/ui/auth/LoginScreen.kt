package org.ucb.cashin.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.ucb.cashin.R
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinCampoTexto
import org.ucb.cashin.ui.components.VarianteBoton
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio

/** Pantalla con estado: conecta el ViewModel con la UI. */
@Composable
fun LoginScreen(
    onCrearCuenta: () -> Unit,
    onOlvideContrasena: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val textoGooglePronto = stringResource(R.string.login_google_pronto)

    LoginContenido(
        estado = estado,
        snackbar = snackbar,
        onCorreoCambio = viewModel::onCorreoCambio,
        onContrasenaCambio = viewModel::onContrasenaCambio,
        onIngresar = viewModel::ingresar,
        onGoogle = { scope.launch { snackbar.showSnackbar(textoGooglePronto) } }, // HU-02 · Sprint 2
        onCrearCuenta = onCrearCuenta,
        onOlvideContrasena = onOlvideContrasena,
    )
}

/** Pantalla sin estado: solo dibuja. Así se puede previsualizar sin Supabase. */
@Composable
fun LoginContenido(
    estado: LoginUiState,
    snackbar: SnackbarHostState,
    onCorreoCambio: (String) -> Unit,
    onContrasenaCambio: (String) -> Unit,
    onIngresar: () -> Unit,
    onGoogle: () -> Unit,
    onCrearCuenta: () -> Unit,
    onOlvideContrasena: () -> Unit,
) {
    AuthLayout(
        titulo = stringResource(R.string.login_titulo),
        subtitulo = stringResource(R.string.login_subtitulo),
        snackbar = snackbar,
        error = estado.error,
    ) {
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
            accionIme = ImeAction.Done,
            onAccionIme = onIngresar,
            habilitado = !estado.cargando,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            CashinBoton(
                texto = stringResource(R.string.login_olvide),
                onClick = onOlvideContrasena,
                variante = VarianteBoton.TEXTO,
            )
        }
        CashinBoton(
            texto = stringResource(R.string.login_boton),
            onClick = onIngresar,
            cargando = estado.cargando,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacio.s),
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
            Text(stringResource(R.string.separador_o), color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
        }
        CashinBoton(
            texto = stringResource(R.string.login_google),
            onClick = onGoogle,
            variante = VarianteBoton.SECUNDARIO,
            icono = Icons.Filled.AccountCircle,
            habilitado = !estado.cargando,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.login_sin_cuenta),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CashinBoton(
                texto = stringResource(R.string.login_crear_cuenta),
                onClick = onCrearCuenta,
                variante = VarianteBoton.TEXTO,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginPreview() = CashinTheme {
    LoginContenido(
        estado = LoginUiState(correo = "ana@correo.com"),
        snackbar = remember { SnackbarHostState() },
        onCorreoCambio = {}, onContrasenaCambio = {}, onIngresar = {},
        onGoogle = {}, onCrearCuenta = {}, onOlvideContrasena = {},
    )
}
