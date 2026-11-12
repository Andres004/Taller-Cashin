package org.ucb.cashin.ui.perfil

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.ucb.cashin.R
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.notificaciones.RecordatorioLocal
import org.ucb.cashin.prediccion.AjusteRecordatorio
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.ImagenCashi
import org.ucb.cashin.ui.components.RecursoCashi
import org.ucb.cashin.ui.components.VarianteBoton
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio

data class RecordatorioUiState(
    val ajuste: AjusteRecordatorio = AjusteRecordatorio(),
    /** El teléfono no deja notificar: se explica y se ofrece abrir los ajustes. */
    val sinPermiso: Boolean = false,
    val guardado: Boolean = false,
)

/** Recordatorio diario (Sprint 5): se guarda en el teléfono y programa la alarma al guardar. */
class RecordatorioViewModel(private val local: RecordatorioLocal) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RecordatorioUiState(ajuste = local.ajuste(), sinPermiso = local.ajuste().activo && !local.puedeNotificar()),
    )
    val uiState: StateFlow<RecordatorioUiState> = _uiState.asStateFlow()

    fun onActivo(activo: Boolean) = _uiState.update { it.copy(ajuste = it.ajuste.copy(activo = activo)) }

    fun onHora(hora: Int, minuto: Int) = _uiState.update { it.copy(ajuste = it.ajuste.copy(hora = hora, minuto = minuto)) }

    fun onSoloLaborables(solo: Boolean) = _uiState.update { it.copy(ajuste = it.ajuste.copy(soloLaborables = solo)) }

    /** Respuesta del permiso de notificaciones (o al volver de los ajustes del teléfono). */
    fun revisarPermiso() = _uiState.update { it.copy(sinPermiso = it.ajuste.activo && !local.puedeNotificar()) }

    fun guardar() {
        local.guardar(_uiState.value.ajuste)
        _uiState.update { it.copy(guardado = true) }
    }

    companion object {
        val Factory = fabricaDe { RecordatorioViewModel(it.recordatorio) }
    }
}

/** "Recordarme anotar": encender, elegir la hora y los días. Pide permiso de notificaciones si hace falta. */
@Composable
fun RecordatorioScreen(
    onAtras: () -> Unit,
    onGuardado: () -> Unit,
    viewModel: RecordatorioViewModel = viewModel(factory = RecordatorioViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.revisarPermiso() }

    LaunchedEffect(estado.guardado) { if (estado.guardado) onGuardado() }

    RecordatorioContenido(
        estado = estado,
        onAtras = onAtras,
        onActivo = { activo ->
            viewModel.onActivo(activo)
            if (activo && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.revisarPermiso()
            }
        },
        onHora = viewModel::onHora,
        onSoloLaborables = viewModel::onSoloLaborables,
        onAbrirAjustes = {
            contexto.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        },
        onGuardar = viewModel::guardar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordatorioContenido(
    estado: RecordatorioUiState,
    onAtras: () -> Unit,
    onActivo: (Boolean) -> Unit,
    onHora: (Int, Int) -> Unit,
    onSoloLaborables: (Boolean) -> Unit,
    onAbrirAjustes: () -> Unit,
    onGuardar: () -> Unit,
) {
    var eligiendoHora by remember { mutableStateOf(false) }
    val ajuste = estado.ajuste

    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(titulo = stringResource(R.string.recordatorio_titulo), onAtras = onAtras)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.pantalla, vertical = Espacio.s),
            verticalArrangement = Arrangement.spacedBy(Espacio.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ImagenCashi(RecursoCashi.RACHA, contentDescription = stringResource(R.string.avatar_descripcion), modifier = Modifier.size(150.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = Espacio.m, vertical = 12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.recordatorio_activar), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text(stringResource(R.string.recordatorio_activar_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = ajuste.activo,
                        onCheckedChange = onActivo,
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                    )
                }
            }

            if (estado.sinPermiso) {
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = Espacio.m, vertical = 10.dp)) {
                        Text(stringResource(R.string.recordatorio_sin_permiso), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                        CashinBoton(texto = stringResource(R.string.recordatorio_abrir_ajustes), onClick = onAbrirAjustes, variante = VarianteBoton.TEXTO)
                    }
                }
            }

            if (ajuste.activo) {
                Text(
                    stringResource(R.string.recordatorio_hora),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                )
                Surface(
                    onClick = { eligiendoHora = true },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "%02d:%02d".format(ajuste.hora, ajuste.minuto),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = Espacio.s),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s), modifier = Modifier.fillMaxWidth()) {
                    listOf(false to R.string.recordatorio_todos, true to R.string.recordatorio_laborables).forEach { (solo, texto) ->
                        FilterChip(
                            selected = ajuste.soloLaborables == solo,
                            onClick = { onSoloLaborables(solo) },
                            label = { Text(stringResource(texto)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        }
        CashinBoton(
            texto = stringResource(R.string.recordatorio_guardar),
            onClick = onGuardar,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Espacio.pantalla, vertical = Espacio.m),
        )
    }

    if (eligiendoHora) {
        val reloj = rememberTimePickerState(initialHour = ajuste.hora, initialMinute = ajuste.minuto, is24Hour = true)
        AlertDialog(
            onDismissRequest = { eligiendoHora = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text(stringResource(R.string.recordatorio_cambiar_hora)) },
            text = {
                TimePicker(
                    state = reloj,
                    colors = TimePickerDefaults.colors(
                        selectorColor = MaterialTheme.colorScheme.secondary,
                        timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { onHora(reloj.hour, reloj.minute); eligiendoHora = false }) { Text(stringResource(R.string.accion_aceptar)) }
            },
            dismissButton = { TextButton(onClick = { eligiendoHora = false }) { Text(stringResource(R.string.accion_cancelar)) } },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA, heightDp = 800)
@Composable
private fun RecordatorioPreview() = CashinTheme {
    RecordatorioContenido(RecordatorioUiState(ajuste = AjusteRecordatorio(activo = true)), {}, {}, { _, _ -> }, {}, {}, {})
}
