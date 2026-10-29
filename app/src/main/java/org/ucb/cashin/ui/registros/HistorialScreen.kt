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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Text
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
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.util.Formato
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth

/**
 * HU-09: historial de anotaciones agrupado por día, con el total de cada día.
 * HU-10 (parte): filtros por tipo y por mes.
 * HU-08: al tocar una anotación se abre para editarla o eliminarla.
 */
@Composable
fun HistorialScreen(
    onRegistroClick: (registroId: String) -> Unit,
    viewModel: HistorialViewModel = viewModel(factory = HistorialViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Se recarga cada vez que se entra a la pantalla (después de anotar, editar o eliminar).
    LaunchedEffect(Unit) { viewModel.cargar() }

    HistorialContenido(
        estado = estado,
        onReintentar = viewModel::cargar,
        onFiltroCambio = viewModel::onFiltroCambio,
        onMesCambio = viewModel::onMesCambio,
        onRegistroClick = onRegistroClick,
    )
}

/**
 * Solo dibuja. Estados: cargando, error, sin anotaciones (todavía no anotó nada),
 * filtro sin resultados, o la lista.
 */
@Composable
fun HistorialContenido(
    estado: HistorialUiState,
    onReintentar: () -> Unit,
    onFiltroCambio: (FiltroTipo) -> Unit,
    onMesCambio: (YearMonth) -> Unit,
    onRegistroClick: (registroId: String) -> Unit,
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
            )

            else -> {
                FiltrosHistorial(
                    filtro = estado.filtro,
                    mes = estado.mes,
                    meses = estado.mesesDisponibles,
                    onFiltroCambio = onFiltroCambio,
                    onMesCambio = onMesCambio,
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

/** "Todo | Gastos | Ingresos" y el selector de mes, en una fila que se acomoda si no entra. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltrosHistorial(
    filtro: FiltroTipo,
    mes: YearMonth?,
    meses: List<YearMonth>,
    onFiltroCambio: (FiltroTipo) -> Unit,
    onMesCambio: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Espacio.s),
        verticalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        FiltroTipo.entries.forEach { opcion ->
            FilterChip(
                selected = opcion == filtro,
                onClick = { onFiltroCambio(opcion) },
                label = { Text(stringResource(opcion.etiqueta())) },
                colors = coloresDeChip(),
            )
        }
        if (mes != null) {
            SelectorMes(mes = mes, meses = meses, onMesCambio = onMesCambio)
        }
    }
}

/** Chip con el mes elegido; al tocarlo abre la lista de meses que tienen anotaciones. */
@Composable
private fun SelectorMes(
    mes: YearMonth,
    meses: List<YearMonth>,
    onMesCambio: (YearMonth) -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = false,
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
            hayAnotaciones = true,
            cargando = false,
        ),
        onReintentar = {},
        onFiltroCambio = {},
        onMesCambio = {},
        onRegistroClick = {},
    )
}