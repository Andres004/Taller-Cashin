package org.ucb.cashin.ui.registros

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
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
import org.ucb.cashin.ui.components.SelectorCategoria
import org.ucb.cashin.ui.components.VarianteBoton
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio

/**
 * HU-06 / HU-07: anotar un gasto o un ingreso.
 * Al guardar avisa a quien la abrió, indicando si la anotación ganó XP.
 */
@Composable
fun AnotarScreen(
    onGuardado: (ganoXp: Boolean) -> Unit,
    viewModel: AnotarViewModel = viewModel(factory = AnotarViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Cuando se guarda: limpia el formulario y avisa para volver a Inicio.
    LaunchedEffect(estado.guardado) {
        if (estado.guardado) {
            val ganoXp = estado.ganoXp
            viewModel.limpiarFormulario()
            onGuardado(ganoXp)
        }
    }

    AnotarContenido(
        estado = estado,
        onTipoCambio = viewModel::onTipoCambio,
        onMontoCambio = viewModel::onMontoCambio,
        onCategoriaElegida = viewModel::onCategoriaElegida,
        onMedioPagoCambio = viewModel::onMedioPagoCambio,
        onNotaCambio = viewModel::onNotaCambio,
        onGuardar = viewModel::guardar,
        onReintentarCategorias = viewModel::cargarCategorias,
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
    onReintentarCategorias: () -> Unit,
) {
    val foco = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(titulo = stringResource(R.string.nav_anotar))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = Espacio.pantalla, vertical = Espacio.s),
            verticalArrangement = Arrangement.spacedBy(Espacio.l),
        ) {
            SelectorTipo(tipo = estado.tipo, onTipoCambio = onTipoCambio)

            CashinCampoMonto(
                valor = estado.monto,
                onValorCambio = onMontoCambio,
                error = estado.errorMonto?.let { stringResource(it) },
                colorMonto = if (estado.tipo == TipoRegistro.GASTO) CashinColores.gasto else CashinColores.ingreso,
            )

            Column {
                EtiquetaSeccion(texto = stringResource(R.string.anotar_categoria))
                SeccionCategorias(
                    estado = estado,
                    onCategoriaElegida = onCategoriaElegida,
                    onReintentar = onReintentarCategorias,
                )
                estado.errorCategoria?.let { MensajeError(texto = stringResource(it)) }
            }

            Column {
                EtiquetaSeccion(texto = stringResource(R.string.anotar_medio_pago))
                SelectorMedioPago(medioPago = estado.medioPago, onMedioPagoCambio = onMedioPagoCambio)
            }

            CashinCampoTexto(
                etiqueta = stringResource(R.string.anotar_nota),
                valor = estado.nota,
                onValorCambio = onNotaCambio,
                placeholder = stringResource(R.string.anotar_nota_ejemplo),
                accionIme = ImeAction.Done,
                onAccionIme = { foco.clearFocus() },
                habilitado = !estado.cargando,
            )

            estado.error?.let { MensajeError(texto = it.mensaje()) }

            CashinBoton(
                texto = stringResource(R.string.anotar_guardar),
                onClick = onGuardar,
                cargando = estado.cargando,
            )
        }
    }
}

/** Botones "Gasto | Ingreso". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorTipo(tipo: TipoRegistro, onTipoCambio: (TipoRegistro) -> Unit) {
    val tipos = TipoRegistro.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        tipos.forEachIndexed { indice, opcion ->
            SegmentedButton(
                selected = opcion == tipo,
                onClick = { onTipoCambio(opcion) },
                shape = SegmentedButtonDefaults.itemShape(index = indice, count = tipos.size),
                label = { Text(stringResource(opcion.etiqueta())) },
            )
        }
    }
}

/** Grilla de categorías, o lo que corresponda mientras carga, si falla o si no hay ninguna. */
@Composable
private fun SeccionCategorias(
    estado: AnotarUiState,
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
        )
    }
}

/** Efectivo, QR, Tarjeta, Transferencia u Otro. Efectivo viene elegido por defecto. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorMedioPago(medioPago: MedioPago, onMedioPagoCambio: (MedioPago) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Espacio.s),
        verticalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        MedioPago.entries.forEach { opcion ->
            FilterChip(
                selected = opcion == medioPago,
                onClick = { onMedioPagoCambio(opcion) },
                label = { Text(stringResource(opcion.etiqueta())) },
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

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun AnotarPreview() = CashinTheme {
    AnotarContenido(
        estado = AnotarUiState(
            monto = "20,50",
            categorias = listOf(
                Categoria(id = "1", nombre = "Alimentación", tipo = TipoRegistro.GASTO, icono = "restaurant", color = "#4CAF50"),
                Categoria(id = "2", nombre = "Transporte", tipo = TipoRegistro.GASTO, icono = "bus", color = "#2E7D32"),
                Categoria(id = "3", nombre = "Salud", tipo = TipoRegistro.GASTO, icono = "health", color = "#FF8C32"),
            ),
            categoriaId = "1",
            cargandoCategorias = false,
        ),
        onTipoCambio = {},
        onMontoCambio = {},
        onCategoriaElegida = {},
        onMedioPagoCambio = {},
        onNotaCambio = {},
        onGuardar = {},
        onReintentarCategorias = {},
    )
}