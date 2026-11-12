package org.ucb.cashin.ui.deudas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.model.EstadoDeuda
import org.ucb.cashin.model.ResumenDeuda
import org.ucb.cashin.model.TipoDeuda
import org.ucb.cashin.prediccion.MovimientoDeuda
import org.ucb.cashin.prediccion.PasoDeuda
import org.ucb.cashin.prediccion.ProblemaMovimiento
import org.ucb.cashin.prediccion.TipoMovimiento
import org.ucb.cashin.prediccion.lineaDeTiempo
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinCampoMonto
import org.ucb.cashin.ui.components.CashinCampoTexto
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.Confeti
import org.ucb.cashin.ui.components.EstadoCargando
import org.ucb.cashin.ui.components.EstadoError
import org.ucb.cashin.ui.components.ImagenCashi
import org.ucb.cashin.ui.components.RecursoCashi
import org.ucb.cashin.ui.components.VarianteBoton
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.util.Formato
import java.math.BigDecimal
import java.time.LocalDate

/**
 * HU-26: detalle de una deuda. Arriba lo que falta; debajo la línea de tiempo con todos los
 * movimientos (lo más nuevo arriba). Desde aquí se abona, se presta más y se borra el último movimiento.
 * [abrirPrestarMas] y [montoInicial] llegan desde "Sumar a la de Amy" en el formulario.
 */
