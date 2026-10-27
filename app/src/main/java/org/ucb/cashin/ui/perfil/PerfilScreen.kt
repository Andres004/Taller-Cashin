package org.ucb.cashin.ui.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.Perfil
import org.ucb.cashin.ui.components.AvatarImagen
import org.ucb.cashin.ui.components.BarraProgreso
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinCampoTexto
import org.ucb.cashin.ui.components.CashinTarjeta
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.Emocion
import org.ucb.cashin.ui.components.VarianteBoton
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import java.time.LocalDate

/**
 * HU-05: perfil con nombre, nivel, XP y racha, y la opción de cambiar el nombre.
 * HU-04: cerrar sesión.
 */
@Composable
fun PerfilScreen(viewModel: PerfilViewModel = viewModel(factory = PerfilViewModel.Factory)) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Se recarga cada vez que se entra a la pestaña, para ver el XP y la racha al día.
    LaunchedEffect(Unit) { viewModel.cargar() }

    PerfilContenido(
        estado = estado,
        onReintentar = viewModel::cargar,
        onEditarNombre = viewModel::onEditarNombre,
        onNombreCambio = viewModel::onNombreCambio,
        onGuardarNombre = viewModel::guardarNombre,
        onCancelarEdicion = viewModel::onCancelarEdicion,
        onCerrarSesion = viewModel::cerrarSesion,
    )
}

/** Solo dibuja. "Cerrar sesión" siempre está visible, aunque el perfil no cargue. */
@Composable
fun PerfilContenido(
    estado: PerfilUiState,
    onReintentar: () -> Unit,
    onEditarNombre: () -> Unit,
    onNombreCambio: (String) -> Unit,
    onGuardarNombre: () -> Unit,
    onCancelarEdicion: () -> Unit,
    onCerrarSesion: () -> Unit,
) {
    var confirmarCierre by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(titulo = stringResource(R.string.nav_perfil))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.pantalla, vertical = Espacio.s),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espacio.m),
        ) {
            AvatarImagen(emocion = Emocion.COOL, tamano = 140.dp)

            when {
                estado.cargando -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)

                estado.errorCarga != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = estado.errorCarga.mensaje(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    CashinBoton(
                        texto = stringResource(R.string.estado_reintentar),
                        onClick = onReintentar,
                        variante = VarianteBoton.TEXTO,
                    )
                }

                estado.perfil != null -> DatosDelPerfil(
                    perfil = estado.perfil,
                    correo = estado.correo,
                    racha = estado.racha,
                    onEditarNombre = onEditarNombre,
                )
            }

            estado.error?.let {
                Text(it.mensaje(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            CashinBoton(
                texto = stringResource(R.string.perfil_cerrar_sesion),
                onClick = { confirmarCierre = true },
                variante = VarianteBoton.SECUNDARIO,
                icono = Icons.AutoMirrored.Outlined.Logout,
                cargando = estado.cerrandoSesion,
            )
        }
    }

    if (estado.editandoNombre) {
        DialogoEditarNombre(
            nombre = estado.nombreEditado,
            errorNombre = estado.errorNombre,
            errorGuardar = estado.errorGuardar,
            guardando = estado.guardandoNombre,
            onNombreCambio = onNombreCambio,
            onGuardar = onGuardarNombre,
            onCancelar = onCancelarEdicion,
        )
    }

    if (confirmarCierre) {
        AlertDialog(
            onDismissRequest = { confirmarCierre = false },
            title = { Text(stringResource(R.string.perfil_cerrar_titulo)) },
            text = { Text(stringResource(R.string.perfil_cerrar_mensaje)) },
            confirmButton = {
                TextButton(onClick = { confirmarCierre = false; onCerrarSesion() }) {
                    Text(stringResource(R.string.perfil_cerrar_sesion))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarCierre = false }) { Text(stringResource(R.string.accion_cancelar)) }
            },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }
}

/** Nombre, correo, botón para editar el nombre, nivel con XP y las dos rachas. */
@Composable
private fun DatosDelPerfil(
    perfil: Perfil,
    correo: String,
    racha: Int,
    onEditarNombre: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = perfil.nombre,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = correo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CashinBoton(
                texto = stringResource(R.string.perfil_editar_nombre),
                onClick = onEditarNombre,
                variante = VarianteBoton.TEXTO,
                icono = Icons.Outlined.Edit,
            )
        }

        TarjetaNivel(perfil = perfil)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Espacio.m),
        ) {
            TarjetaRacha(
                titulo = stringResource(R.string.perfil_racha_actual),
                dias = racha,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            TarjetaRacha(
                titulo = stringResource(R.string.perfil_racha_maxima),
                dias = perfil.rachaMaxima,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

/** Nivel actual, XP total y cuánto falta para el siguiente nivel. */
@Composable
private fun TarjetaNivel(perfil: Perfil) {
    val xpFaltante = Perfil.XP_POR_NIVEL - perfil.xp % Perfil.XP_POR_NIVEL

    CashinTarjeta {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.perfil_nivel, perfil.nivel),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.perfil_xp, perfil.xp),
                style = MaterialTheme.typography.titleMedium,
                color = CashinColores.xp,
            )
        }
        BarraProgreso(progreso = perfil.progresoNivel, modifier = Modifier.padding(top = Espacio.s))
        Text(
            text = stringResource(R.string.perfil_xp_siguiente, xpFaltante, perfil.nivel + 1),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Espacio.s),
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TarjetaRacha(titulo: String, dias: Int, modifier: Modifier = Modifier) {
    CashinTarjeta(modifier = modifier) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = pluralStringResource(R.plurals.perfil_dias, dias, dias),
            style = MaterialTheme.typography.headlineSmall,
            color = CashinColores.racha,
        )
    }
}

/** Diálogo para escribir el nombre nuevo. No se puede cerrar mientras se guarda. */
@Composable
private fun DialogoEditarNombre(
    nombre: String,
    errorNombre: Int?,
    errorGuardar: ErrorApp?,
    guardando: Boolean,
    onNombreCambio: (String) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!guardando) onCancelar() },
        title = { Text(stringResource(R.string.perfil_editar_nombre)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                CashinCampoTexto(
                    etiqueta = stringResource(R.string.campo_nombre),
                    valor = nombre,
                    onValorCambio = onNombreCambio,
                    error = errorNombre?.let { stringResource(it) },
                    accionIme = ImeAction.Done,
                    onAccionIme = onGuardar,
                    habilitado = !guardando,
                    capitalizar = true,
                )
                errorGuardar?.let {
                    Text(
                        text = it.mensaje(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onGuardar, enabled = !guardando) {
                Text(stringResource(R.string.perfil_guardar))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar, enabled = !guardando) {
                Text(stringResource(R.string.accion_cancelar))
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun PerfilPreview() = CashinTheme {
    PerfilContenido(
        estado = PerfilUiState(
            correo = "carlos@correo.com",
            perfil = Perfil(
                id = "u",
                nombre = "Carlos",
                xp = 230,
                rachaActual = 3,
                rachaMaxima = 5,
                ultimoRegistro = LocalDate.now(),
            ),
            racha = 3,
            cargando = false,
        ),
        onReintentar = {},
        onEditarNombre = {},
        onNombreCambio = {},
        onGuardarNombre = {},
        onCancelarEdicion = {},
        onCerrarSesion = {},
    )
}