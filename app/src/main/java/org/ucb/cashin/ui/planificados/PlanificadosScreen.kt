package org.ucb.cashin.ui.planificados

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.model.Categoria
import org.ucb.cashin.model.Frecuencia
import org.ucb.cashin.model.RegistroPlanificado
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.model.estaVencido
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinTarjeta
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.Emocion
import org.ucb.cashin.ui.components.EstadoCargando
import org.ucb.cashin.ui.components.EstadoError
import org.ucb.cashin.ui.components.EstadoVacio
import org.ucb.cashin.ui.components.colorCategoria
import org.ucb.cashin.ui.components.iconoCategoria
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.util.Formato
import java.math.BigDecimal

/**
 * HU-15: pagos e ingresos esperados (alquiler, sueldo...).
 * HU-16: "Ya lo pagué / cobré" los convierte en una anotación del historial.
 */
@Composable
fun PlanificadosScreen(
    onAtras: () -> Unit,
    onNuevo: () -> Unit,
    onPagoAnotado: (TipoRegistro) -> Unit,
    viewModel: PlanificadosViewModel = viewModel(factory = PlanificadosViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Se recarga cada vez que se entra (por ejemplo al volver de crear uno nuevo).
    LaunchedEffect(Unit) { viewModel.cargar() }

    LaunchedEffect(estado.pagoAnotado) {
        estado.pagoAnotado?.let { tipo ->
            onPagoAnotado(tipo)
            viewModel.avisoMostrado()
        }
    }

    PlanificadosContenido(
        estado = estado,
        onAtras = onAtras,
        onReintentar = viewModel::cargar,
        onNuevo = onNuevo,
        onRegistrarPago = viewModel::registrarPago,
        onQuitar = viewModel::quitar,
    )
}

/** Solo dibuja. Estados: cargando, error, sin planificados o la lista. */
@Composable
fun PlanificadosContenido(
    estado: PlanificadosUiState,
    onAtras: () -> Unit,
    onReintentar: () -> Unit,
    onNuevo: () -> Unit,
    onRegistrarPago: (RegistroPlanificado) -> Unit,
    onQuitar: (RegistroPlanificado) -> Unit,
) {
    var porQuitar by remember { mutableStateOf<RegistroPlanificado?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(titulo = stringResource(R.string.perfil_planificados), onAtras = onAtras)

        Box(modifier = Modifier.weight(1f)) {
            when {
                estado.cargando -> EstadoCargando()

                estado.error != null -> EstadoError(mensaje = estado.error.mensaje(), onReintentar = onReintentar)

                estado.planificados.isEmpty() -> EstadoVacio(
                    titulo = stringResource(R.string.planificados_vacio_titulo),
                    mensaje = stringResource(R.string.planificados_vacio_mensaje),
                    emocion = Emocion.FELIZ,
                )

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    if (estado.errorAccion != null) {
                        Text(
                            text = estado.errorAccion.mensaje(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = Espacio.pantalla, vertical = Espacio.s),
                        )
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = Espacio.pantalla, vertical = Espacio.s),
                        verticalArrangement = Arrangement.spacedBy(Espacio.s),
                    ) {
                        items(estado.planificados, key = { it.planificado.id }) { item ->
                            TarjetaPlanificado(
                                item = item,
                                procesando = estado.procesandoId == item.planificado.id,
                                bloqueado = estado.procesandoId != null,
                                onRegistrarPago = { onRegistrarPago(item.planificado) },
                                onQuitar = { porQuitar = item.planificado },
                            )
                        }
                    }
                }
            }
        }

        CashinBoton(
            texto = stringResource(R.string.planificados_nuevo),
            onClick = onNuevo,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = Espacio.pantalla, vertical = Espacio.m),
        )
    }

    porQuitar?.let { planificado ->
        AlertDialog(
            onDismissRequest = { porQuitar = null },
            title = { Text(stringResource(R.string.planificados_quitar_titulo)) },
            text = { Text(stringResource(R.string.planificados_quitar_mensaje, planificado.nombre)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        porQuitar = null
                        onQuitar(planificado)
                    },
                ) {
                    Text(stringResource(R.string.planificados_quitar), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { porQuitar = null }) {
                    Text(stringResource(R.string.accion_cancelar))
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }
}

/**
 * Arriba: ícono, nombre, "Mensual · 5 nov." y el monto.
 * Abajo: "Quitar" y "Ya lo pagué" (gasto) o "Ya lo cobré" (ingreso).
 * Si la fecha ya pasó, la línea de la fecha se pinta en rojo con "Vencido".
 */
@Composable
private fun TarjetaPlanificado(
    item: PlanificadoConCategoria,
    procesando: Boolean,
    bloqueado: Boolean,
    onRegistrarPago: () -> Unit,
    onQuitar: () -> Unit,
) {
    val planificado = item.planificado
    val esGasto = planificado.tipo == TipoRegistro.GASTO
    val color = colorCategoria(item.categoria?.color, MaterialTheme.colorScheme.primary)
    val hoy = Formato.hoy()
    val vencido = planificado.estaVencido(hoy)
    val fecha = when (planificado.proximaFecha) {
        hoy -> stringResource(R.string.planificados_hoy)
        else -> Formato.fechaCorta(planificado.proximaFecha)
    }

    CashinTarjeta {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacio.m),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconoCategoria(item.categoria?.icono),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = planificado.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (vencido) {
                        stringResource(R.string.planificados_vencido, fecha)
                    } else {
                        stringResource(R.string.planificados_frecuencia_fecha, stringResource(planificado.frecuencia.etiqueta()), fecha)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (vencido) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = Formato.montoConSigno(planificado.monto, esGasto),
                style = MaterialTheme.typography.titleMedium,
                color = if (esGasto) CashinColores.gasto else CashinColores.ingreso,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Espacio.s),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onQuitar, enabled = !bloqueado) {
                Text(stringResource(R.string.planificados_quitar), color = MaterialTheme.colorScheme.error)
            }
            TextButton(onClick = onRegistrarPago, enabled = !bloqueado) {
                Text(
                    stringResource(
                        when {
                            procesando -> R.string.planificados_anotando
                            esGasto -> R.string.planificados_ya_pague
                            else -> R.string.planificados_ya_cobre
                        },
                    ),
                )
            }
        }
    }
}

