package org.ucb.cashin.ui.planificados

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
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
import org.ucb.cashin.model.Categoria
import org.ucb.cashin.model.Frecuencia
import org.ucb.cashin.model.MedioPago
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinCampoMonto
import org.ucb.cashin.ui.components.CashinCampoTexto
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.SelectorCategoria
import org.ucb.cashin.ui.components.VarianteBoton
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.util.Formato
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** HU-15: formulario para anotar un pago o ingreso esperado. */
@Composable
fun NuevoPlanificadoScreen(
    onAtras: () -> Unit,
    onGuardado: () -> Unit,
    viewModel: NuevoPlanificadoViewModel = viewModel(factory = NuevoPlanificadoViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(estado.guardado) {
        if (estado.guardado) onGuardado()
    }

    NuevoPlanificadoContenido(
        estado = estado,
        onAtras = onAtras,
        onTipoCambio = viewModel::onTipoCambio,
        onNombreCambio = viewModel::onNombreCambio,
        onMontoCambio = viewModel::onMontoCambio,
        onCategoriaElegida = viewModel::onCategoriaElegida,
        onReintentarCategorias = viewModel::cargarCategorias,
        onMedioPagoCambio = viewModel::onMedioPagoCambio,
        onFrecuenciaCambio = viewModel::onFrecuenciaCambio,
        onFechaCambio = viewModel::onFechaCambio,
        onGuardar = viewModel::guardar,
    )
}

/**
 * Solo dibuja el formulario. Cada sección marca su error debajo y,
 * en las de chips, los bordes se pintan de rojo hasta que se elija una opción.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NuevoPlanificadoContenido(
    estado: NuevoPlanificadoUiState,
    onAtras: () -> Unit,
    onTipoCambio: (TipoRegistro) -> Unit,
    onNombreCambio: (String) -> Unit,
    onMontoCambio: (String) -> Unit,
    onCategoriaElegida: (Categoria) -> Unit,
    onReintentarCategorias: () -> Unit,
    onMedioPagoCambio: (MedioPago) -> Unit,
    onFrecuenciaCambio: (Frecuencia) -> Unit,
    onFechaCambio: (LocalDate) -> Unit,
    onGuardar: () -> Unit,
) {
    var mostrarCalendario by remember { mutableStateOf(false) }
    val habilitado = !estado.guardando
    val colorMonto = when (estado.tipo) {
        TipoRegistro.GASTO -> CashinColores.gasto
        TipoRegistro.INGRESO -> CashinColores.ingreso
        null -> MaterialTheme.colorScheme.onBackground
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(titulo = stringResource(R.string.planificados_nuevo), onAtras = onAtras)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = Espacio.pantalla, vertical = Espacio.s),
            verticalArrangement = Arrangement.spacedBy(Espacio.l),
        ) {
            // 1. Gasto | Ingreso
            Column {
                EtiquetaSeccion(stringResource(R.string.planificados_tipo))
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    TipoRegistro.entries.forEach { opcion ->
                        ChipOpcion(
                            texto = stringResource(opcion.etiqueta()),
                            elegido = opcion == estado.tipo,
                            conError = estado.errorTipo != null,
                            habilitado = habilitado,
                            onClick = { onTipoCambio(opcion) },
                        )
                    }
                }
                estado.errorTipo?.let { MensajeError(stringResource(it)) }
            }

            // 2. Nombre
            CashinCampoTexto(
                etiqueta = stringResource(R.string.planificados_nombre),
                valor = estado.nombre,
                onValorCambio = onNombreCambio,
                placeholder = stringResource(
                    if (estado.tipo == TipoRegistro.INGRESO) R.string.planificados_nombre_ejemplo_ingreso
                    else R.string.planificados_nombre_ejemplo_gasto,
                ),
                error = estado.errorNombre?.let { stringResource(it) },
                accionIme = ImeAction.Next,
                habilitado = habilitado,
                capitalizar = true,
            )

            // 3. Monto
            CashinCampoMonto(
                valor = estado.monto,
                onValorCambio = onMontoCambio,
                etiqueta = stringResource(R.string.anotar_monto),
                error = estado.errorMonto?.let { stringResource(it) },
                colorMonto = colorMonto,
                habilitado = habilitado,
            )

            // 4. Categoría (depende del tipo)
            Column {
                EtiquetaSeccion(stringResource(R.string.anotar_categoria))
                SeccionCategorias(
                    estado = estado,
                    onCategoriaElegida = onCategoriaElegida,
                    onReintentar = onReintentarCategorias,
                )
                estado.errorCategoria?.let { MensajeError(stringResource(it)) }
            }

            // 5. Medio de pago
            Column {
                EtiquetaSeccion(stringResource(R.string.anotar_medio_pago))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Espacio.s),
                    verticalArrangement = Arrangement.spacedBy(Espacio.xs),
                ) {
                    MedioPago.entries.forEach { opcion ->
                        ChipOpcion(
                            texto = stringResource(opcion.etiqueta()),
                            elegido = opcion == estado.medioPago,
                            conError = estado.errorMedioPago != null,
                            habilitado = habilitado,
                            onClick = { onMedioPagoCambio(opcion) },
                        )
                    }
                }
                estado.errorMedioPago?.let { MensajeError(stringResource(it)) }
            }

            // 6. Frecuencia
            Column {
                EtiquetaSeccion(stringResource(R.string.planificados_frecuencia))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Espacio.s),
                    verticalArrangement = Arrangement.spacedBy(Espacio.xs),
                ) {
                    Frecuencia.entries.forEach { opcion ->
                        ChipOpcion(
                            texto = stringResource(opcion.etiqueta()),
                            elegido = opcion == estado.frecuencia,
                            conError = estado.errorFrecuencia != null,
                            habilitado = habilitado,
                            onClick = { onFrecuenciaCambio(opcion) },
                        )
                    }
                }
                estado.errorFrecuencia?.let { MensajeError(stringResource(it)) }
            }

            // 7. Próxima fecha
            Column {
                EtiquetaSeccion(
                    stringResource(
                        if (estado.tipo == TipoRegistro.INGRESO) R.string.planificados_fecha_cobro
                        else R.string.planificados_fecha_pago,
                    ),
                )
                CampoFecha(
                    fecha = estado.proximaFecha,
                    conError = estado.errorFecha != null,
                    habilitado = habilitado,
                    onClick = { mostrarCalendario = true },
                )
                estado.errorFecha?.let { MensajeError(stringResource(it)) }
            }

            estado.error?.let { MensajeError(it.mensaje()) }

            CashinBoton(
                texto = stringResource(R.string.anotar_guardar),
                onClick = onGuardar,
                cargando = estado.guardando,
            )
        }
    }

    if (mostrarCalendario) {
        CalendarioFecha(
            fecha = estado.proximaFecha,
            onConfirmar = { fecha ->
                mostrarCalendario = false
                onFechaCambio(fecha)
            },
            onCancelar = { mostrarCalendario = false },
        )
    }
}

/** Chip de una opción. Con [conError] el borde se pinta de rojo, igual que en Anotar. */
@Composable
private fun ChipOpcion(
    texto: String,
    elegido: Boolean,
    conError: Boolean,
    habilitado: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = elegido,
        onClick = onClick,
        enabled = habilitado,
        label = { Text(texto) },
        colors = coloresDeChip(),
        border = FilterChipDefaults.filterChipBorder(
            enabled = habilitado,
            selected = elegido,
            borderColor = if (conError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

/** "Elige una fecha" o "Hoy" / "Ayer" / "jueves 5 de noviembre". Al tocarlo abre el calendario. */
@Composable
private fun CampoFecha(
    fecha: LocalDate?,
    conError: Boolean,
    habilitado: Boolean,
    onClick: () -> Unit,
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
            modifier = Modifier.padding(horizontal = Espacio.m, vertical = 14.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = fecha?.let { Formato.encabezadoDia(it) } ?: stringResource(R.string.planificados_elegir_fecha),
                style = MaterialTheme.typography.bodyLarge,
                color = if (fecha == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

/** Grilla de categorías, o lo que corresponda: sin tipo, cargando, error o vacía. */
@Composable
private fun SeccionCategorias(
    estado: NuevoPlanificadoUiState,
    onCategoriaElegida: (Categoria) -> Unit,
    onReintentar: () -> Unit,
) {
    when {
        estado.tipo == null -> Text(
            text = stringResource(R.string.planificados_elige_tipo_primero),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp),
        )

        estado.cargandoCategorias -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Espacio.m),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        estado.errorCategorias != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MensajeError(estado.errorCategorias.mensaje())
            CashinBoton(
                texto = stringResource(R.string.estado_reintentar),
                onClick = onReintentar,
                variante = VarianteBoton.TEXTO,
            )
        }

        estado.categorias.isEmpty() -> Text(
            text = stringResource(R.string.anotar_sin_categorias_mensaje),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        else -> SelectorCategoria(
            categorias = estado.categorias,
            seleccionadaId = estado.categoriaId,
            onSeleccionar = onCategoriaElegida,
            conError = estado.errorCategoria != null,
            habilitado = !estado.guardando,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Calendario de un solo día que solo deja elegir desde hoy en adelante.
 * Trabaja con milisegundos en UTC a la medianoche, por eso se convierte
 * con ZoneOffset.UTC (con la hora de Bolivia se correría un día).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarioFecha(
    fecha: LocalDate?,
    onConfirmar: (LocalDate) -> Unit,
    onCancelar: () -> Unit,
) {
    val hoyMilis = Formato.hoy().aMilisUtc()
    val calendario = rememberDatePickerState(
        initialSelectedDateMillis = (fecha ?: Formato.hoy()).aMilisUtc(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= hoyMilis
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
private fun coloresDeChip(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

@Composable
private fun EtiquetaSeccion(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
    )
}

@Composable
private fun MensajeError(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
    )
}

@StringRes
private fun TipoRegistro.etiqueta(): Int = when (this) {
    TipoRegistro.GASTO -> R.string.anotar_tipo_gasto
    TipoRegistro.INGRESO -> R.string.anotar_tipo_ingreso
}

@StringRes
private fun MedioPago.etiqueta(): Int = when (this) {
    MedioPago.EFECTIVO -> R.string.anotar_medio_efectivo
    MedioPago.QR -> R.string.anotar_medio_qr
    MedioPago.TARJETA -> R.string.anotar_medio_tarjeta
    MedioPago.TRANSFERENCIA -> R.string.anotar_medio_transferencia
    MedioPago.OTRO -> R.string.anotar_medio_otro
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA, heightDp = 1100)
@Composable
private fun NuevoPlanificadoConErroresPreview() = CashinTheme {
    NuevoPlanificadoContenido(
        estado = NuevoPlanificadoUiState(
            errorTipo = R.string.validacion_tipo,
            errorNombre = R.string.planificados_error_nombre,
            errorMonto = R.string.validacion_monto,
            errorMedioPago = R.string.validacion_medio_pago,
            errorFrecuencia = R.string.validacion_frecuencia,
            errorFecha = R.string.validacion_fecha,
        ),
        onAtras = {},
        onTipoCambio = {},
        onNombreCambio = {},
        onMontoCambio = {},
        onCategoriaElegida = {},
        onReintentarCategorias = {},
        onMedioPagoCambio = {},
        onFrecuenciaCambio = {},
        onFechaCambio = {},
        onGuardar = {},
    )
}