package org.ucb.cashin.ui.metas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.model.AporteMeta
import org.ucb.cashin.model.AvanceMeta
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.EstadoMeta
import org.ucb.cashin.model.TipoAporte
import org.ucb.cashin.prediccion.CadenciaPlan
import org.ucb.cashin.prediccion.EstadoPlan
import org.ucb.cashin.prediccion.PlanAhorro
import org.ucb.cashin.ui.components.AvatarBurbuja
import org.ucb.cashin.ui.components.BarraProgreso
import org.ucb.cashin.ui.components.CaminoMeta
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinCampoMonto
import org.ucb.cashin.ui.components.CashinCampoTexto
import org.ucb.cashin.ui.components.CashinTarjeta
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.ComoLoCalcule
import org.ucb.cashin.ui.components.Emocion
import org.ucb.cashin.ui.components.EstadoCargando
import org.ucb.cashin.ui.components.EstadoError
import org.ucb.cashin.ui.components.RecursoCashi
import org.ucb.cashin.ui.components.TarjetaJuego
import org.ucb.cashin.ui.components.VarianteBoton
import org.ucb.cashin.ui.components.focoCamino
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.util.Formato
import java.math.BigDecimal

/**
 * HU-18 / HU-19: detalle de una meta. Avance con barra, monto y porcentaje;
 * botones para anotar un ahorro o un retiro; y la lista de lo anotado.
 * HU-20: "¿Cuándo lo logro?" con el motor de predicción. Al completarla suma +100 XP.
 */
@Composable
fun DetalleMetaScreen(
    metaId: String,
    onAtras: () -> Unit,
    onEditar: () -> Unit,
    onCancelada: () -> Unit,
    viewModel: DetalleMetaViewModel = viewModel(factory = DetalleMetaViewModel.fabrica(metaId)),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Se recarga cada vez que se entra (por ejemplo al volver de editar la meta).
    LaunchedEffect(Unit) { viewModel.cargar() }

    LaunchedEffect(estado.cancelada) {
        if (estado.cancelada) onCancelada()
    }

    DetalleMetaContenido(
        estado = estado,
        onAtras = onAtras,
        onEditar = onEditar,
        onReintentar = viewModel::cargar,
        onAbrirDialogo = viewModel::abrirDialogo,
        onCerrarDialogo = viewModel::cerrarDialogo,
        onMontoAporteCambio = viewModel::onMontoAporteCambio,
        onNotaAporteCambio = viewModel::onNotaAporteCambio,
        onGuardarAporte = viewModel::guardarAporte,
        onCompletarMeta = viewModel::completarMeta,
        onCancelarMeta = viewModel::cancelarMeta,
        onCelebracionVista = viewModel::celebracionVista,
    )
}

