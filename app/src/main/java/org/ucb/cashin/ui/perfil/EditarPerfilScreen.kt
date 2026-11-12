package org.ucb.cashin.ui.perfil

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.model.Moneda
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinCampoTexto
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.EstadoCargando
import org.ucb.cashin.ui.components.EstadoError
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio

/** Días que se ofrecen como atajo; "Otro" abre un diálogo para escribir cualquiera de 1 a 31. */
private val DIAS_RAPIDOS = listOf(1, 5, 15, 30)

/** HU-05: avatar, nombre, moneda, día de cobro y el acceso a "Cambiar contraseña". */
@Composable
fun EditarPerfilScreen(
    onAtras: () -> Unit,
    onGuardado: () -> Unit,
    onCambiarContrasena: () -> Unit,
    viewModel: EditarPerfilViewModel = viewModel(factory = EditarPerfilViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(estado.guardado) { if (estado.guardado) onGuardado() }

    EditarPerfilContenido(
        estado = estado,
        onAtras = onAtras,
        onReintentar = viewModel::cargar,
        onAvatar = viewModel::onAvatar,
        onNombre = viewModel::onNombre,
        onMoneda = viewModel::onMoneda,
        onDiaCobro = viewModel::onDiaCobro,
        onCambiarContrasena = onCambiarContrasena,
        onGuardar = viewModel::guardar,
    )
}

@Composable
fun EditarPerfilContenido(
    estado: EditarPerfilUiState,
    onAtras: () -> Unit,
    onReintentar: () -> Unit,
    onAvatar: (AvatarCashi) -> Unit,
    onNombre: (String) -> Unit,
    onMoneda: (Moneda) -> Unit,
    onDiaCobro: (Int?) -> Unit,
    onCambiarContrasena: () -> Unit,
    onGuardar: () -> Unit,
) {
    var eligiendoAvatar by remember { mutableStateOf(false) }
    var otroDia by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(titulo = stringResource(R.string.perfil_editar), onAtras = onAtras)
        when {
            estado.cargando -> EstadoCargando()
            estado.errorCarga != null -> EstadoError(mensaje = estado.errorCarga.mensaje(), onReintentar = onReintentar)
            else -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Espacio.pantalla, vertical = Espacio.s),
                    verticalArrangement = Arrangement.spacedBy(Espacio.m),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CirculoAvatar(estado.avatar, tamano = 104.dp, modifier = Modifier.clickable { eligiendoAvatar = true })
                        TextButton(onClick = { eligiendoAvatar = true }) {
                            Text(stringResource(R.string.editar_perfil_cambiar_avatar), fontWeight = FontWeight.Bold)
                        }
                    }

                    CashinCampoTexto(
                        etiqueta = stringResource(R.string.editar_perfil_nombre),
                        valor = estado.nombre,
                        onValorCambio = onNombre,
                        error = estado.errorNombre?.let { stringResource(it) },
                        capitalizar = true,
                    )

                    Etiqueta(stringResource(R.string.editar_perfil_moneda))
                    SelectorMoneda(estado.moneda, onMoneda)

                    Etiqueta(stringResource(R.string.editar_perfil_dia_cobro))
                    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                        DIAS_RAPIDOS.forEach { dia ->
                            ChipDia(texto = dia.toString(), elegido = estado.diaCobro == dia, onClick = { onDiaCobro(dia) })
                        }
                        val esOtro = estado.diaCobro != null && estado.diaCobro !in DIAS_RAPIDOS
                        ChipDia(
                            texto = if (esOtro) stringResource(R.string.editar_perfil_dia_elegido, estado.diaCobro!!) else stringResource(R.string.editar_perfil_dia_otro),
                            elegido = esOtro,
                            onClick = { if (esOtro) onDiaCobro(estado.diaCobro) else otroDia = true },
                        )
                    }
                    Text(
                        stringResource(R.string.editar_perfil_dia_ayuda),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (estado.puedeCambiarContrasena) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Espacio.m),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button, onClick = onCambiarContrasena)
                                .heightIn(min = 48.dp),
                        ) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                            Text(
                                stringResource(R.string.editar_perfil_cambiar_contrasena),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    estado.errorGuardar?.let {
                        Text(it.mensaje(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
                CashinBoton(
                    texto = stringResource(R.string.editar_perfil_guardar),
                    onClick = onGuardar,
                    cargando = estado.guardando,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Espacio.pantalla, vertical = Espacio.m),
                )
            }
        }
    }

    if (eligiendoAvatar) {
        HojaAvatar(actual = estado.avatar, onElegir = { onAvatar(it); eligiendoAvatar = false }, onCerrar = { eligiendoAvatar = false })
    }
    if (otroDia) {
        DialogoDia(onConfirmar = { onDiaCobro(it); otroDia = false }, onCancelar = { otroDia = false })
    }
}

