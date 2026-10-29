package org.ucb.cashin.ui.perfil

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.Moneda
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
import org.ucb.cashin.util.Formato
import java.time.LocalDate

/** Opciones del menú del Perfil. Cada una abre la pantalla de su módulo. */
enum class OpcionPerfil { MIS_CATEGORIAS, PLANIFICADOS, DEUDAS, DESAFIOS }

/**
 * HU-05: perfil con nombre, nivel, XP, racha y moneda, y la opción de cambiar el nombre y la moneda.
 * HU-04: cerrar sesión.
 * El menú lleva a Mis categorías, Pagos esperados, Deudas y Desafíos; quien abre esta
 * pantalla decide a qué ruta ir con [onOpcion].
 */
@Composable
fun PerfilScreen(
    onOpcion: (OpcionPerfil) -> Unit,
    viewModel: PerfilViewModel = viewModel(factory = PerfilViewModel.Factory),
) {
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
        onMonedaCambio = viewModel::cambiarMoneda,
        onOpcion = onOpcion,
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
    onMonedaCambio: (Moneda) -> Unit,
    onOpcion: (OpcionPerfil) -> Unit,
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
            when {
                estado.cargando -> CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = Espacio.xl),
                )

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
                    guardandoMoneda = estado.guardandoMoneda,
                    onEditarNombre = onEditarNombre,
                    onMonedaCambio = onMonedaCambio,
                )
            }

            if (estado.perfil != null) {
                MenuPerfil(onOpcion = onOpcion)
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

/** Encabezado con Cashi, nombre y correo; nivel con XP; las dos rachas; y la moneda. */
@Composable
private fun DatosDelPerfil(
    perfil: Perfil,
    correo: String,
    racha: Int,
    guardandoMoneda: Boolean,
    onEditarNombre: () -> Unit,
    onMonedaCambio: (Moneda) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        EncabezadoPerfil(nombre = perfil.nombre, correo = correo, onEditarNombre = onEditarNombre)

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

        TarjetaMoneda(moneda = perfil.moneda, guardando = guardandoMoneda, onMonedaCambio = onMonedaCambio)
    }
}

/** Cashi en un círculo, el nombre y el correo al lado, y el botón de lápiz para editar el nombre. */
@Composable
private fun EncabezadoPerfil(nombre: String, correo: String, onEditarNombre: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.size(68.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                AvatarImagen(emocion = Emocion.COOL, tamano = 56.dp)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nombre,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = correo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Surface(
            onClick = onEditarNombre,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.perfil_editar_nombre),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp),
                )
            }
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

/** "Moneda" con dos opciones, Bs y $us. La elegida va en verde oscuro. */
@Composable
private fun TarjetaMoneda(moneda: Moneda, guardando: Boolean, onMonedaCambio: (Moneda) -> Unit) {
    CashinTarjeta {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.perfil_moneda),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
            ) {
                Moneda.entries.forEach { opcion ->
                    val elegida = opcion == moneda
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (elegida) MaterialTheme.colorScheme.secondary else Color.Transparent)
                            .selectable(
                                selected = elegida,
                                enabled = !guardando,
                                role = Role.RadioButton,
                                onClick = { onMonedaCambio(opcion) },
                            )
                            .padding(horizontal = Espacio.m),
                    ) {
                        Text(
                            text = Formato.simboloMoneda(opcion.name),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (elegida) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Lista de opciones con ícono, nombre y una flecha ">", separadas por líneas finas. */
@Composable
private fun MenuPerfil(onOpcion: (OpcionPerfil) -> Unit) {
    val linea = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, linea),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            OpcionPerfil.entries.forEachIndexed { indice, opcion ->
                if (indice > 0) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Espacio.m), color = linea)
                }
                FilaMenu(opcion = opcion, onClick = { onOpcion(opcion) })
            }
        }
    }
}

@Composable
private fun FilaMenu(opcion: OpcionPerfil, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .height(52.dp)
            .padding(horizontal = Espacio.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        Icon(
            imageVector = opcion.icono(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(opcion.etiqueta()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@StringRes
private fun OpcionPerfil.etiqueta(): Int = when (this) {
    OpcionPerfil.MIS_CATEGORIAS -> R.string.perfil_mis_categorias
    OpcionPerfil.PLANIFICADOS -> R.string.perfil_planificados
    OpcionPerfil.DEUDAS -> R.string.perfil_deudas
    OpcionPerfil.DESAFIOS -> R.string.perfil_desafios
}

private fun OpcionPerfil.icono(): ImageVector = when (this) {
    OpcionPerfil.MIS_CATEGORIAS -> Icons.Outlined.GridView
    OpcionPerfil.PLANIFICADOS -> Icons.Outlined.CalendarMonth
    OpcionPerfil.DEUDAS -> Icons.Outlined.SwapHoriz
    OpcionPerfil.DESAFIOS -> Icons.Outlined.EmojiEvents
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

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA, heightDp = 900)
@Composable
private fun PerfilPreview() = CashinTheme {
    PerfilContenido(
        estado = PerfilUiState(
            correo = "carlos@correo.com",
            perfil = Perfil(
                id = "u",
                nombre = "Carlos",
                xp = 230,
                rachaActual = 4,
                rachaMaxima = 9,
                ultimoRegistro = LocalDate.now(),
            ),
            racha = 4,
            cargando = false,
        ),
        onReintentar = {},
        onEditarNombre = {},
        onNombreCambio = {},
        onGuardarNombre = {},
        onCancelarEdicion = {},
        onMonedaCambio = {},
        onOpcion = {},
        onCerrarSesion = {},
    )
}