@Composable
fun DetalleDeudaScreen(
    deudaId: String,
    onAtras: () -> Unit,
    onEditar: () -> Unit,
    onEliminada: () -> Unit,
    abrirPrestarMas: Boolean = false,
    montoInicial: String? = null,
    viewModel: DetalleDeudaViewModel = viewModel(factory = DetalleDeudaViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var yaAbrio by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(deudaId) { viewModel.cargar(deudaId) }
    LaunchedEffect(estado.deuda != null) {
        if (abrirPrestarMas && !yaAbrio && estado.deuda != null) {
            yaAbrio = true
            viewModel.abrirHoja(HojaDeuda.AUMENTO, montoInicial)
        }
    }
    LaunchedEffect(estado.eliminada) { if (estado.eliminada) onEliminada() }

    Box(modifier = Modifier.fillMaxSize()) {
        DetalleDeudaContenido(
            estado = estado,
            onAtras = onAtras,
            onReintentar = { viewModel.cargar(deudaId) },
            onEditar = onEditar,
            onEliminar = viewModel::pedirEliminar,
            onAbrirHoja = { viewModel.abrirHoja(it) },
            onBorrar = viewModel::pedirBorrar,
        )
        if (estado.celebrar && estado.deuda != null) {
            CelebracionDeuda(estado.deuda!!, cantidadAbonos(estado.linea), onListo = viewModel::cerrarCelebracion)
        }
    }

    if (estado.hoja != HojaDeuda.NINGUNA && estado.deuda != null) {
        HojaMovimiento(
            estado = estado,
            onCerrar = viewModel::cerrarHoja,
            onMonto = viewModel::onMonto,
            onFecha = viewModel::onFecha,
            onNota = viewModel::onNota,
            onTodo = viewModel::onTodo,
            onCambiarLimite = viewModel::onCambiarLimite,
            onNuevaFechaLimite = viewModel::onNuevaFechaLimite,
            onGuardar = viewModel::guardar,
        )
    }

    estado.confirmarBorrar?.let { movimiento ->
        DialogoBorrar(estado, movimiento, onConfirmar = viewModel::confirmarBorrar, onCancelar = viewModel::cancelarBorrar)
    }

    if (estado.confirmarEliminar) {
        AlertDialog(
            onDismissRequest = viewModel::cancelarEliminar,
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text(stringResource(R.string.deuda_eliminar_titulo, estado.deuda?.contraparte.orEmpty())) },
            text = { Text(stringResource(R.string.deuda_eliminar_mensaje)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmarEliminar) {
                    Text(stringResource(R.string.deuda_eliminar_si), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelarEliminar) { Text(stringResource(R.string.accion_cancelar)) } },
        )
    }

    // Error al borrar o eliminar (sin hoja abierta): un aviso que se cierra con Aceptar.
    if (estado.hoja == HojaDeuda.NINGUNA && estado.errorGuardar != null) {
        AlertDialog(
            onDismissRequest = viewModel::limpiarError,
            containerColor = MaterialTheme.colorScheme.background,
            text = { Text(stringResource(estado.errorGuardar!!)) },
            confirmButton = { TextButton(onClick = viewModel::limpiarError) { Text(stringResource(R.string.accion_aceptar)) } },
        )
    }

}

/** Solo dibuja. Estados: cargando, error y datos. */
@Composable
fun DetalleDeudaContenido(
    estado: DetalleDeudaUiState,
    onAtras: () -> Unit,
    onReintentar: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onAbrirHoja: (HojaDeuda) -> Unit,
    onBorrar: (MovimientoDeuda) -> Unit,
) {
    val deuda = estado.deuda
    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(
            titulo = deuda?.contraparte.orEmpty(),
            onAtras = onAtras,
            acciones = { if (deuda != null) MenuDeuda(onEditar, onEliminar) },
        )
        when {
            estado.cargando -> EstadoCargando()
            estado.error != null || deuda == null -> EstadoError(
                mensaje = estado.error?.mensaje() ?: stringResource(R.string.error_desconocido),
                onReintentar = onReintentar,
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Espacio.pantalla, vertical = Espacio.s),
                    verticalArrangement = Arrangement.spacedBy(Espacio.s),
                ) {
                    item(key = "cabecera") { CabeceraDeuda(deuda, estado.hoy) }
                    estado.contraria?.let { otra ->
                        item(key = "contraria") { AvisoContraria(deuda.tipo, otra) }
                    }
                    item(key = "titulo") {
                        Text(
                            text = stringResource(R.string.deuda_linea_tiempo),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = Espacio.s),
                        )
                    }
                    val pasos = estado.linea.reversed()
                    itemsIndexed(pasos, key = { i, p -> p.movimiento.id ?: "inicio$i" }) { i, paso ->
                        FilaMovimiento(
                            paso = paso,
                            tipoDeuda = deuda.tipo,
                            ultimo = i == pasos.lastIndex,
                            borrable = paso.movimiento == estado.borrable,
                            onBorrar = { onBorrar(paso.movimiento) },
                        )
                    }
                }
                BotonesDeuda(estado.pagada, deuda.tipo, onAbrirHoja)
            }
        }
    }
}

@Composable
private fun MenuDeuda(onEditar: () -> Unit, onEliminar: () -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.deuda_menu), tint = MaterialTheme.colorScheme.onBackground)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }, containerColor = MaterialTheme.colorScheme.background) {
            DropdownMenuItem(text = { Text(stringResource(R.string.deuda_editar)) }, onClick = { abierto = false; onEditar() })
            DropdownMenuItem(
                text = { Text(stringResource(R.string.deuda_eliminar), color = MaterialTheme.colorScheme.error) },
                onClick = { abierto = false; onEliminar() },
            )
        }
    }
}

