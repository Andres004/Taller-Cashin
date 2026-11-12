package org.ucb.cashin.ui.deudas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.model.TipoDeuda
import org.ucb.cashin.prediccion.ProblemaEdicion
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinCampoMonto
import org.ucb.cashin.ui.components.CashinCampoTexto
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.EstadoCargando
import org.ucb.cashin.ui.components.EstadoError
import org.ucb.cashin.ui.components.VarianteBoton
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.ui.theme.NaranjaRelieve
import org.ucb.cashin.util.Formato
import java.math.BigDecimal

/**
 * HU-25: nueva deuda o editar una. Si al escribir el nombre ya hay una deuda abierta con esa persona,
 * se ofrece "Sumar a la de Amy" ([onSumar] lleva al detalle con "Prestar más" abierto) o "Crear otra".
 */
@Composable
fun FormularioDeudaScreen(
    deudaId: String?,
    tipoInicial: TipoDeuda,
    onAtras: () -> Unit,
    onGuardada: (deudaId: String) -> Unit,
    onSumar: (deudaId: String, monto: String) -> Unit,
    viewModel: FormularioDeudaViewModel = viewModel(factory = FormularioDeudaViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(deudaId) { viewModel.iniciar(deudaId, tipoInicial) }
    LaunchedEffect(estado.guardadaId) { estado.guardadaId?.let(onGuardada) }

    FormularioDeudaContenido(
        estado = estado,
        onAtras = onAtras,
        onReintentar = onAtras,
        onTipo = viewModel::onTipo,
        onContraparte = viewModel::onContraparte,
        onMonto = viewModel::onMonto,
        onDescripcion = viewModel::onDescripcion,
        onFechaInicio = viewModel::onFechaInicio,
        onFechaLimite = viewModel::onFechaLimite,
        onSumar = { id -> onSumar(id, estado.montoTexto) },
        onCrearOtra = viewModel::onCrearOtra,
        onGuardar = viewModel::guardar,
    )
}

@Composable
fun FormularioDeudaContenido(
    estado: FormularioDeudaUiState,
    onAtras: () -> Unit,
    onReintentar: () -> Unit,
    onTipo: (TipoDeuda) -> Unit,
    onContraparte: (String) -> Unit,
    onMonto: (String) -> Unit,
    onDescripcion: (String) -> Unit,
    onFechaInicio: (java.time.LocalDate) -> Unit,
    onFechaLimite: (java.time.LocalDate?) -> Unit,
    onSumar: (deudaId: String) -> Unit,
    onCrearOtra: () -> Unit,
    onGuardar: () -> Unit,
) {
    var calendarioInicio by remember { mutableStateOf(false) }
    var calendarioLimite by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(
            titulo = stringResource(if (estado.editando) R.string.deuda_form_titulo_editar else R.string.deuda_form_titulo_nueva),
            onAtras = onAtras,
        )
        when {
            estado.cargando -> EstadoCargando()
            estado.error != null -> EstadoError(mensaje = estado.error.mensaje(), onReintentar = onReintentar)
            else -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Espacio.pantalla, vertical = Espacio.s),
                    verticalArrangement = Arrangement.spacedBy(Espacio.m),
                ) {
                    SelectorTipo(estado.tipo, habilitado = !estado.tieneMovimientos, onTipo = onTipo)
                    if (estado.tieneMovimientos) {
                        Text(
                            stringResource(R.string.deuda_form_tipo_bloqueado),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    CashinCampoTexto(
                        etiqueta = stringResource(estado.tipo.texto(R.string.deuda_form_quien_debo, R.string.deuda_form_quien_me_deben)),
                        valor = estado.contraparte,
                        onValorCambio = onContraparte,
                        error = if (estado.intento && estado.errorNombre) stringResource(R.string.deuda_form_error_nombre) else null,
                        capitalizar = true,
                    )

                    estado.sugerencia?.let { otra ->
                        AvisoMismaPersona(estado.tipo, otra, onSumar = { onSumar(otra.deudaId) }, onCrearOtra = onCrearOtra)
                    }

                    CashinCampoMonto(
                        valor = estado.montoTexto,
                        onValorCambio = onMonto,
                        etiqueta = stringResource(if (estado.editando) R.string.deuda_form_monto_editar else R.string.deuda_form_monto),
                        error = errorMonto(estado),
                    )

                    CashinCampoTexto(
                        etiqueta = stringResource(R.string.deuda_form_descripcion),
                        valor = estado.descripcion,
                        onValorCambio = onDescripcion,
                        capitalizar = true,
                    )

                    CampoFechaDeuda(
                        etiqueta = stringResource(R.string.deuda_form_desde),
                        fecha = estado.fechaInicio,
                        textoVacio = "",
                        onClick = { calendarioInicio = true },
                        error = errorInicio(estado),
                    )
                    CampoFechaDeuda(
                        etiqueta = stringResource(R.string.deuda_form_hasta),
                        fecha = estado.fechaLimite,
                        textoVacio = stringResource(R.string.deuda_form_sin_fecha),
                        onClick = { calendarioLimite = true },
                        onQuitar = { onFechaLimite(null) },
                        descripcionQuitar = stringResource(R.string.deuda_form_quitar_fecha),
                        error = if (estado.intento && estado.errorLimite) stringResource(R.string.deuda_form_error_limite) else null,
                    )

                    estado.errorGuardar?.let {
                        Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
                CashinBoton(
                    texto = stringResource(R.string.deuda_form_guardar),
                    onClick = onGuardar,
                    cargando = estado.guardando,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Espacio.pantalla, vertical = Espacio.m),
                )
            }
        }
    }

    if (calendarioInicio) {
        CalendarioDeuda(
            fecha = estado.fechaInicio,
            desde = null,
            hasta = estado.hoy,
            onConfirmar = { calendarioInicio = false; onFechaInicio(it) },
            onCancelar = { calendarioInicio = false },
        )
    }
    if (calendarioLimite) {
        CalendarioDeuda(
            fecha = estado.fechaLimite,
            desde = estado.fechaInicio,
            hasta = null,
            onConfirmar = { calendarioLimite = false; onFechaLimite(it) },
            onCancelar = { calendarioLimite = false },
        )
    }
}

