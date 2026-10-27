package org.ucb.cashin.ui.registros

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import java.time.LocalDate

/**
 * HU-09: historial de anotaciones agrupado por día.
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
        onRegistroClick = onRegistroClick,
    )
}

/** Solo dibuja. Muestra uno de los 4 estados: cargando, error, vacío o la lista. */
@Composable
fun HistorialContenido(
    estado: HistorialUiState,
    onReintentar: () -> Unit,
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

            estado.dias.isEmpty() -> EstadoVacio(
                titulo = stringResource(R.string.historial_vacio_titulo),
                mensaje = stringResource(R.string.historial_vacio_mensaje),
                emocion = Emocion.FELIZ,
            )

            else -> ListaPorDia(dias = estado.dias, onRegistroClick = onRegistroClick)
        }
    }
}

/** Un encabezado por día ("Hoy", "Ayer", "Sábado 24 de octubre") y debajo sus anotaciones. */
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
                EncabezadoDia(fecha = dia.fecha)
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

@Composable
private fun EncabezadoDia(fecha: LocalDate) {
    Text(
        text = Formato.encabezadoDia(fecha),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = Espacio.s),
    )
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
            cargando = false,
        ),
        onReintentar = {},
        onRegistroClick = {},
    )
}