/** "Te debe · desde el 1 nov." · "Te falta cobrar Bs 70" · barra · "Te devolvió Bs 30 de Bs 100" · aviso. */
@Composable
private fun CabeceraDeuda(deuda: ResumenDeuda, hoy: LocalDate) {
    val color = deuda.tipo.color()
    val pagada = deuda.estado == EstadoDeuda.PAGADA
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        Text(
            text = stringResource(
                deuda.tipo.texto(R.string.deuda_detalle_tipo_debo, R.string.deuda_detalle_tipo_me_deben),
                deuda.fechaInicio?.let { Formato.fechaCorta(it) }.orEmpty(),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (pagada) stringResource(R.string.deuda_pagada)
            else stringResource(deuda.tipo.texto(R.string.deuda_falta_debo, R.string.deuda_falta_me_deben), Formato.monto(deuda.pendiente)),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (!deuda.descripcion.isNullOrBlank()) {
            Text(deuda.descripcion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BarraDeuda(deuda.avance(), color, Modifier.padding(vertical = Espacio.xs), alto = 14)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (pagada) stringResource(
                    deuda.tipo.texto(R.string.deuda_pagada_sello_debo, R.string.deuda_pagada_sello_me_deben),
                    Formato.montoRedondo(deuda.montoTotal),
                ) else stringResource(
                    deuda.tipo.texto(R.string.deuda_avance_debo, R.string.deuda_avance_me_deben),
                    Formato.montoRedondo(deuda.abonado), Formato.montoRedondo(deuda.montoTotal),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.reportes_porcentaje, (deuda.avance() * 100).toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ChipAviso(deuda, hoy)
    }
}

/** "También le debes Bs 20 a Amy." */
@Composable
private fun AvisoContraria(tipo: TipoDeuda, otra: ResumenDeuda) {
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(tipo.texto(R.string.deuda_contraria_debo, R.string.deuda_contraria_me_deben), otra.contraparte, Formato.montoRedondo(otra.pendiente)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = Espacio.m, vertical = 10.dp),
        )
    }
}

/**
 * Un movimiento: círculo con + (se sumó) o − (se pagó), qué fue, la fecha y la nota, el monto y
 * cuánto quedó. Una línea une cada círculo con el de abajo. Solo el último lleva el botón de borrar.
 */
@Composable
private fun FilaMovimiento(paso: PasoDeuda, tipoDeuda: TipoDeuda, ultimo: Boolean, borrable: Boolean, onBorrar: () -> Unit) {
    val mov = paso.movimiento
    val esAbono = mov.tipo == TipoMovimiento.ABONO
    val color = if (esAbono) CashinColores.ingreso else CashinColores.gasto
    val signo = if (esAbono) "−" else "+"
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(30.dp).fillMaxHeight()) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(2.dp, color), modifier = Modifier.size(30.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(signo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                }
            }
            if (!ultimo) {
                Box(Modifier.width(2.dp).weight(1f).background(MaterialTheme.colorScheme.outline))
            }
        }
        Column(modifier = Modifier.weight(1f).padding(bottom = Espacio.m)) {
            Text(
                text = stringResource(textoMovimiento(mov.tipo, tipoDeuda)),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = listOfNotNull(Formato.fechaCorta(mov.fecha), mov.nota?.takeIf { it.isNotBlank() }).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = Espacio.m)) {
            Text(
                text = "$signo ${Formato.monto(mov.monto)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                text = stringResource(R.string.deuda_queda, Formato.montoRedondo(paso.saldo)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (borrable) {
            IconButton(onClick = onBorrar) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.deuda_borrar_ultimo), tint = CashinColores.gasto)
            }
        }
    }
}

private fun textoMovimiento(tipo: TipoMovimiento, deuda: TipoDeuda): Int = when (tipo) {
    TipoMovimiento.INICIO -> deuda.texto(R.string.deuda_mov_inicio_debo, R.string.deuda_mov_inicio_me_deben)
    TipoMovimiento.AUMENTO -> deuda.texto(R.string.deuda_mov_aumento_debo, R.string.deuda_mov_aumento_me_deben)
    TipoMovimiento.ABONO -> deuda.texto(R.string.deuda_mov_abono_debo, R.string.deuda_mov_abono_me_deben)
}