@Composable
private fun errorMonto(estado: FormularioDeudaUiState): String? = when {
    estado.intento && estado.errorMonto -> stringResource(R.string.deuda_error_monto)
    estado.validacionEdicion?.problema == ProblemaEdicion.MENOR_QUE_LO_DEVUELTO ->
        stringResource(R.string.deuda_form_error_menor, Formato.monto(estado.validacionEdicion?.limite ?: BigDecimal.ZERO))
    else -> null
}

@Composable
private fun errorInicio(estado: FormularioDeudaUiState): String? = when {
    estado.errorInicioFuturo -> stringResource(R.string.deuda_form_error_inicio_futuro)
    estado.validacionEdicion?.problema == ProblemaEdicion.MOVIMIENTO_ANTES_DEL_INICIO -> stringResource(R.string.deuda_form_error_mov_antes)
    else -> null
}

/** "Debo | Me deben" como control segmentado. */
@Composable
private fun SelectorTipo(tipo: TipoDeuda, habilitado: Boolean, onTipo: (TipoDeuda) -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(3.dp)) {
            TipoDeuda.entries.forEach { opcion ->
                val elegido = opcion == tipo
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (elegido) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .selectable(selected = elegido, enabled = habilitado, role = Role.RadioButton, onClick = { onTipo(opcion) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(opcion.texto(R.string.deuda_form_debo, R.string.deuda_form_me_deben)),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (elegido) FontWeight.Bold else FontWeight.Medium,
                        color = if (elegido) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** "Ya tienes una deuda con Amy · Te debe Bs 70 desde el 1 nov. · [Sumar a la de Amy] [Crear otra]". */
@Composable
private fun AvisoMismaPersona(
    tipo: TipoDeuda,
    otra: org.ucb.cashin.model.ResumenDeuda,
    onSumar: () -> Unit,
    onCrearOtra: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Espacio.m), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
            Text(
                stringResource(R.string.deuda_ya_existe_titulo, otra.contraparte),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = NaranjaRelieve,
            )
            Text(
                stringResource(
                    tipo.texto(R.string.deuda_ya_existe_debo, R.string.deuda_ya_existe_me_deben),
                    Formato.monto(otra.pendiente),
                    otra.fechaInicio?.let { Formato.fechaCorta(it) }.orEmpty(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                CashinBoton(
                    texto = stringResource(R.string.deuda_ya_existe_sumar, otra.contraparte),
                    onClick = onSumar,
                    modifier = Modifier.weight(1f),
                )
                CashinBoton(
                    texto = stringResource(R.string.deuda_ya_existe_otra),
                    onClick = onCrearOtra,
                    variante = VarianteBoton.SECUNDARIO,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA, heightDp = 800)
@Composable
private fun FormularioDeudaPreview() = CashinTheme {
    FormularioDeudaContenido(
        estado = FormularioDeudaUiState(tipo = TipoDeuda.ME_DEBEN, contraparte = "Amy", montoTexto = "50"),
        onAtras = {}, onReintentar = {}, onTipo = {}, onContraparte = {}, onMonto = {}, onDescripcion = {},
        onFechaInicio = {}, onFechaLimite = {}, onSumar = {}, onCrearOtra = {}, onGuardar = {},
    )
}
