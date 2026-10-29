package org.ucb.cashin.ui.planificados

import androidx.annotation.StringRes
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
import org.ucb.cashin.ui.components.CashinTarjeta
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

/** Solo dibuja el formulario. */
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
    val esGasto = estado.tipo == TipoRegistro.GASTO

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
            // Gasto | Ingreso
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                TipoRegistro.entries.forEach { opcion ->
                    FilterChip(
                        selected = opcion == estado.tipo,
                        onClick = { onTipoCambio(opcion) },
                        enabled = !estado.guardando,
                        label = { Text(stringResource(opcion.etiqueta())) },
                        colors = coloresDeChip(),
                    )
                }
            }

            CashinCampoTexto(
                etiqueta = stringResource(R.string.planificados_nombre),
                valor = estado.nombre,
                onValorCambio = onNombreCambio,
                placeholder = stringResource(
                    if (esGasto) R.string.planificados_nombre_ejemplo_gasto else R.string.planificados_nombre_ejemplo_ingreso,
                ),
                error = estado.errorNombre?.let { stringResource(it) },
                accionIme = ImeAction.Next,
                habilitado = !estado.guardando,
                capitalizar = true,
            )

            CashinCampoMonto(
                valor = estado.monto,
                onValorCambio = onMontoCambio,
                etiqueta = stringResource(R.string.anotar_monto),
                error = estado.errorMonto?.let { stringResource(it) },
                colorMonto = if (esGasto) CashinColores.gasto else CashinColores.ingreso,
                habilitado = !estado.guardando,
            )

            Column {
                EtiquetaSeccion(stringResource(R.string.anotar_categoria))
                SeccionCategorias(
                    estado = estado,
                    onCategoriaElegida = onCategoriaElegida,
                    onReintentar = onReintentarCategorias,
                )
                estado.errorCategoria?.let { MensajeError(stringResource(it)) }
            }

            Column {
                EtiquetaSeccion(stringResource(R.string.anotar_medio_pago))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Espacio.s),
                    verticalArrangement = Arrangement.spacedBy(Espacio.xs),
                ) {
                    MedioPago.entries.forEach { opcion ->
                        FilterChip(
                            selected = opcion == estado.medioPago,
                            onClick = { onMedioPagoCambio(opcion) },
                            enabled = !estado.guardando,
                            label = { Text(stringResource(opcion.etiqueta())) },
                            colors = coloresDeChip(),
                        )
                    }
                }
            }

            Column {
                EtiquetaSeccion(stringResource(R.string.planificados_frecuencia))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Espacio.s),
                    verticalArrangement = Arrangement.spacedBy(Espacio.xs),
                ) {
                    Frecuencia.entries.forEach { opcion ->
                        FilterChip(
                            selected = opcion == estado.frecuencia,
                            onClick = { onFrecuenciaCambio(opcion) },
                            enabled = !estado.guardando,
                            label = { Text(stringResource(opcion.etiqueta())) },
                            colors = coloresDeChip(),
                        )
                    }
                }
            }

            Column {
                EtiquetaSeccion(
                    stringResource(if (esGasto) R.string.planificados_fecha_pago else R.string.planificados_fecha_cobro),
                )
                CashinTarjeta(onClick = { if (!estado.guardando) mostrarCalendario = true }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Espacio.m),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            text = Formato.encabezadoDia(estado.proximaFecha),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
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

/** Grilla de categorías, o lo que corresponda mientras carga, si falla o si no hay ninguna. */
@Composable
private fun SeccionCategorias(
    estado: NuevoPlanificadoUiState,
    onCategoriaElegida: (Categoria) -> Unit,
    onReintentar: () -> Unit,
) {
    when {
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
 * Calendario de un solo día. Trabaja con milisegundos en UTC a la medianoche,
 * por eso se convierte con ZoneOffset.UTC (con la hora de Bolivia se correría un día).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarioFecha(
    fecha: LocalDate,
    onConfirmar: (LocalDate) -> Unit,
    onCancelar: () -> Unit,
) {
    val calendario = rememberDatePickerState(
        initialSelectedDateMillis = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
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

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA, heightDp = 1000)
@Composable
private fun NuevoPlanificadoPreview() = CashinTheme {
    NuevoPlanificadoContenido(
        estado = NuevoPlanificadoUiState(
            nombre = "Alquiler",
            monto = "1500",
            cargandoCategorias = false,
            categorias = listOf(
                Categoria(id = "1", nombre = "Vivienda", tipo = TipoRegistro.GASTO, icono = "home", color = "#81C784"),
                Categoria(id = "2", nombre = "Transporte", tipo = TipoRegistro.GASTO, icono = "bus", color = "#2E7D32"),
            ),
            categoriaId = "1",
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