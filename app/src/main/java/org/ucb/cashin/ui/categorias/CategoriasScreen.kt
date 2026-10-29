package org.ucb.cashin.ui.categorias

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.model.Categoria
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinCampoTexto
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.EstadoCargando
import org.ucb.cashin.ui.components.EstadoError
import org.ucb.cashin.ui.components.EstadoVacio
import org.ucb.cashin.ui.components.colorCategoria
import org.ucb.cashin.ui.components.iconoCategoria
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.ColoresCategoria
import org.ucb.cashin.ui.theme.Espacio

/** HU-11 / HU-12: lista de categorías con pestañas Gasto / Ingreso, para crear y archivar las propias. */
@Composable
fun CategoriasScreen(
    onAtras: () -> Unit,
    viewModel: CategoriasViewModel = viewModel(factory = CategoriasViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    CategoriasContenido(
        estado = estado,
        onAtras = onAtras,
        onReintentar = viewModel::cargar,
        onTipoCambio = viewModel::onTipoCambio,
        onNueva = viewModel::abrirDialogo,
        onArchivar = viewModel::archivar,
        onNombreCambio = viewModel::onNombreCambio,
        onIconoCambio = viewModel::onIconoCambio,
        onColorCambio = viewModel::onColorCambio,
        onCrear = viewModel::crear,
        onCerrarDialogo = viewModel::cerrarDialogo,
    )
}

/** Solo dibuja. Estados: cargando, error, sin categorías o la lista. */
@Composable
fun CategoriasContenido(
    estado: CategoriasUiState,
    onAtras: () -> Unit,
    onReintentar: () -> Unit,
    onTipoCambio: (TipoRegistro) -> Unit,
    onNueva: () -> Unit,
    onArchivar: (Categoria, Boolean) -> Unit,
    onNombreCambio: (String) -> Unit,
    onIconoCambio: (String) -> Unit,
    onColorCambio: (String) -> Unit,
    onCrear: () -> Unit,
    onCerrarDialogo: () -> Unit,
) {
    var porArchivar by remember { mutableStateOf<Categoria?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(titulo = stringResource(R.string.perfil_mis_categorias), onAtras = onAtras)

        TabRow(
            selectedTabIndex = TipoRegistro.entries.indexOf(estado.tipo),
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            TipoRegistro.entries.forEach { tipo ->
                Tab(
                    selected = tipo == estado.tipo,
                    onClick = { onTipoCambio(tipo) },
                    text = {
                        Text(
                            stringResource(
                                if (tipo == TipoRegistro.GASTO) R.string.categorias_tab_gasto
                                else R.string.categorias_tab_ingreso,
                            ),
                        )
                    },
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                estado.cargando -> EstadoCargando()

                estado.error != null -> EstadoError(mensaje = estado.error.mensaje(), onReintentar = onReintentar)

                estado.categorias.isEmpty() -> EstadoVacio(
                    titulo = stringResource(R.string.categorias_vacio_titulo),
                    mensaje = stringResource(R.string.categorias_vacio_mensaje),
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
                        items(estado.categorias, key = { it.id }) { categoria ->
                            FilaCategoria(
                                categoria = categoria,
                                onArchivar = { porArchivar = categoria },
                                onDesarchivar = { onArchivar(categoria, false) },
                            )
                        }
                    }
                }
            }
        }

        CashinBoton(
            texto = stringResource(R.string.categorias_nueva),
            onClick = onNueva,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = Espacio.pantalla, vertical = Espacio.m),
        )
    }

    porArchivar?.let { categoria ->
        AlertDialog(
            onDismissRequest = { porArchivar = null },
            title = { Text(stringResource(R.string.categorias_archivar_titulo)) },
            text = { Text(stringResource(R.string.categorias_archivar_mensaje, categoria.nombre)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        porArchivar = null
                        onArchivar(categoria, true)
                    },
                ) {
                    Text(stringResource(R.string.categorias_archivar), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { porArchivar = null }) {
                    Text(stringResource(R.string.accion_cancelar))
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }

    if (estado.dialogoAbierto) {
        DialogoNuevaCategoria(
            estado = estado,
            onNombreCambio = onNombreCambio,
            onIconoCambio = onIconoCambio,
            onColorCambio = onColorCambio,
            onCrear = onCrear,
            onCerrar = onCerrarDialogo,
        )
    }
}

/**
 * Una categoría: ícono, nombre y etiqueta ("Del sistema" o "Archivada").
 * Las del sistema no tienen botón: no se pueden archivar (RLS solo deja cambiar las propias).
 */
@Composable
private fun FilaCategoria(
    categoria: Categoria,
    onArchivar: () -> Unit,
    onDesarchivar: () -> Unit,
) {
    val color = colorCategoria(categoria.color, MaterialTheme.colorScheme.primary)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (categoria.archivada) 0.55f else 1f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacio.m),
            modifier = Modifier.padding(horizontal = Espacio.m, vertical = Espacio.s),
        ) {
            Icon(iconoCategoria(categoria.icono), contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoria.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                val etiqueta = when {
                    categoria.archivada -> R.string.categorias_archivada
                    categoria.esDelSistema -> R.string.categorias_del_sistema
                    else -> null
                }
                if (etiqueta != null) {
                    Text(
                        text = stringResource(etiqueta),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            when {
                categoria.esDelSistema -> Unit
                categoria.archivada -> TextButton(onClick = onDesarchivar) {
                    Text(stringResource(R.string.categorias_desarchivar))
                }
                else -> TextButton(onClick = onArchivar) {
                    Text(stringResource(R.string.categorias_archivar), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/**
 * Formulario para crear una categoría: nombre, ícono y color.
 * El contenido se desplaza para que en celulares pequeños no se corten los colores.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoNuevaCategoria(
    estado: CategoriasUiState,
    onNombreCambio: (String) -> Unit,
    onIconoCambio: (String) -> Unit,
    onColorCambio: (String) -> Unit,
    onCrear: () -> Unit,
    onCerrar: () -> Unit,
) {
    val colorElegido = colorCategoria(estado.color, MaterialTheme.colorScheme.primary)

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(stringResource(R.string.categorias_nueva)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Espacio.m),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                CashinCampoTexto(
                    etiqueta = stringResource(R.string.categorias_nombre),
                    valor = estado.nombre,
                    onValorCambio = onNombreCambio,
                    error = estado.errorNombre?.let { stringResource(it) },
                    accionIme = ImeAction.Done,
                    habilitado = !estado.guardando,
                    capitalizar = true,
                )

                Text(
                    text = stringResource(R.string.categorias_icono),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Espacio.s),
                    verticalArrangement = Arrangement.spacedBy(Espacio.s),
                ) {
                    ICONOS.forEach { nombre ->
                        val elegido = nombre == estado.icono
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (elegido) colorElegido.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(
                                if (elegido) 2.dp else 1.dp,
                                if (elegido) colorElegido else MaterialTheme.colorScheme.outlineVariant,
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(MaterialTheme.shapes.small)
                                .selectable(selected = elegido, role = Role.RadioButton, onClick = { onIconoCambio(nombre) }),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    iconoCategoria(nombre),
                                    contentDescription = null,
                                    tint = if (elegido) colorElegido else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.categorias_color),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Espacio.s),
                    verticalArrangement = Arrangement.spacedBy(Espacio.s),
                ) {
                    ColoresCategoria.forEach { hex ->
                        val elegido = hex == estado.color
                        Surface(
                            shape = CircleShape,
                            color = colorCategoria(hex, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .selectable(selected = elegido, role = Role.RadioButton, onClick = { onColorCambio(hex) }),
                        ) {
                            if (elegido) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                    }
                }

                if (estado.errorGuardar != null) {
                    Text(
                        text = estado.errorGuardar.mensaje(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCrear, enabled = !estado.guardando) {
                Text(stringResource(R.string.categorias_crear))
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar, enabled = !estado.guardando) {
                Text(stringResource(R.string.accion_cancelar))
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun CategoriasPreview() = CashinTheme {
    CategoriasContenido(
        estado = CategoriasUiState(
            cargando = false,
            categorias = listOf(
                Categoria(id = "1", nombre = "Alimentación", tipo = TipoRegistro.GASTO, icono = "restaurant", color = "#4CAF50"),
                Categoria(id = "2", usuarioId = "u", nombre = "Café", tipo = TipoRegistro.GASTO, icono = "store", color = "#E08A2C"),
                Categoria(id = "3", usuarioId = "u", nombre = "Gimnasio", tipo = TipoRegistro.GASTO, icono = "health", color = "#C0473A", archivada = true),
            ),
        ),
        onAtras = {},
        onReintentar = {},
        onTipoCambio = {},
        onNueva = {},
        onArchivar = { _, _ -> },
        onNombreCambio = {},
        onIconoCambio = {},
        onColorCambio = {},
        onCrear = {},
        onCerrarDialogo = {},
    )
}