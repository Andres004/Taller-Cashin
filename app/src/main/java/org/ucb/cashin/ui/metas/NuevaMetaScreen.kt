package org.ucb.cashin.ui.metas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinCampoMonto
import org.ucb.cashin.ui.components.CashinCampoTexto
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.EstadoCargando
import org.ucb.cashin.ui.components.EstadoError
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.util.Formato
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * HU-17: formulario para crear una meta.
 * Con un [metaId] sirve para editarla (mismos campos y mismas validaciones).
 */
@Composable
fun NuevaMetaScreen(
    onAtras: () -> Unit,
    onGuardada: (metaId: String) -> Unit,
    metaId: String? = null,
    viewModel: NuevaMetaViewModel = viewModel(factory = NuevaMetaViewModel.fabrica(metaId)),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(estado.metaGuardadaId) {
        estado.metaGuardadaId?.let { onGuardada(it) }
    }

    NuevaMetaContenido(
        estado = estado,
        onAtras = onAtras,
        onReintentar = viewModel::cargarMeta,
        onNombreCambio = viewModel::onNombreCambio,
        onMontoCambio = viewModel::onMontoCambio,
        onFechaCambio = viewModel::onFechaCambio,
        onDescripcionCambio = viewModel::onDescripcionCambio,
        onGuardar = viewModel::guardar,
    )
}

/** Solo dibuja el formulario. En edición también muestra la carga y el error al traer la meta. */
@Composable
fun NuevaMetaContenido(
    estado: NuevaMetaUiState,
    onAtras: () -> Unit,
    onReintentar: () -> Unit,
    onNombreCambio: (String) -> Unit,
    onMontoCambio: (String) -> Unit,
    onFechaCambio: (LocalDate?) -> Unit,
    onDescripcionCambio: (String) -> Unit,
    onGuardar: () -> Unit,
) {
    var mostrarCalendario by remember { mutableStateOf(false) }
    val habilitado = !estado.guardando

    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(
            titulo = stringResource(if (estado.esEdicion) R.string.metas_editar else R.string.metas_nueva),
            onAtras = onAtras,
        )

        when {
            estado.cargandoMeta -> EstadoCargando()

            estado.errorMeta != null -> EstadoError(mensaje = estado.errorMeta.mensaje(), onReintentar = onReintentar)

            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = Espacio.pantalla, vertical = Espacio.s),
                verticalArrangement = Arrangement.spacedBy(Espacio.l),
            ) {
                CashinCampoTexto(
                    etiqueta = stringResource(R.string.metas_nombre),
                    valor = estado.nombre,
                    onValorCambio = onNombreCambio,
                    placeholder = stringResource(R.string.metas_nombre_ejemplo),
                    error = estado.errorNombre?.let { stringResource(it) },
                    accionIme = ImeAction.Next,
                    habilitado = habilitado,
                    capitalizar = true,
                )

                CashinCampoMonto(
                    valor = estado.montoObjetivo,
                    onValorCambio = onMontoCambio,
                    etiqueta = stringResource(R.string.metas_monto_objetivo),
                    error = estado.errorMonto?.let { stringResource(it) },
                    colorMonto = MaterialTheme.colorScheme.secondary,
                    habilitado = habilitado,
                )

                CashinCampoTexto(
                    etiqueta = stringResource(R.string.metas_descripcion),
                    valor = estado.descripcion,
                    onValorCambio = onDescripcionCambio,
                    placeholder = stringResource(R.string.metas_descripcion_ejemplo),
                    error = estado.errorDescripcion?.let { stringResource(it) },
                    accionIme = ImeAction.Done,
                    habilitado = habilitado,
                    capitalizar = true,
                )

                Column {
                    Text(
                        text = stringResource(R.string.metas_fecha_limite),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
                    )
                    CampoFechaOpcional(
                        fecha = estado.fechaLimite,
                        conError = estado.errorFecha != null,
                        habilitado = habilitado,
                        onClick = { mostrarCalendario = true },
                        onQuitar = { onFechaCambio(null) },
                    )
                    estado.errorFecha?.let { MensajeError(stringResource(it)) }
                }

                // Aviso junto al botón: así se nota aunque el campo con error haya quedado fuera de la pantalla.
                if (estado.hayErrores) MensajeError(stringResource(R.string.formulario_revisa_campos))
                estado.error?.let { MensajeError(it.mensaje()) }

                CashinBoton(
                    texto = stringResource(if (estado.esEdicion) R.string.anotar_guardar_cambios else R.string.metas_crear),
                    onClick = onGuardar,
                    cargando = estado.guardando,
                )
            }
        }
    }

    if (mostrarCalendario) {
        CalendarioFechaLimite(
            fecha = estado.fechaLimite,
            onConfirmar = { fecha ->
                mostrarCalendario = false
                onFechaCambio(fecha)
            },
            onCancelar = { mostrarCalendario = false },
        )
    }
}