/** Solo dibuja. Estados: cargando, error o la meta con sus movimientos. */
@Composable
fun DetalleMetaContenido(
    estado: DetalleMetaUiState,
    onAtras: () -> Unit,
    onEditar: () -> Unit,
    onReintentar: () -> Unit,
    onAbrirDialogo: (TipoAporte) -> Unit,
    onCerrarDialogo: () -> Unit,
    onMontoAporteCambio: (String) -> Unit,
    onNotaAporteCambio: (String) -> Unit,
    onGuardarAporte: () -> Unit,
    onCompletarMeta: () -> Unit,
    onCancelarMeta: () -> Unit,
    onCelebracionVista: () -> Unit,
) {
    var confirmarCancelar by remember { mutableStateOf(false) }
    var caminoVisto by rememberSaveable { mutableStateOf(false) }
    val avance = estado.avance
    val lista = rememberLazyListState()
    val densidad = LocalDensity.current

    // Al anotar un ahorro baja hasta el paso, para que se vea la moneda.
    LaunchedEffect(estado.celebracion) {
        val plan = estado.plan
        if (estado.celebracion != null && plan != null) {
            val foco = with(densidad) { focoCamino(plan, densidad.fontScale).roundToPx() }
            val alturaVisible = lista.layoutInfo.viewportSize.height
            val indiceCamino = if (avance?.alcanzada == true) 1 else 2
            lista.animateScrollToItem(indiceCamino, (foco - alturaVisible / 2).coerceAtLeast(0))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(
            titulo = avance?.nombre ?: stringResource(R.string.nav_metas),
            onAtras = onAtras,
            acciones = {
                if (avance != null) {
                    IconButton(onClick = onEditar, enabled = !estado.cambiandoEstado) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.metas_editar),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    IconButton(onClick = { confirmarCancelar = true }, enabled = !estado.cambiandoEstado) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.metas_cancelar),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
        )

        when {
            estado.cargando -> EstadoCargando()

            estado.error != null || avance == null -> EstadoError(
                mensaje = (estado.error ?: ErrorApp.DESCONOCIDO).mensaje(),
                onReintentar = onReintentar,
            )

            else -> {
                LazyColumn(
                    state = lista,
                    contentPadding = PaddingValues(horizontal = Espacio.pantalla, vertical = Espacio.s),
                    verticalArrangement = Arrangement.spacedBy(Espacio.m),
                    modifier = Modifier.weight(1f),
                ) {
                    item(key = "avance") { TarjetaAvance(avance = avance, completada = estado.completada) }

                    // HU-20: justo debajo del encabezado. No se muestra si ya llegó al objetivo.
                    val plan = estado.plan
                    if (!avance.alcanzada && plan != null) {
                        item(key = "ia") {
                            TarjetaCuandoLoLogro(
                                plan = plan,
                                tieneFechaLimite = avance.fechaLimite != null,
                                mesesDeDatos = estado.mesesDeDatos,
                                recorte = estado.recorte,
                            )
                        }
                    }

                    if (plan != null && plan.estado != EstadoPlan.SIN_DATOS) {
                        item(key = "camino") {
                            CaminoMeta(
                                plan = plan,
                                nombreMeta = avance.nombre,
                                animarEntrada = !caminoVisto,
                                onEntradaVista = { caminoVisto = true },
                                confetiAlEntrar = avance.alcanzada && !estado.completada && !caminoVisto,
                                celebracion = estado.celebracion,
                                onCelebracionVista = onCelebracionVista,
                                modifier = Modifier.anchoCompleto(Espacio.pantalla),
                            )
                        }
                    }

                    if (estado.completada) {
                        item(key = "celebracion") {
                            AvatarBurbuja(
                                mensaje = stringResource(
                                    if (estado.recienCompletada) R.string.metas_completada_xp else R.string.metas_completada_mensaje,
                                ),
                                emocion = Emocion.CELEBRANDO,
                                recurso = RecursoCashi.META_COMPLETADA,
                            )
                        }
                    }

                    item(key = "titulo-movimientos") {
                        Text(
                            text = stringResource(R.string.metas_movimientos),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = Espacio.s),
                        )
                    }

                    if (estado.aportes.isEmpty()) {
                        item(key = "sin-movimientos") {
                            Text(
                                text = stringResource(R.string.metas_sin_movimientos),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    } else {
                        items(estado.aportes, key = { it.id }) { aporte -> FilaAporte(aporte) }
                    }
                }

                // Una meta completada ya no recibe ahorros ni retiros.
                if (!estado.completada) {
                    BotonesMeta(
                        avance = avance,
                        estado = estado,
                        onAbrirDialogo = onAbrirDialogo,
                        onCompletarMeta = onCompletarMeta,
                    )
                }
            }
        }
    }

    if (estado.dialogoTipo != null && avance != null) {
        DialogoAporte(
            tipo = estado.dialogoTipo,
            totalAhorrado = avance.totalAhorrado,
            estado = estado,
            onMontoCambio = onMontoAporteCambio,
            onNotaCambio = onNotaAporteCambio,
            onGuardar = onGuardarAporte,
            onCerrar = onCerrarDialogo,
        )
    }

    if (confirmarCancelar && avance != null) {
        AlertDialog(
            onDismissRequest = { confirmarCancelar = false },
            title = { Text(stringResource(R.string.metas_cancelar_titulo)) },
            text = { Text(stringResource(R.string.metas_cancelar_mensaje, avance.nombre)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarCancelar = false
                        onCancelarMeta()
                    },
                ) {
                    Text(stringResource(R.string.metas_cancelar), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarCancelar = false }) {
                    Text(stringResource(R.string.accion_cancelar))
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }
}

/** Fijos abajo: uno debajo del otro y de borde a borde. Al llegar al objetivo, lo principal es completarla. */
@Composable
private fun BotonesMeta(
    avance: AvanceMeta,
    estado: DetalleMetaUiState,
    onAbrirDialogo: (TipoAporte) -> Unit,
    onCompletarMeta: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Espacio.s),
        modifier = Modifier.padding(horizontal = Espacio.pantalla, vertical = Espacio.s),
    ) {
        estado.errorEstado?.let { error -> MensajeError(error.mensaje()) }
        if (estado.avisoNoLlega) MensajeError(stringResource(R.string.metas_todavia_no_llega))

        if (avance.alcanzada) {
            CashinBoton(
                texto = stringResource(R.string.metas_marcar_completada),
                onClick = onCompletarMeta,
                cargando = estado.cambiandoEstado,
            )
        } else {
            CashinBoton(
                texto = stringResource(R.string.metas_anotar_ahorro),
                onClick = { onAbrirDialogo(TipoAporte.AHORRO) },
                icono = Icons.Outlined.Add,
            )
        }
        CashinBoton(
            texto = stringResource(R.string.metas_anotar_retiro),
            onClick = { onAbrirDialogo(TipoAporte.RETIRO) },
            variante = VarianteBoton.SECUNDARIO,
            icono = Icons.Outlined.Remove,
            grosorBorde = 2.5.dp,
            // Sin nada ahorrado no hay qué retirar.
            habilitado = avance.totalAhorrado.signum() > 0,
        )
    }
}

/** El camino va de borde a borde aunque la lista tenga margen a los lados. */
private fun Modifier.anchoCompleto(margen: Dp): Modifier = layout { medible, restricciones ->
    val lados = margen.roundToPx()
    val ancho = restricciones.maxWidth + lados * 2
    val colocable = medible.measure(restricciones.copy(minWidth = ancho, maxWidth = ancho))
    layout(restricciones.maxWidth, colocable.height) { colocable.place(-lados, 0) }
}

/**
 * HU-19: "Bs 350,00" grande, "de Bs 2.500,00", la barra y "14 % · Te faltan Bs 2.150,00".
 * Si ya está completada, lo dice arriba.
 */
@Composable
private fun TarjetaAvance(avance: AvanceMeta, completada: Boolean) {
    TarjetaJuego {
        if (completada) {
            Text(
                text = stringResource(R.string.metas_estado_completada),
                style = MaterialTheme.typography.labelLarge,
                color = CashinColores.ingreso,
                modifier = Modifier.padding(bottom = Espacio.xs),
            )
        }
        Text(
            text = stringResource(R.string.metas_ahorrado),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = Formato.monto(avance.totalAhorrado),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = stringResource(R.string.metas_de_objetivo, Formato.monto(avance.montoObjetivo)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = Espacio.m),
        )
        BarraProgreso(progreso = avance.progreso)
        Text(
            text = if (avance.alcanzada) {
                stringResource(R.string.metas_alcanzada)
            } else {
                stringResource(R.string.metas_porcentaje_falta, avance.porcentaje, Formato.monto(avance.faltante))
            },
            style = MaterialTheme.typography.labelLarge,
            color = if (avance.alcanzada) CashinColores.ingreso else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = Espacio.s),
        )
    }
}

/**
 * HU-20: "¿Cuándo lo logro?" en lenguaje simple: un solo monto y una sola fecha, los mismos del camino.
 * Debajo, dónde se podría recortar y, plegado, con cuántos datos se calculó.
 */
@Composable
private fun TarjetaCuandoLoLogro(
    plan: PlanAhorro,
    tieneFechaLimite: Boolean,
    mesesDeDatos: Int,
    recorte: RecorteSugerido?,
) {
    val color = MaterialTheme.colorScheme.onTertiaryContainer
    val monto = Formato.montoEntero(plan.ahorroPorPaso)
    val periodo = plan.periodo()
    val fecha = plan.fechaEstimada
    val (titulo, detalle) = when (plan.estado) {
        EstadoPlan.ALCANZA -> Pair(
            if (tieneFechaLimite || fecha == null) {
                stringResource(R.string.plan_llegas)
            } else {
                stringResource(R.string.ia_lo_lograrias, Formato.mesEnFrase(fecha))
            },
            stringResource(R.string.plan_ritmo, monto, periodo),
        )
        EstadoPlan.CON_RECORTE -> Pair(
            stringResource(if (tieneFechaLimite || fecha == null) R.string.plan_con_recorte else R.string.plan_con_recorte_sin_fecha),
            if (tieneFechaLimite || fecha == null) {
                stringResource(R.string.plan_recorte_fecha, monto, periodo)
            } else {
                stringResource(R.string.plan_recorte_sin_fecha, monto, periodo, Formato.mesEnFrase(fecha))
            },
        )
        EstadoPlan.NO_LLEGA_A_FECHA -> Pair(
            stringResource(R.string.plan_llegarias, fecha?.let(Formato::fechaEnFrase).orEmpty()),
            stringResource(R.string.plan_no_llega, monto, periodo),
        )
        EstadoPlan.SIN_MARGEN -> Pair(
            stringResource(R.string.plan_sin_margen_titulo),
            stringResource(R.string.plan_sin_margen, monto),
        )
        EstadoPlan.SIN_DATOS, EstadoPlan.LOGRADA -> Pair(
            stringResource(R.string.plan_sin_datos_titulo),
            stringResource(R.string.plan_sin_datos),
        )
    }

    CashinTarjeta(color = MaterialTheme.colorScheme.tertiaryContainer) {
        Text(
            text = stringResource(R.string.metas_cuando_titulo),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(top = Espacio.xs),
        )
        Text(
            text = detalle,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            modifier = Modifier.padding(top = Espacio.xs),
        )

        if (recorte != null && plan.estado != EstadoPlan.SIN_DATOS) {
            Text(
                text = stringResource(R.string.ia_que_recortar),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(top = Espacio.s),
            )
            Text(
                text = stringResource(R.string.ia_recorte, recorte.categoria, Formato.montoAproximado(recorte.monto)),
                style = MaterialTheme.typography.bodyMedium,
                color = color,
            )
        }

        if (mesesDeDatos > 0 && plan.estado != EstadoPlan.SIN_DATOS) {
            ComoLoCalcule(
                detalle = pluralStringResource(R.plurals.metas_como_calcule, mesesDeDatos, mesesDeDatos),
                color = color,
            )
        }
    }
}

/** "al mes", "cada quincena", "cada semana" o "cada 2 meses". */
@Composable
private fun PlanAhorro.periodo(): String = when {
    cadencia == CadenciaPlan.SEMANAL -> stringResource(R.string.periodo_semana)
    cadencia == CadenciaPlan.QUINCENAL -> stringResource(R.string.periodo_quincena)
    mesesPorPaso > 1 -> stringResource(R.string.periodo_meses, mesesPorPaso)
    else -> stringResource(R.string.periodo_mes)
}

/** "+ Bs 100,00" (ahorro, verde) o "- Bs 50,00" (retiro, rojo), con fecha y nota. */
@Composable
private fun FilaAporte(aporte: AporteMeta) {
    val esRetiro = aporte.tipo == TipoAporte.RETIRO
    CashinTarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (esRetiro) R.string.metas_retiro else R.string.metas_ahorro),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = aporte.nota?.let { stringResource(R.string.historial_hora_nota, Formato.encabezadoDia(aporte.fecha), it) }
                        ?: Formato.encabezadoDia(aporte.fecha),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = Formato.montoConSigno(aporte.monto, esGasto = esRetiro),
                style = MaterialTheme.typography.titleMedium,
                color = if (esRetiro) CashinColores.gasto else CashinColores.ingreso,
            )
        }
    }
}

