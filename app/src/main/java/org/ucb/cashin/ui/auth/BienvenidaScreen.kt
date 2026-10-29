package org.ucb.cashin.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.ucb.cashin.R
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.VarianteBoton
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio

/**
 * Primera pantalla cuando no hay sesión: logo de Cashin, un mensaje corto y dos caminos,
 * "Crear cuenta" (HU-01) o "Iniciar sesión".
 * No tiene ViewModel porque no guarda estado ni llama a Supabase: solo avisa qué botón se tocó.
 */
@Composable
fun BienvenidaScreen(
    onCrearCuenta: () -> Unit,
    onIniciarSesion: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxSize()
                    .padding(horizontal = Espacio.pantalla, vertical = Espacio.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Parte de arriba: ocupa todo el espacio libre y centra el logo y los textos.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_cashin),
                        contentDescription = stringResource(R.string.logo_descripcion),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(width = 240.dp, height = 205.dp),
                    )
                    Spacer(Modifier.height(Espacio.l))
                    Text(
                        text = stringResource(R.string.bienvenida_titulo),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Espacio.s))
                    Text(
                        text = stringResource(R.string.bienvenida_subtitulo),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                // Parte de abajo: los dos botones, siempre pegados al final de la pantalla.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Espacio.m),
                ) {
                    CashinBoton(
                        texto = stringResource(R.string.bienvenida_crear_cuenta),
                        onClick = onCrearCuenta,
                    )
                    CashinBoton(
                        texto = stringResource(R.string.bienvenida_iniciar_sesion),
                        onClick = onIniciarSesion,
                        variante = VarianteBoton.SECUNDARIO,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun BienvenidaPreview() = CashinTheme {
    BienvenidaScreen(onCrearCuenta = {}, onIniciarSesion = {})
}