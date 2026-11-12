package org.ucb.cashin.ui.deudas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.model.EstadoDeuda
import org.ucb.cashin.model.ResumenDeuda
import org.ucb.cashin.model.TipoDeuda
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.EstadoCargando
import org.ucb.cashin.ui.components.EstadoError
import org.ucb.cashin.ui.components.EstadoVacio
import org.ucb.cashin.ui.components.RecursoCashi
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.util.Formato
import java.math.BigDecimal
import java.time.LocalDate

/** HU-25: Perfil → Deudas. Pestañas "Debo | Me deben", cada una con su total, sus deudas y las pagadas. */
@Composable
fun DeudasScreen(
    onAtras: () -> Unit,
    onDeudaClick: (deudaId: String) -> Unit,
    onNueva: (tipo: TipoDeuda) -> Unit,
    viewModel: DeudasViewModel = viewModel(factory = DeudasViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Se recarga cada vez que se vuelve a la lista (después de crear, abonar o borrar).
    LaunchedEffect(Unit) { viewModel.cargar() }

    DeudasContenido(
        estado = estado,
        onAtras = onAtras,
        onReintentar = viewModel::cargar,
        onTipoCambio = viewModel::onTipoCambio,
        onPagadasCambio = viewModel::onPagadasCambio,
        onDeudaClick = onDeudaClick,
        onNueva = { onNueva(estado.tipo) },
    )
}

/** Solo dibuja. Estados: cargando, error, pestaña vacía y datos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeudasContenido(
    estado: DeudasUiState,
    onAtras: () -> Unit,
    onReintentar: () -> Unit,
    onTipoCambio: (TipoDeuda) -> Unit,
    onPagadasCambio: () -> Unit,
    onDeudaClick: (deudaId: String) -> Unit,
    onNueva: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(titulo = stringResource(R.string.perfil_deudas), onAtras = onAtras)

        when {
            estado.cargando -> EstadoCargando()
            estado.error != null -> EstadoError(mensaje = estado.error.mensaje(), onReintentar = onReintentar)
            else -> {
                PrimaryTabRow(
                    selectedTabIndex = estado.tipo.ordinal,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.secondary,
                ) {
                    TipoDeuda.entries.forEach { tipo ->
                        val total = Formato.montoRedondo(if (tipo == TipoDeuda.DEBO) estado.totalDebo else estado.totalMeDeben)
                        Tab(
                            selected = estado.tipo == tipo,
                            onClick = { onTipoCambio(tipo) },
                            text = {
                                Text(
                                    stringResource(tipo.texto(R.string.deudas_pestana_debo, R.string.deudas_pestana_me_deben), total),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            },
                            selectedContentColor = MaterialTheme.colorScheme.secondary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (estado.pendientes.isEmpty() && estado.pagadas.isEmpty()) {
                        EstadoVacio(
                            titulo = stringResource(estado.tipo.texto(R.string.deudas_vacio_debo_titulo, R.string.deudas_vacio_me_deben_titulo)),
                            mensaje = stringResource(estado.tipo.texto(R.string.deudas_vacio_debo_mensaje, R.string.deudas_vacio_me_deben_mensaje)),
                            recurso = RecursoCashi.PENSANDO,
                        )
                    } else {
                        ListaDeudas(estado, onPagadasCambio, onDeudaClick)
                    }
                }

                CashinBoton(
                    texto = stringResource(R.string.deudas_nueva),
                    onClick = onNueva,
                    icono = Icons.Filled.Add,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Espacio.pantalla, vertical = Espacio.m),
                )
            }
        }
    }
}

@Composable
private fun ListaDeudas(
    estado: DeudasUiState,
    onPagadasCambio: () -> Unit,
    onDeudaClick: (deudaId: String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = Espacio.pantalla, vertical = Espacio.m),
        verticalArrangement = Arrangement.spacedBy(Espacio.s),
    ) {
        if (estado.pendientes.isNotEmpty()) {
            item(key = "resumen") {
                ResumenPestana(
                    tipo = estado.tipo,
                    total = if (estado.tipo == TipoDeuda.DEBO) estado.totalDebo else estado.totalMeDeben,
                    cantidad = estado.pendientes.size,
                )
            }
        }
        items(estado.pendientes, key = { it.deudaId }) { deuda ->
            TarjetaDeuda(deuda, estado.hoy, onClick = { onDeudaClick(deuda.deudaId) })
        }
        if (estado.pagadas.isNotEmpty()) {
            item(key = "pagadas") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(onClick = onPagadasCambio)
                        .padding(horizontal = Espacio.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.deudas_pagadas, estado.pagadas.size),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = if (estado.mostrarPagadas) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            items(estado.pagadas, key = { "p" + it.deudaId }) { deuda ->
                AnimatedVisibility(visible = estado.mostrarPagadas) {
                    TarjetaDeuda(deuda, estado.hoy, onClick = { onDeudaClick(deuda.deudaId) })
                }
            }
        }
    }
}

/** "Te falta pagar · Bs 1.120 · en 3 deudas" en una franja verde oscura. */
@Composable
private fun ResumenPestana(tipo: TipoDeuda, total: BigDecimal, cantidad: Int) {
    Surface(
        color = MaterialTheme.colorScheme.secondary,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Espacio.m, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacio.s),
        ) {
            Text(
                text = stringResource(tipo.texto(R.string.deudas_resumen_debo, R.string.deudas_resumen_me_deben)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = Formato.montoRedondo(total),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondary,
            )
            Text(
                text = pluralStringResource(R.plurals.deudas_cantidad, cantidad, cantidad),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondary,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA, heightDp = 800)
@Composable
private fun DeudasPreview() = CashinTheme {
    val hoy = LocalDate.of(2026, 11, 11)
    fun d(id: String, nombre: String, desc: String, total: String, pendiente: String, limite: LocalDate?) = ResumenDeuda(
        deudaId = id, usuarioId = "u", tipo = TipoDeuda.DEBO, contraparte = nombre, montoTotal = BigDecimal(total),
        fechaLimite = limite, estado = EstadoDeuda.PENDIENTE, pendiente = BigDecimal(pendiente),
        abonado = BigDecimal(total) - BigDecimal(pendiente), descripcion = desc, fechaInicio = hoy.minusDays(20),
    )
    DeudasContenido(
        estado = DeudasUiState(
            deudas = listOf(
                d("1", "Pasanaku del trabajo", "Cuota de noviembre", "400", "200", hoy.plusDays(3)),
                d("2", "Juan", "Almuerzo y pasajes", "50", "20", hoy.minusDays(6)),
                d("3", "Tío Carlos", "Préstamo para la laptop", "1500", "900", hoy.plusDays(49)),
            ),
            hoy = hoy,
            cargando = false,
        ),
        onAtras = {}, onReintentar = {}, onTipoCambio = {}, onPagadasCambio = {}, onDeudaClick = {}, onNueva = {},
    )
}
