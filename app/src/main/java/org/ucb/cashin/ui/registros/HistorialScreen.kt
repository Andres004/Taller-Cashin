package org.ucb.cashin.ui.registros

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.model.Categoria
import org.ucb.cashin.model.Registro
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.Emocion
import org.ucb.cashin.ui.components.EstadoCargando
import org.ucb.cashin.ui.components.EstadoError
import org.ucb.cashin.ui.components.EstadoVacio
import org.ucb.cashin.ui.components.FilaRegistro
import org.ucb.cashin.ui.components.colorCategoria
import org.ucb.cashin.ui.components.iconoCategoria
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.util.Formato
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * HU-09: historial de anotaciones agrupado por día, con el total de cada día.
 * HU-10: filtros por tipo, categoría y fechas (hoy, esta semana, mes o rango personalizado).
 * HU-08: al tocar una anotación se abre para editarla o eliminarla.
 */
@Composable
fun HistorialScreen(
    onRegistroClick: (registroId: String) -> Unit,
    onAnotar: () -> Unit,
    viewModel: HistorialViewModel = viewModel(factory = HistorialViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Se recarga cada vez que se entra a la pantalla (después de anotar, editar o eliminar).
    LaunchedEffect(Unit) { viewModel.cargar() }

    HistorialContenido(
        estado = estado,
        onReintentar = viewModel::cargar,
        onFiltroCambio = viewModel::onFiltroCambio,
        onCategoriaCambio = viewModel::onCategoriaCambio,
        onPeriodoCambio = viewModel::onPeriodoCambio,
        onMesCambio = viewModel::onMesCambio,
        onRangoCambio = viewModel::onRangoCambio,
        onRegistroClick = onRegistroClick,
        onAnotar = onAnotar,
    )
}

/**
 * Solo dibuja. Estados: cargando, error, sin anotaciones (todavía no anotó nada),
 * filtros sin resultados, o la lista.
 */
@Composable
fun HistorialContenido(
    estado: HistorialUiState,
    onReintentar: () -> Unit,
    onFiltroCambio: (FiltroTipo) -> Unit,
    onCategoriaCambio: (categoriaId: String?) -> Unit,
    onPeriodoCambio: (FiltroPeriodo) -> Unit,
    onMesCambio: (YearMonth) -> Unit,
    onRangoCambio: (desde: LocalDate, hasta: LocalDate) -> Unit,
    onRegistroClick: (registroId: String) -> Unit,
    onAnotar: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(titulo = stringResource(R.string.nav_historial))

        when {
            estado.cargando -> EstadoCargando()

            estado.error != null -> EstadoError(
                mensaje = estado.error.mensaje(),
                onReintentar = onReintentar,
            )

            !estado.hayAnotaciones -> EstadoVacio(
                titulo = stringResource(R.string.historial_vacio_titulo),
                mensaje = stringResource(R.string.historial_vacio_mensaje),
                emocion = Emocion.FELIZ,
                textoAccion = stringResource(R.string.historial_vacio_accion),
                onAccion = onAnotar,
            )

            else -> {
                FiltrosHistorial(
                    estado = estado,
                    onFiltroCambio = onFiltroCambio,
                    onCategoriaCambio = onCategoriaCambio,
                    onPeriodoCambio = onPeriodoCambio,
                    onMesCambio = onMesCambio,
                    onRangoCambio = onRangoCambio,
                    modifier = Modifier.padding(horizontal = Espacio.pantalla),
                )

                if (estado.dias.isEmpty()) {
                    EstadoVacio(
                        titulo = stringResource(R.string.historial_sin_resultados_titulo),
                        mensaje = stringResource(R.string.historial_sin_resultados_mensaje),
                        emocion = Emocion.NEUTRAL,
                    )
                } else {
                    ListaPorDia(dias = estado.dias, onRegistroClick = onRegistroClick)
                }
            }
        }
    }
}