/** Pendiente: "Prestar más" + "Anotar devolución". Pagada: "Prestarle de nuevo". */
@Composable
private fun BotonesDeuda(pagada: Boolean, tipo: TipoDeuda, onAbrirHoja: (HojaDeuda) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Espacio.pantalla, vertical = Espacio.m),
        horizontalArrangement = Arrangement.spacedBy(Espacio.s),
    ) {
        if (pagada) {
            CashinBoton(
                texto = stringResource(tipo.texto(R.string.deuda_boton_de_nuevo_debo, R.string.deuda_boton_de_nuevo_me_deben)),
                onClick = { onAbrirHoja(HojaDeuda.AUMENTO) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            CashinBoton(
                texto = stringResource(tipo.texto(R.string.deuda_boton_aumento_debo, R.string.deuda_boton_aumento_me_deben)),
                onClick = { onAbrirHoja(HojaDeuda.AUMENTO) },
                variante = VarianteBoton.SECUNDARIO,
                modifier = Modifier.weight(1f),
            )
            CashinBoton(
                texto = stringResource(tipo.texto(R.string.deuda_boton_abono_debo, R.string.deuda_boton_abono_me_deben)),
                onClick = { onAbrirHoja(HojaDeuda.ABONO) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Hoja de abajo para anotar un abono o sumar un préstamo. Revisa todo antes de dejar guardar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaMovimiento(
    estado: DetalleDeudaUiState,
    onCerrar: () -> Unit,
    onMonto: (String) -> Unit,
    onFecha: (LocalDate) -> Unit,
    onNota: (String) -> Unit,
    onTodo: () -> Unit,
    onCambiarLimite: (Boolean) -> Unit,
    onNuevaFechaLimite: (LocalDate?) -> Unit,
    onGuardar: () -> Unit,
) {
    val deuda = estado.deuda ?: return
    val tipo = deuda.tipo
    val esAbono = estado.hoja == HojaDeuda.ABONO
    val inicio = deuda.fechaInicio ?: estado.hoy
    var calendario by remember { mutableStateOf(false) }
    var calendarioLimite by remember { mutableStateOf(false) }
    val validacion = estado.validacion

    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.pantalla)
                .padding(bottom = Espacio.l)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Espacio.m),
        ) {
            Column {
                Text(
                    text = stringResource(
                        if (esAbono) tipo.texto(R.string.deuda_hoja_abono_debo, R.string.deuda_hoja_abono_me_deben)
                        else tipo.texto(R.string.deuda_hoja_aumento_debo, R.string.deuda_hoja_aumento_me_deben),
                        deuda.contraparte,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = if (esAbono) stringResource(
                        tipo.texto(R.string.deuda_hoja_abono_sub_debo, R.string.deuda_hoja_abono_sub_me_deben),
                        Formato.monto(deuda.pendiente),
                    ) else stringResource(R.string.deuda_hoja_aumento_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            CashinCampoMonto(
                valor = estado.montoTexto,
                onValorCambio = onMonto,
                etiqueta = stringResource(if (esAbono) R.string.deuda_campo_monto else R.string.deuda_campo_cuanto_mas),
                error = errorMonto(validacion, tipo, deuda.contraparte),
            )
            if (esAbono) {
                FilterChip(
                    selected = estado.montoIngresado?.compareTo(deuda.pendiente) == 0,
                    onClick = onTodo,
                    label = { Text(stringResource(R.string.deuda_todo, Formato.monto(deuda.pendiente))) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }

            CampoFechaDeuda(
                etiqueta = stringResource(R.string.deuda_campo_fecha),
                fecha = estado.fecha,
                textoVacio = "",
                onClick = { calendario = true },
                error = errorFecha(validacion, tipo, inicio),
            )

            CashinCampoTexto(
                etiqueta = stringResource(R.string.deuda_campo_nota),
                valor = estado.nota,
                onValorCambio = onNota,
                placeholder = stringResource(R.string.deuda_campo_nota_ej),
                capitalizar = true,
            )

            if (!esAbono) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.deuda_poner_fecha_limite), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                        Text(
                            text = deuda.fechaLimite?.let { stringResource(R.string.deuda_fecha_limite_actual, Formato.fechaCorta(it)) }
                                ?: stringResource(R.string.deuda_sin_fecha_limite_hoy),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = estado.cambiarLimite,
                        onCheckedChange = onCambiarLimite,
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                    )
                }
                if (estado.cambiarLimite) {
                    CampoFechaDeuda(
                        etiqueta = stringResource(R.string.deuda_form_hasta),
                        fecha = estado.nuevaFechaLimite,
                        textoVacio = stringResource(R.string.deuda_form_sin_fecha),
                        onClick = { calendarioLimite = true },
                        onQuitar = { onNuevaFechaLimite(null) },
                        descripcionQuitar = stringResource(R.string.deuda_form_quitar_fecha),
                    )
                }
                val monto = estado.montoIngresado
                if (monto != null && validacion?.valido == true) {
                    val nuevo = Formato.montoRedondo(deuda.pendiente + monto)
                    Surface(color = CashinColores.ingreso.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(
                                tipo.texto(R.string.deuda_prev_debo, R.string.deuda_prev_me_deben),
                                nuevo, deuda.contraparte, Formato.montoRedondo(deuda.pendiente),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = Espacio.m, vertical = 10.dp),
                        )
                    }
                }
            }

            estado.errorGuardar?.let {
                Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }

            CashinBoton(
                texto = stringResource(
                    if (esAbono) tipo.texto(R.string.deuda_guardar_abono_debo, R.string.deuda_guardar_abono_me_deben)
                    else R.string.deuda_guardar_aumento,
                ),
                onClick = onGuardar,
                cargando = estado.guardando,
                habilitado = validacion?.valido == true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (calendario) {
        CalendarioDeuda(
            fecha = estado.fecha,
            desde = inicio,
            hasta = estado.hoy,
            onConfirmar = { calendario = false; onFecha(it) },
            onCancelar = { calendario = false },
        )
    }
    if (calendarioLimite) {
        CalendarioDeuda(
            fecha = estado.nuevaFechaLimite,
            desde = maxOf(inicio, estado.hoy),
            hasta = null,
            onConfirmar = { calendarioLimite = false; onNuevaFechaLimite(it) },
            onCancelar = { calendarioLimite = false },
        )
    }
}

@Composable
private fun errorMonto(v: org.ucb.cashin.prediccion.ValidacionMovimiento?, tipo: TipoDeuda, persona: String): String? = when (v?.problema) {
    ProblemaMovimiento.MONTO_INVALIDO -> stringResource(R.string.deuda_error_monto)
    ProblemaMovimiento.SUPERA_PENDIENTE ->
        stringResource(tipo.texto(R.string.deuda_error_supera_debo, R.string.deuda_error_supera_me_deben), Formato.monto(v.limite ?: BigDecimal.ZERO), persona)
    else -> null
}

@Composable
private fun errorFecha(v: org.ucb.cashin.prediccion.ValidacionMovimiento?, tipo: TipoDeuda, inicio: LocalDate): String? = when (v?.problema) {
    ProblemaMovimiento.FECHA_ANTES_DEL_INICIO -> stringResource(R.string.deuda_error_fecha_inicio, Formato.fechaCorta(inicio))
    ProblemaMovimiento.FECHA_FUTURA -> stringResource(R.string.deuda_error_fecha_futura)
    ProblemaMovimiento.NO_SE_DEBIA_TANTO ->
        stringResource(tipo.texto(R.string.deuda_error_no_se_debia_debo, R.string.deuda_error_no_se_debia_me_deben), Formato.monto(v.limite ?: BigDecimal.ZERO))
    else -> null
}

/** "¿Borrar «Te devolvió Bs 30,00»?" · "Te va a deber Bs 100." */
@Composable
private fun DialogoBorrar(estado: DetalleDeudaUiState, movimiento: MovimientoDeuda, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    val deuda = estado.deuda ?: return
    val sin = estado.linea.filter { it.movimiento != movimiento }.map { it.movimiento }
    val quedaria = lineaDeTiempo(sin).lastOrNull()?.saldo ?: BigDecimal.ZERO
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Text(stringResource(R.string.deuda_borrar_titulo, stringResource(textoMovimiento(movimiento.tipo, deuda.tipo)), Formato.monto(movimiento.monto)))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                Text(stringResource(deuda.tipo.texto(R.string.deuda_borrar_mensaje_debo, R.string.deuda_borrar_mensaje_me_deben), Formato.monto(quedaria)))
                Text(stringResource(R.string.deuda_borrar_nota), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onConfirmar) { Text(stringResource(R.string.deuda_borrar_si), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.accion_cancelar)) } },
    )
}

private fun cantidadAbonos(linea: List<PasoDeuda>): Int = linea.count { it.movimiento.tipo == TipoMovimiento.ABONO }

/** Pantalla completa al terminar de pagar: Cashi celebrando, confeti y "Pagada el …". */
@Composable
private fun CelebracionDeuda(deuda: ResumenDeuda, abonos: Int, onListo: () -> Unit) {
    var confeti by remember { mutableStateOf(true) }
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espacio.s),
            modifier = Modifier.padding(Espacio.pantalla).widthIn(max = 360.dp),
        ) {
            ImagenCashi(RecursoCashi.CELEBRANDO, contentDescription = stringResource(R.string.avatar_descripcion), modifier = Modifier.size(200.dp))
            Text(
                text = stringResource(deuda.tipo.texto(R.string.deuda_celebracion_debo, R.string.deuda_celebracion_me_deben), deuda.contraparte),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = pluralStringResource(
                    deuda.tipo.texto(R.plurals.deuda_celebracion_detalle_debo, R.plurals.deuda_celebracion_detalle_me_deben),
                    abonos, Formato.montoRedondo(deuda.montoTotal), abonos,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Surface(color = CashinColores.ingreso.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = CashinColores.ingreso, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.deudas_aviso_pagada, deuda.fechaCancelacion?.let { Formato.fechaCorta(it) }.orEmpty()),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = CashinColores.ingreso,
                    )
                }
            }
            CashinBoton(texto = stringResource(R.string.deuda_listo), onClick = onListo, modifier = Modifier.fillMaxWidth().padding(top = Espacio.s))
        }
        if (confeti) Confeti(foco = 300.dp, onFin = { confeti = false })
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA, heightDp = 800)
@Composable
private fun DetalleDeudaPreview() = CashinTheme {
    val hoy = LocalDate.of(2026, 11, 11)
    val inicio = LocalDate.of(2026, 11, 1)
    val movs = org.ucb.cashin.prediccion.movimientosDe(
        BigDecimal("40"), inicio, "Almuerzo",
        listOf(org.ucb.cashin.model.AumentoDeuda("au", "d", BigDecimal("60"), LocalDate.of(2026, 11, 5), "Para su pasaje")),
        listOf(org.ucb.cashin.model.AbonoDeuda("ab", "d", BigDecimal("30"), LocalDate.of(2026, 11, 8), "En efectivo")),
    )
    DetalleDeudaContenido(
        estado = DetalleDeudaUiState(
            deuda = ResumenDeuda(
                deudaId = "d", usuarioId = "u", tipo = TipoDeuda.ME_DEBEN, contraparte = "Amy", montoTotal = BigDecimal("100"),
                fechaLimite = null, estado = EstadoDeuda.PENDIENTE, pendiente = BigDecimal("70"), abonado = BigDecimal("30"),
                fechaInicio = inicio, montoInicial = BigDecimal("40"), aumentado = BigDecimal("60"), cantidadAumentos = 1, cantidadAbonos = 1,
            ),
            linea = lineaDeTiempo(movs),
            hoy = hoy,
            cargando = false,
        ),
        onAtras = {}, onReintentar = {}, onEditar = {}, onEliminar = {}, onAbrirHoja = {}, onBorrar = {},
    )
}
