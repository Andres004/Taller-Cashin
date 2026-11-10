package org.ucb.cashin.ui.metas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.model.AvanceMeta
import org.ucb.cashin.model.EstadoMeta
import org.ucb.cashin.prediccion.PlanAhorro
import org.ucb.cashin.ui.components.AnilloAvance
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.Emocion
import org.ucb.cashin.ui.components.EstadoCargando
import org.ucb.cashin.ui.components.EstadoError
import org.ucb.cashin.ui.components.EstadoVacio
import org.ucb.cashin.ui.components.RecursoCashi
import org.ucb.cashin.ui.components.TarjetaJuego
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.GrisTexto
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.util.Formato
import java.math.BigDecimal

/**
 * Pestaña Metas.
 * HU-17: botón para crear una meta.
 * HU-19: cada meta activa con su barra, monto y porcentaje; abajo la sección plegable "Completadas".
 * Al tocar una meta se abre su detalle.
 */
@Composable
fun MetasScreen(
    onNueva: () -> Unit,
    onMetaClick: (metaId: String) -> Unit,
    /** false cuando va dentro de la pestaña "Metas | Desafíos", que ya tiene su propio título. */
    mostrarTitulo: Boolean = true,
    viewModel: MetasViewModel = viewModel(factory = MetasViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Se recarga cada vez que se entra (después de crear una meta o anotar un ahorro).
    LaunchedEffect(Unit) { viewModel.cargar() }

    MetasContenido(
        estado = estado,
        onReintentar = viewModel::cargar,
        onNueva = onNueva,
        onMetaClick = onMetaClick,
        onVerCompletadasCambio = viewModel::onVerCompletadasCambio,
        mostrarTitulo = mostrarTitulo,
    )
}

/** Solo dibuja. Estados: cargando, error, sin metas o la lista. */
@Composable
fun MetasContenido(
    estado: MetasUiState,
    onReintentar: () -> Unit,
    onNueva: () -> Unit,
    onMetaClick: (metaId: String) -> Unit,
    onVerCompletadasCambio: () -> Unit,
    mostrarTitulo: Boolean = true,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (mostrarTitulo) CashinTopBar(titulo = stringResource(R.string.nav_metas))

        when {
            estado.cargando -> EstadoCargando()

            estado.error != null -> EstadoError(mensaje = estado.error.mensaje(), onReintentar = onReintentar)

            estado.sinMetas -> EstadoVacio(
                titulo = stringResource(R.string.metas_vacio_titulo),
                mensaje = stringResource(R.string.metas_vacio_mensaje),
                emocion = Emocion.FELIZ,
                textoAccion = stringResource(R.string.metas_vacio_accion),
                onAccion = onNueva,
                recurso = RecursoCashi.METAS_VACIO,
                tamanoAvatar = 240.dp,
            )

            else -> Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = Espacio.pantalla, vertical = Espacio.s),
                        verticalArrangement = Arrangement.spacedBy(Espacio.s),
                    ) {
                        if (estado.activas.isEmpty()) {
                            item(key = "sin-activas") {
                                Text(
                                    text = stringResource(R.string.metas_sin_activas),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }

                        items(estado.activas, key = { it.metaId }) { meta ->
                            TarjetaMeta(meta = meta, plan = estado.planes[meta.metaId], onClick = { onMetaClick(meta.metaId) })
                        }

                        if (estado.completadas.isNotEmpty()) {
                            item(key = "titulo-completadas") {
                                EncabezadoCompletadas(
                                    cantidad = estado.completadas.size,
                                    abierto = estado.verCompletadas,
                                    onClick = onVerCompletadasCambio,
                                )
                            }
                            if (estado.verCompletadas) {
                                items(estado.completadas, key = { it.metaId }) { meta ->
                                    TarjetaMeta(meta = meta, plan = null, onClick = { onMetaClick(meta.metaId) })
                                }
                            }
                        }
                    }
                }
                CashinBoton(
                    texto = stringResource(R.string.metas_nueva),
                    onClick = onNueva,
                    modifier = Modifier.padding(horizontal = Espacio.pantalla, vertical = Espacio.m),
                )
            }
        }
    }
}

/** "Completadas (2) ▾": al tocarlo se abre o se cierra la sección. */
@Composable
private fun EncabezadoCompletadas(cantidad: Int, abierto: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(top = Espacio.m, bottom = Espacio.xs),
    ) {
        Text(
            text = stringResource(R.string.metas_completadas, cantidad),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (abierto) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = stringResource(if (abierto) R.string.metas_ocultar_completadas else R.string.metas_ver_completadas),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Anillo de avance, nombre, cuánto falta y qué hacer a continuación. */
@Composable
private fun TarjetaMeta(meta: AvanceMeta, plan: PlanAhorro?, onClick: () -> Unit) {
    TarjetaJuego(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnilloAvance(progreso = meta.progreso, porcentaje = meta.porcentaje)
            Column(modifier = Modifier.padding(start = Espacio.m).weight(1f)) {
                Text(
                    text = meta.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (meta.alcanzada) {
                        stringResource(R.string.metas_objetivo_llegado)
                    } else {
                        stringResource(R.string.metas_te_faltan, Formato.monto(meta.faltante))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = GrisTexto,
                )
                val paso = plan?.pasoActual
                when {
                    meta.estado == EstadoMeta.COMPLETADA -> TextoLogro(stringResource(R.string.metas_estado_completada))
                    meta.alcanzada -> TextoLogro(stringResource(R.string.metas_alcanzada))
                    paso != null -> Text(
                        text = stringResource(
                            R.string.metas_proximo_ahorro,
                            Formato.montoEntero(paso.monto),
                            Formato.fechaCorta(paso.fecha),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TextoLogro(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = CashinColores.ingreso,
        modifier = Modifier.padding(top = 2.dp),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun MetasPreview() = CashinTheme {
    MetasContenido(
        estado = MetasUiState(
            cargando = false,
            activas = listOf(
                AvanceMeta(
                    metaId = "1", usuarioId = "u", nombre = "Celular nuevo",
                    montoObjetivo = BigDecimal("2500.00"), fechaLimite = Formato.hoy().plusMonths(5),
                    estado = EstadoMeta.ACTIVA, totalAhorrado = BigDecimal("300.00"),
                ),
            ),
            completadas = listOf(
                AvanceMeta(
                    metaId = "2", usuarioId = "u", nombre = "Viaje a Samaipata",
                    montoObjetivo = BigDecimal("800.00"), estado = EstadoMeta.COMPLETADA,
                    totalAhorrado = BigDecimal("800.00"),
                ),
            ),
            verCompletadas = true,
        ),
        onReintentar = {},
        onNueva = {},
        onMetaClick = {},
        onVerCompletadasCambio = {},
    )
}