/** HU-18: monto y nota opcional. En un retiro recuerda cuánto hay ahorrado. */
@Composable
private fun DialogoAporte(
    tipo: TipoAporte,
    totalAhorrado: BigDecimal,
    estado: DetalleMetaUiState,
    onMontoCambio: (String) -> Unit,
    onNotaCambio: (String) -> Unit,
    onGuardar: () -> Unit,
    onCerrar: () -> Unit,
) {
    val esRetiro = tipo == TipoAporte.RETIRO
    val textoErrorMonto = estado.errorMontoAporte?.let { id ->
        if (id == R.string.metas_error_retiro) stringResource(id, Formato.monto(totalAhorrado)) else stringResource(id)
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(stringResource(if (esRetiro) R.string.metas_anotar_retiro else R.string.metas_anotar_ahorro)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                if (esRetiro) {
                    Text(
                        text = stringResource(R.string.metas_disponible_retirar, Formato.monto(totalAhorrado)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                CashinCampoMonto(
                    valor = estado.montoAporte,
                    onValorCambio = onMontoCambio,
                    etiqueta = stringResource(R.string.anotar_monto),
                    error = textoErrorMonto,
                    colorMonto = if (esRetiro) CashinColores.gasto else CashinColores.ingreso,
                    habilitado = !estado.guardandoAporte,
                )
                CashinCampoTexto(
                    etiqueta = stringResource(R.string.anotar_nota),
                    valor = estado.notaAporte,
                    onValorCambio = onNotaCambio,
                    accionIme = ImeAction.Done,
                    habilitado = !estado.guardandoAporte,
                )
                estado.errorAporte?.let { MensajeError(it.mensaje()) }
            }
        },
        confirmButton = {
            TextButton(onClick = onGuardar, enabled = !estado.guardandoAporte) {
                Text(stringResource(R.string.anotar_guardar))
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar, enabled = !estado.guardandoAporte) {
                Text(stringResource(R.string.accion_cancelar))
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    )
}

@Composable
private fun MensajeError(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Espacio.xs),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA, heightDp = 900)
@Composable
private fun DetalleMetaPreview() = CashinTheme {
    DetalleMetaContenido(
        estado = DetalleMetaUiState(
            cargando = false,
            avance = AvanceMeta(
                metaId = "1", usuarioId = "u", nombre = "Celular nuevo",
                montoObjetivo = BigDecimal("2500.00"), fechaLimite = Formato.hoy().plusMonths(5),
                estado = EstadoMeta.ACTIVA, totalAhorrado = BigDecimal("350.00"),
            ),
            aportes = listOf(
                AporteMeta(id = "a1", metaId = "1", tipo = TipoAporte.AHORRO, monto = BigDecimal("400.00"), fecha = Formato.hoy(), nota = "Sueldo"),
                AporteMeta(id = "a2", metaId = "1", tipo = TipoAporte.RETIRO, monto = BigDecimal("50.00"), fecha = Formato.hoy().minusDays(3)),
            ),
        ),
        onAtras = {},
        onEditar = {},
        onReintentar = {},
        onAbrirDialogo = {},
        onCerrarDialogo = {},
        onMontoAporteCambio = {},
        onNotaAporteCambio = {},
        onGuardarAporte = {},
        onCompletarMeta = {},
        onCancelarMeta = {},
        onCelebracionVista = {},
    )
}