package org.ucb.cashin.ui.registros

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.model.Categoria
import org.ucb.cashin.model.MedioPago
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinCampoMonto
import org.ucb.cashin.ui.components.CashinCampoTexto
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.EstadoCargando
import org.ucb.cashin.ui.components.EstadoError
import org.ucb.cashin.ui.components.SelectorCategoria
import org.ucb.cashin.ui.components.VarianteBoton
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio

/**
 * HU-06 / HU-07: anotar un gasto o un ingreso.
 * HU-08: con un [registroId] abre la misma pantalla para corregir o borrar esa anotación.
 */
@Composable
fun AnotarScreen(
    onGuardado: (ganoXp: Boolean) -> Unit,
    registroId: String? = null,
    onEliminado: () -> Unit = {},
    onAtras: (() -> Unit)? = null,
    viewModel: AnotarViewModel = viewModel(factory = AnotarViewModel.fabrica(registroId)),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Cuando se guarda: limpia el formulario y avisa a quien abrió la pantalla.
    LaunchedEffect(estado.guardado) {
        if (estado.guardado) {
            val ganoXp = estado.ganoXp
            viewModel.limpiarFormulario()
            onGuardado(ganoXp)
        }
    }

    LaunchedEffect(estado.eliminado) {
        if (estado.eliminado) onEliminado()
    }

    AnotarContenido(
        estado = estado,
        onTipoCambio = viewModel::onTipoCambio,
        onMontoCambio = viewModel::onMontoCambio,
        onCategoriaElegida = viewModel::onCategoriaElegida,
        onMedioPagoCambio = viewModel::onMedioPagoCambio,
        onNotaCambio = viewModel::onNotaCambio,
        onGuardar = viewModel::guardar,
        onEliminar = viewModel::eliminar,
        onReintentarCategorias = viewModel::cargarCategorias,
        onReintentarRegistro = viewModel::cargarRegistro,
        onAtras = onAtras,
    )
}

/** Solo dibuja el formulario. Se puede previsualizar sin Supabase. */
@Composable
fun AnotarContenido(
    estado: AnotarUiState,
    onTipoCambio: (TipoRegistro) -> Unit,
    onMontoCambio: (String) -> Unit,
    onCategoriaElegida: (Categoria) -> Unit,
    onMedioPagoCambio: (MedioPago) -> Unit,
    onNotaCambio: (String) -> Unit,
    onGuardar: () -> Unit,
    onEliminar: () -> Unit,
    onReintentarCategorias: () -> Unit,
    onReintentarRegistro: () -> Unit,
    onAtras: (() -> Unit)?,
) {
    val foco = LocalFocusManager.current
    var confirmarEliminar by rememberSaveable { mutableStateOf(false) }
    val ocupado = estado.cargando || estado.eliminando

    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(
            titulo = stringResource(if (estado.esEdicion) R.string.anotar_editar_titulo else R.string.nav_anotar),
            onAtras = onAtras,
        )

        when {
            estado.cargandoRegistro -> EstadoCargando()

            estado.errorRegistro != null -> EstadoError(
                mensaje = estado.errorRegistro.mensaje(),
                onReintentar = onReintentarRegistro,
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = Espacio.pantalla, vertical = Espacio.s),
                verticalArrangement = Arrangement.spacedBy(Espacio.l),
            ) {
                SelectorTipo(tipo = estado.tipo, onTipoCambio = onTipoCambio, habilitado = !ocupado)

                CashinCampoMonto(
                    valor = estado.monto,
                    onValorCambio = onMontoCambio,
                    etiqueta = stringResource(R.string.anotar_monto),
                    error = estado.errorMonto?.let { stringResource(it) },
                    colorMonto = colorDelTipo(estado.tipo),
                    habilitado = !ocupado,
                )

                Column {
                    EtiquetaSeccion(texto = stringResource(R.string.anotar_categoria))
                    SeccionCategorias(
                        estado = estado,
                        onCategoriaElegida = onCategoriaElegida,
                        onReintentar = onReintentarCategorias,
                        habilitado = !ocupado,
                    )
                    estado.errorCategoria?.let { MensajeError(texto = stringResource(it)) }
                }

                Column {
                    EtiquetaSeccion(texto = stringResource(R.string.anotar_medio_pago))
                    SelectorMedioPago(
                        medioPago = estado.medioPago,
                        onMedioPagoCambio = onMedioPagoCambio,
                        habilitado = !ocupado,
                    )
                }

                CashinCampoTexto(
                    etiqueta = stringResource(R.string.anotar_nota),
                    valor = estado.nota,
                    onValorCambio = onNotaCambio,
                    placeholder = stringResource(R.string.anotar_nota_ejemplo),
                    accionIme = ImeAction.Done,
                    onAccionIme = { foco.clearFocus() },
                    habilitado = !ocupado,
                )

                estado.error?.let { MensajeError(texto = it.mensaje()) }

                Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    CashinBoton(
                        texto = stringResource(if (estado.esEdicion) R.string.anotar_guardar_cambios else R.string.anotar_guardar),
                        onClick = onGuardar,
                        cargando = estado.cargando,
                        habilitado = !estado.eliminando,
                    )
                    if (estado.esEdicion) {
                        CashinBoton(
                            texto = stringResource(R.string.anotar_eliminar),
                            onClick = { confirmarEliminar = true },
                            variante = VarianteBoton.PELIGRO,
                            icono = Icons.Outlined.Delete,
                            cargando = estado.eliminando,
                            habilitado = !estado.cargando,
                        )
                    }
                }
            }
        }
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text(stringResource(R.string.anotar_eliminar_titulo)) },
            text = { Text(stringResource(R.string.anotar_eliminar_mensaje)) },
            confirmButton = {
                TextButton(onClick = { confirmarEliminar = false; onEliminar() }) {
                    Text(stringResource(R.string.anotar_eliminar), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminar = false }) { Text(stringResource(R.string.accion_cancelar)) }
            },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }
}

