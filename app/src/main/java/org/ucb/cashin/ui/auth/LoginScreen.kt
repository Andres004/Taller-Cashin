package org.ucb.cashin.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.ui.components.RecursoCashi
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
    onAtras: (() -> Unit)? = null,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // HU-02: la hoja de cuentas de Google (Credential Manager). Null si falta GOOGLE_WEB_CLIENT_ID.
    val google = viewModel.googleNativo?.rememberSignInWithGoogle(
        onResult = { resultado -> viewModel.onGoogleResultado(traducir(resultado)) },
        // Sin Google Play o sin cuentas en el teléfono: se avisa (no se abre el navegador).
        fallback = { viewModel.onGoogleResultado(ResultadoGoogle.SIN_CUENTA_GOOGLE) },
    )

    // Avisos cortos de Google (sin internet, sin configurar...): se muestran una vez y se limpian.
    val aviso = estado.avisoGoogle?.let { stringResource(it) }
    LaunchedEffect(aviso) {
        if (aviso != null) {
            snackbar.showSnackbar(aviso)
            viewModel.onAvisoVisto()
        }
    }

    LoginContenido(
        estado = estado,
        snackbar = snackbar,
        onCorreoCambio = viewModel::onCorreoCambio,
        onContrasenaCambio = viewModel::onContrasenaCambio,
        onIngresar = viewModel::ingresar,
        onGoogle = {
            if (google == null) {
                viewModel.onGoogleSinConfigurar()
            } else {
                viewModel.onGoogleInicio()
                google.startFlow()
            }
        },
        onCrearCuenta = onCrearCuenta,
        onOlvideContrasena = onOlvideContrasena,
        onAtras = onAtras,
    )

    if (estado.correoConCuenta) {
        AlertDialog(
            onDismissRequest = viewModel::onCerrarCorreoConCuenta,
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text(stringResource(R.string.login_google_correo_titulo)) },
            text = { Text(stringResource(R.string.login_google_correo_mensaje)) },
            confirmButton = {
                TextButton(onClick = viewModel::onCerrarCorreoConCuenta) { Text(stringResource(R.string.accion_aceptar)) }
            },
        )
    }
}

/** Traduce el resultado de compose-auth a lo que entiende el ViewModel. */
private fun traducir(resultado: NativeSignInResult): ResultadoGoogle = when (resultado) {
    is NativeSignInResult.Success -> ResultadoGoogle.LISTO
    is NativeSignInResult.ClosedByUser -> ResultadoGoogle.CANCELADO
    is NativeSignInResult.NetworkError -> ResultadoGoogle.SIN_CONEXION
    is NativeSignInResult.Error ->
        if (LoginViewModel.esCorreoConCuenta(resultado.message)) ResultadoGoogle.CUENTA_CON_CORREO else ResultadoGoogle.ERROR
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
    onAtras: (() -> Unit)? = null,
) {
    AuthLayout(
        titulo = stringResource(R.string.login_titulo),
        subtitulo = stringResource(R.string.login_subtitulo),
        snackbar = snackbar,
        error = estado.error,
        onAtras = onAtras,
        recursoCashi = RecursoCashi.INICIO_SESION,
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
        BotonGoogle(
            onClick = onGoogle,
            cargando = estado.cargandoGoogle,
            habilitado = !estado.cargando && !estado.cargandoGoogle,
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

/**
 * "Continuar con Google": botón plano blanco con el logo de Google, como lo pide la marca.
 * Mientras se abre la hoja de cuentas muestra una ruedita en lugar del logo.
 */
@Composable
private fun BotonGoogle(onClick: () -> Unit, cargando: Boolean, habilitado: Boolean) {
    Surface(
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(50),
        color = Color.White,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 54.dp),
        ) {
            Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                if (cargando) {
                    CircularProgressIndicator(strokeWidth = 2.dp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                } else {
                    Icon(painterResource(R.drawable.ic_google), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(22.dp))
                }
            }
            Text(
                text = stringResource(R.string.login_google),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = Espacio.s),
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