@Composable
private fun Etiqueta(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 12.dp, bottom = 0.dp),
    )
}

@Composable
private fun ChipDia(texto: String, elegido: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = elegido,
        onClick = onClick,
        label = { Text(texto) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

/** "Bs | $us" como control segmentado. */
@Composable
private fun SelectorMoneda(moneda: Moneda, onMoneda: (Moneda) -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(3.dp)) {
            Moneda.entries.forEach { opcion ->
                val elegida = opcion == moneda
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (elegida) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .selectable(selected = elegida, role = Role.RadioButton, onClick = { onMoneda(opcion) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(if (opcion == Moneda.BOB) R.string.editar_perfil_moneda_bob else R.string.editar_perfil_moneda_usd),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (elegida) FontWeight.Bold else FontWeight.Medium,
                        color = if (elegida) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Hoja con las 6 poses de Cashi en una cuadrícula de 3 × 2. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaAvatar(actual: AvatarCashi, onElegir: (AvatarCashi) -> Unit, onCerrar: () -> Unit) {
    var elegido by remember { mutableStateOf(actual) }
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Espacio.pantalla).padding(bottom = Espacio.l).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Espacio.m),
        ) {
            Text(
                stringResource(R.string.editar_perfil_elige_cashi),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            AvatarCashi.entries.chunked(3).forEach { fila ->
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    fila.forEach { avatar ->
                        CirculoAvatar(
                            avatar = avatar,
                            tamano = 84.dp,
                            resaltado = avatar == elegido,
                            modifier = Modifier.selectable(selected = avatar == elegido, role = Role.RadioButton, onClick = { elegido = avatar }),
                        )
                    }
                }
            }
            CashinBoton(texto = stringResource(R.string.editar_perfil_usar_este), onClick = { onElegir(elegido) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** "Otro": escribir un día del 1 al 31. */
@Composable
private fun DialogoDia(onConfirmar: (Int) -> Unit, onCancelar: () -> Unit) {
    var texto by remember { mutableStateOf("") }
    val dia = texto.toIntOrNull()?.takeIf { it in 1..31 }
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = MaterialTheme.colorScheme.background,
        title = { Text(stringResource(R.string.editar_perfil_dia_titulo)) },
        text = {
            CashinCampoTexto(
                etiqueta = stringResource(R.string.editar_perfil_dia_campo),
                valor = texto,
                onValorCambio = { nuevo -> texto = nuevo.filter { it.isDigit() }.take(2) },
                tipoTeclado = KeyboardType.Number,
                error = if (texto.isNotEmpty() && dia == null) stringResource(R.string.editar_perfil_dia_error) else null,
            )
        },
        confirmButton = { TextButton(onClick = { dia?.let(onConfirmar) }, enabled = dia != null) { Text(stringResource(R.string.accion_aceptar)) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.accion_cancelar)) } },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA, heightDp = 800)
@Composable
private fun EditarPerfilPreview() = CashinTheme {
    EditarPerfilContenido(
        estado = EditarPerfilUiState(avatar = AvatarCashi.COOL, nombre = "Andres", diaCobro = 15, cargando = false),
        onAtras = {}, onReintentar = {}, onAvatar = {}, onNombre = {}, onMoneda = {}, onDiaCobro = {},
        onCambiarContrasena = {}, onGuardar = {},
    )
}