/** Rojo para gastos y verde para ingresos, igual que en el historial. */
@Composable
private fun colorDelTipo(tipo: TipoRegistro): Color =
    if (tipo == TipoRegistro.GASTO) CashinColores.gasto else CashinColores.ingreso

/**
 * "Gasto | Ingreso" como una píldora con dos mitades. La mitad elegida se pinta
 * del color de su tipo (rojo o verde) con el texto en blanco.
 */
@Composable
private fun SelectorTipo(tipo: TipoRegistro, onTipoCambio: (TipoRegistro) -> Unit, habilitado: Boolean) {
    val pildora = RoundedCornerShape(50)
    Surface(
        shape = pildora,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp)
                .selectableGroup(),
        ) {
            TipoRegistro.entries.forEach { opcion ->
                val elegido = opcion == tipo
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(pildora)
                        .background(if (elegido) colorDelTipo(opcion) else Color.Transparent)
                        .selectable(
                            selected = elegido,
                            enabled = habilitado,
                            role = Role.RadioButton,
                            onClick = { onTipoCambio(opcion) },
                        ),
                ) {
                    Text(
                        text = stringResource(opcion.etiqueta()),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (elegido) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Grilla de categorías, o lo que corresponda mientras carga, si falla o si no hay ninguna. */
@Composable
private fun SeccionCategorias(
    estado: AnotarUiState,
    onCategoriaElegida: (Categoria) -> Unit,
    onReintentar: () -> Unit,
    habilitado: Boolean,
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
            MensajeError(texto = estado.errorCategorias.mensaje())
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
            habilitado = habilitado,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Efectivo, QR, Tarjeta, Transferencia u Otro (HU-06). Siempre hay uno marcado:
 * empieza en Efectivo y al tocar otro se cambia, nunca queda vacío.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorMedioPago(
    medioPago: MedioPago,
    onMedioPagoCambio: (MedioPago) -> Unit,
    habilitado: Boolean,
) {
    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Espacio.s),
        verticalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        MedioPago.entries.forEach { opcion ->
            val elegido = opcion == medioPago
            FilterChip(
                selected = elegido,
                onClick = { onMedioPagoCambio(opcion) },
                enabled = habilitado,
                label = { Text(stringResource(opcion.etiqueta())) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = habilitado,
                    selected = elegido,
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

/** Título pequeño sobre cada sección, con el mismo estilo que las etiquetas de los campos de texto. */
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

private val categoriasDeEjemplo = listOf(
    Categoria(id = "1", nombre = "Alimentación", tipo = TipoRegistro.GASTO, icono = "restaurant", color = "#4CAF50"),
    Categoria(id = "2", nombre = "Transporte", tipo = TipoRegistro.GASTO, icono = "bus", color = "#2E7D32"),
    Categoria(id = "3", nombre = "Vivienda", tipo = TipoRegistro.GASTO, icono = "home", color = "#81C784"),
    Categoria(id = "4", nombre = "Salud", tipo = TipoRegistro.GASTO, icono = "health", color = "#FF8C32"),
    Categoria(id = "5", nombre = "Educación", tipo = TipoRegistro.GASTO, icono = "school", color = "#1F6E8C"),
)

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA, heightDp = 900)
@Composable
private fun AnotarConErroresPreview() = CashinTheme {
    AnotarContenido(
        estado = AnotarUiState(
            categorias = categoriasDeEjemplo,
            cargandoCategorias = false,
            errorMonto = R.string.validacion_monto,
            errorCategoria = R.string.validacion_categoria,
        ),
        onTipoCambio = {}, onMontoCambio = {}, onCategoriaElegida = {}, onMedioPagoCambio = {},
        onNotaCambio = {}, onGuardar = {}, onEliminar = {}, onReintentarCategorias = {},
        onReintentarRegistro = {}, onAtras = null,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA, heightDp = 900)
@Composable
private fun EditarPreview() = CashinTheme {
    AnotarContenido(
        estado = AnotarUiState(
            esEdicion = true,
            monto = "20",
            categorias = categoriasDeEjemplo,
            categoriaId = "1",
            medioPago = MedioPago.QR,
            nota = "Almuerzo",
            cargandoCategorias = false,
        ),
        onTipoCambio = {}, onMontoCambio = {}, onCategoriaElegida = {}, onMedioPagoCambio = {},
        onNotaCambio = {}, onGuardar = {}, onEliminar = {}, onReintentarCategorias = {},
        onReintentarRegistro = {}, onAtras = {},
    )
}