/**
 * Dos filas de chips que se acomodan si no entran:
 *  1. "Todo | Gastos | Ingresos" y la categoría.
 *  2. "Hoy | Esta semana | Octubre 2026 ▾ | Personalizado".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltrosHistorial(
    estado: HistorialUiState,
    onFiltroCambio: (FiltroTipo) -> Unit,
    onCategoriaCambio: (categoriaId: String?) -> Unit,
    onPeriodoCambio: (FiltroPeriodo) -> Unit,
    onMesCambio: (YearMonth) -> Unit,
    onRangoCambio: (desde: LocalDate, hasta: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mostrarCalendario by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Espacio.s),
            verticalArrangement = Arrangement.spacedBy(Espacio.xs),
        ) {
            FiltroTipo.entries.forEach { opcion ->
                FilterChip(
                    selected = opcion == estado.filtro,
                    onClick = { onFiltroCambio(opcion) },
                    label = { Text(stringResource(opcion.etiqueta())) },
                    colors = coloresDeChip(),
                )
            }
            SelectorCategoriaFiltro(
                categorias = estado.categoriasDisponibles,
                seleccionadaId = estado.categoriaId,
                onCategoriaCambio = onCategoriaCambio,
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Espacio.s),
            verticalArrangement = Arrangement.spacedBy(Espacio.xs),
        ) {
            FilterChip(
                selected = estado.periodo == FiltroPeriodo.HOY,
                onClick = { onPeriodoCambio(FiltroPeriodo.HOY) },
                label = { Text(stringResource(R.string.historial_periodo_hoy)) },
                colors = coloresDeChip(),
            )
            FilterChip(
                selected = estado.periodo == FiltroPeriodo.SEMANA,
                onClick = { onPeriodoCambio(FiltroPeriodo.SEMANA) },
                label = { Text(stringResource(R.string.historial_periodo_semana)) },
                colors = coloresDeChip(),
            )
            if (estado.mes != null) {
                SelectorMes(
                    mes = estado.mes,
                    meses = estado.mesesDisponibles,
                    seleccionado = estado.periodo == FiltroPeriodo.MES,
                    onMesCambio = onMesCambio,
                )
            }
            FilterChip(
                selected = estado.periodo == FiltroPeriodo.PERSONALIZADO,
                onClick = { mostrarCalendario = true },
                label = { Text(textoRango(estado)) },
                leadingIcon = { Icon(imageVector = Icons.Outlined.DateRange, contentDescription = null) },
                colors = coloresDeChip(),
            )
        }
    }

    if (mostrarCalendario) {
        CalendarioRango(
            rangoActual = estado.rango,
            onConfirmar = { desde, hasta ->
                mostrarCalendario = false
                onRangoCambio(desde, hasta)
            },
            onCancelar = { mostrarCalendario = false },
        )
    }
}

/** "Personalizado" o, si ya se eligió un rango, "24 oct. – 29 oct.". */
@Composable
private fun textoRango(estado: HistorialUiState): String {
    val rango = estado.rango
    return if (estado.periodo == FiltroPeriodo.PERSONALIZADO && rango != null) {
        stringResource(R.string.historial_rango, Formato.fechaCorta(rango.desde), Formato.fechaCorta(rango.hasta))
    } else {
        stringResource(R.string.historial_periodo_personalizado)
    }
}

/** Chip "Categoría ▾"; al tocarlo abre la lista con las categorías que tienen anotaciones. */
@Composable
private fun SelectorCategoriaFiltro(
    categorias: List<Categoria>,
    seleccionadaId: String?,
    onCategoriaCambio: (categoriaId: String?) -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }
    val seleccionada = categorias.firstOrNull { it.id == seleccionadaId }

    Box {
        FilterChip(
            selected = seleccionada != null,
            onClick = { abierto = true },
            label = { Text(seleccionada?.nombre ?: stringResource(R.string.historial_filtro_categoria)) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = stringResource(R.string.historial_elegir_categoria),
                )
            },
            colors = coloresDeChip(),
        )
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.historial_todas_categorias)) },
                onClick = {
                    abierto = false
                    onCategoriaCambio(null)
                },
            )
            categorias.forEach { categoria ->
                DropdownMenuItem(
                    text = { Text(categoria.nombre) },
                    leadingIcon = {
                        Icon(
                            imageVector = iconoCategoria(categoria.icono),
                            contentDescription = null,
                            tint = colorCategoria(categoria.color, MaterialTheme.colorScheme.primary),
                        )
                    },
                    onClick = {
                        abierto = false
                        onCategoriaCambio(categoria.id)
                    },
                )
            }
        }
    }
}

/** Chip con el mes; al tocarlo abre la lista de meses que tienen anotaciones. */
@Composable
private fun SelectorMes(
    mes: YearMonth,
    meses: List<YearMonth>,
    seleccionado: Boolean,
    onMesCambio: (YearMonth) -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = seleccionado,
            onClick = { abierto = true },
            label = { Text(nombreMes(mes)) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = stringResource(R.string.historial_elegir_mes),
                )
            },
            colors = coloresDeChip(),
        )
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            meses.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(nombreMes(opcion)) },
                    onClick = {
                        abierto = false
                        onMesCambio(opcion)
                    },
                )
            }
        }
    }
}