/** "Sin fecha límite" o la fecha elegida con una ✕ para quitarla. */
@Composable
private fun CampoFechaOpcional(
    fecha: LocalDate?,
    conError: Boolean,
    habilitado: Boolean,
    onClick: () -> Unit,
    onQuitar: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = habilitado,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (conError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacio.m),
            modifier = Modifier.padding(start = Espacio.m, end = Espacio.xs, top = 4.dp, bottom = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = fecha?.let { Formato.encabezadoDia(it) } ?: stringResource(R.string.metas_sin_fecha_opcional),
                style = MaterialTheme.typography.bodyLarge,
                color = if (fecha == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp),
            )
            if (fecha != null) {
                IconButton(onClick = onQuitar, enabled = habilitado) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.metas_quitar_fecha),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Calendario que solo deja elegir desde mañana. Trabaja con milisegundos en UTC
 * a la medianoche, por eso se convierte con ZoneOffset.UTC.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarioFechaLimite(
    fecha: LocalDate?,
    onConfirmar: (LocalDate) -> Unit,
    onCancelar: () -> Unit,
) {
    val manana = Formato.hoy().plusDays(1)
    val mananaMilis = manana.aMilisUtc()
    val calendario = rememberDatePickerState(
        initialSelectedDateMillis = fecha?.takeIf { !it.isBefore(manana) }?.aMilisUtc(),
        initialDisplayedMonthMillis = (fecha ?: manana).aMilisUtc(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= mananaMilis
        },
    )
    val elegida = calendario.selectedDateMillis
    val colores = DatePickerDefaults.colors(
        containerColor = MaterialTheme.colorScheme.background,
        selectedDayContainerColor = MaterialTheme.colorScheme.secondary,
        selectedDayContentColor = MaterialTheme.colorScheme.onSecondary,
        todayDateBorderColor = MaterialTheme.colorScheme.secondary,
        todayContentColor = MaterialTheme.colorScheme.secondary,
    )

    DatePickerDialog(
        onDismissRequest = onCancelar,
        colors = colores,
        confirmButton = {
            TextButton(
                onClick = {
                    if (elegida != null) {
                        onConfirmar(Instant.ofEpochMilli(elegida).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
                enabled = elegida != null,
            ) {
                Text(stringResource(R.string.accion_aceptar))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text(stringResource(R.string.accion_cancelar))
            }
        },
    ) {
        DatePicker(state = calendario, showModeToggle = false, colors = colores)
    }
}

private fun LocalDate.aMilisUtc(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

@Composable
private fun MensajeError(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun NuevaMetaConErroresPreview() = CashinTheme {
    NuevaMetaContenido(
        estado = NuevaMetaUiState(
            errorNombre = R.string.metas_error_nombre,
            errorMonto = R.string.validacion_monto,
            errorDescripcion = R.string.metas_error_descripcion,
        ),
        onAtras = {},
        onReintentar = {},
        onNombreCambio = {},
        onMontoCambio = {},
        onFechaCambio = {},
        onDescripcionCambio = {},
        onGuardar = {},
    )
}