@StringRes
fun Frecuencia.etiqueta(): Int = when (this) {
    Frecuencia.UNICA -> R.string.frecuencia_unica
    Frecuencia.SEMANAL -> R.string.frecuencia_semanal
    Frecuencia.QUINCENAL -> R.string.frecuencia_quincenal
    Frecuencia.MENSUAL -> R.string.frecuencia_mensual
    Frecuencia.ANUAL -> R.string.frecuencia_anual
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun PlanificadosPreview() = CashinTheme {
    val vivienda = Categoria(id = "c1", nombre = "Vivienda", tipo = TipoRegistro.GASTO, icono = "home", color = "#81C784")
    val sueldo = Categoria(id = "c2", nombre = "Sueldo", tipo = TipoRegistro.INGRESO, icono = "work", color = "#2E7D32")
    PlanificadosContenido(
        estado = PlanificadosUiState(
            cargando = false,
            planificados = listOf(
                PlanificadoConCategoria(
                    RegistroPlanificado(
                        id = "1", usuarioId = "u", categoriaId = "c1", tipo = TipoRegistro.GASTO,
                        nombre = "Alquiler", monto = BigDecimal("1500.00"), frecuencia = Frecuencia.MENSUAL,
                        proximaFecha = Formato.hoy().minusDays(2),
                    ),
                    vivienda,
                ),
                PlanificadoConCategoria(
                    RegistroPlanificado(
                        id = "2", usuarioId = "u", categoriaId = "c2", tipo = TipoRegistro.INGRESO,
                        nombre = "Sueldo", monto = BigDecimal("3200.00"), frecuencia = Frecuencia.MENSUAL,
                        proximaFecha = Formato.hoy().plusDays(5),
                    ),
                    sueldo,
                ),
            ),
        ),
        onAtras = {},
        onReintentar = {},
        onNuevo = {},
        onRegistrarPago = {},
        onQuitar = {},
    )
}