/**
 * Calendario para elegir "desde" y "hasta".
 * El calendario trabaja con milisegundos en UTC a la medianoche; por eso se convierte
 * con ZoneOffset.UTC (con la hora de Bolivia se correría un día).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarioRango(
    rangoActual: RangoFechas?,
    onConfirmar: (desde: LocalDate, hasta: LocalDate) -> Unit,
    onCancelar: () -> Unit,
) {
    val calendario = rememberDateRangePickerState(
        initialSelectedStartDateMillis = rangoActual?.desde?.aMilisUtc(),
        initialSelectedEndDateMillis = rangoActual?.hasta?.aMilisUtc(),
    )
    val inicio = calendario.selectedStartDateMillis
    val fin = calendario.selectedEndDateMillis

    DatePickerDialog(
        onDismissRequest = onCancelar,
        confirmButton = {
            TextButton(
                onClick = {
                    if (inicio != null && fin != null) onConfirmar(inicio.aFechaUtc(), fin.aFechaUtc())
                },
                enabled = inicio != null && fin != null,
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
        DateRangePicker(
            state = calendario,
            title = {
                Text(
                    text = stringResource(R.string.historial_elegir_rango),
                    modifier = Modifier.padding(start = Espacio.l, top = Espacio.m),
                )
            },
            showModeToggle = false,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun LocalDate.aMilisUtc(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.aFechaUtc(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** Los mismos colores que los chips de medio de pago en Anotar. */
@Composable
private fun coloresDeChip(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

/** Un encabezado por día ("Hoy", "Ayer", "lunes 26 de octubre") y debajo sus anotaciones. */
@Composable
private fun ListaPorDia(
    dias: List<DiaHistorial>,
    onRegistroClick: (registroId: String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = Espacio.pantalla, vertical = Espacio.s),
        verticalArrangement = Arrangement.spacedBy(Espacio.s),
    ) {
        dias.forEach { dia ->
            item(key = dia.fecha.toString()) {
                EncabezadoDia(dia = dia)
            }
            items(items = dia.registros, key = { it.registro.id }) { anotacion ->
                FilaRegistro(
                    registro = anotacion.registro,
                    categoria = anotacion.categoria,
                    onClick = { onRegistroClick(anotacion.registro.id) },
                )
            }
        }
    }
}

/** Nombre del día a la izquierda y, a la derecha, cuánto suma ese día ("- Bs 32,50"). */
@Composable
private fun EncabezadoDia(dia: DiaHistorial) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Espacio.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = Formato.encabezadoDia(dia.fecha),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = Formato.montoConSigno(dia.total.abs(), esGasto = dia.total.signum() < 0),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** YearMonth(2026, 10) → "Octubre 2026". */
private fun nombreMes(mes: YearMonth): String = Formato.mes(mes.atDay(1))

@StringRes
private fun FiltroTipo.etiqueta(): Int = when (this) {
    FiltroTipo.TODO -> R.string.historial_filtro_todo
    FiltroTipo.GASTOS -> R.string.historial_filtro_gastos
    FiltroTipo.INGRESOS -> R.string.historial_filtro_ingresos
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun HistorialPreview() = CashinTheme {
    val comida = Categoria(id = "c1", nombre = "Alimentación", tipo = TipoRegistro.GASTO, icono = "restaurant", color = "#4CAF50")
    val sueldo = Categoria(id = "c2", nombre = "Sueldo", tipo = TipoRegistro.INGRESO, icono = "work", color = "#2E7D32")
    val ahora = Instant.now()
    HistorialContenido(
        estado = HistorialUiState(
            dias = listOf(
                DiaHistorial(
                    fecha = Formato.hoy(),
                    registros = listOf(
                        RegistroConCategoria(
                            Registro(id = "1", usuarioId = "u", categoriaId = "c1", tipo = TipoRegistro.GASTO, monto = BigDecimal("20.00"), fechaHora = ahora, nota = "Almuerzo"),
                            comida,
                        ),
                        RegistroConCategoria(
                            Registro(id = "2", usuarioId = "u", categoriaId = "c2", tipo = TipoRegistro.INGRESO, monto = BigDecimal("3200.00"), fechaHora = ahora),
                            sueldo,
                        ),
                    ),
                ),
            ),
            mes = YearMonth.now(),
            mesesDisponibles = listOf(YearMonth.now()),
            categoriasDisponibles = listOf(comida, sueldo),
            hayAnotaciones = true,
            cargando = false,
        ),
        onReintentar = {},
        onFiltroCambio = {},
        onCategoriaCambio = {},
        onPeriodoCambio = {},
        onMesCambio = {},
        onRangoCambio = { _, _ -> },
        onRegistroClick = {},
        onAnotar = {},
    )
}