package org.ucb.cashin.ui.perfil

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ucb.cashin.R
import org.ucb.cashin.data.AuthRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinCampoTexto
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.util.Validaciones

data class CambiarContrasenaUiState(
    val nueva: String = "",
    val repetida: String = "",
    @StringRes val errorNueva: Int? = null,
    @StringRes val errorRepetida: Int? = null,
    val guardando: Boolean = false,
    val error: ErrorApp? = null,
    val guardada: Boolean = false,
)

/** Cambiar la contraseña con la sesión iniciada: se pide la nueva dos veces. Solo cuentas con correo. */
class CambiarContrasenaViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CambiarContrasenaUiState())
    val uiState: StateFlow<CambiarContrasenaUiState> = _uiState.asStateFlow()

    fun onNueva(valor: String) = _uiState.update { it.copy(nueva = valor, errorNueva = null, error = null) }

    fun onRepetida(valor: String) = _uiState.update { it.copy(repetida = valor, errorRepetida = null, error = null) }

    fun guardar() {
        val estado = _uiState.value
        if (estado.guardando) return
        val errorNueva = if (!Validaciones.contrasenaValida(estado.nueva)) R.string.validacion_contrasena else null
        val errorRepetida = if (estado.repetida != estado.nueva) R.string.validacion_no_coinciden else null
        if (errorNueva != null || errorRepetida != null) {
            _uiState.update { it.copy(errorNueva = errorNueva, errorRepetida = errorRepetida) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, error = null) }
            auth.cambiarContrasena(estado.nueva)
                .onSuccess { _uiState.update { it.copy(guardando = false, guardada = true) } }
                .onFailure { e -> _uiState.update { it.copy(guardando = false, error = e.comoErrorApp()) } }
        }
    }

    companion object {
        val Factory = fabricaDe { CambiarContrasenaViewModel(it.authRepository) }
    }
}

@Composable
fun CambiarContrasenaScreen(
    onAtras: () -> Unit,
    onGuardada: () -> Unit,
    viewModel: CambiarContrasenaViewModel = viewModel(factory = CambiarContrasenaViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(estado.guardada) { if (estado.guardada) onGuardada() }
    CambiarContrasenaContenido(estado, onAtras, viewModel::onNueva, viewModel::onRepetida, viewModel::guardar)
}

@Composable
fun CambiarContrasenaContenido(
    estado: CambiarContrasenaUiState,
    onAtras: () -> Unit,
    onNueva: (String) -> Unit,
    onRepetida: (String) -> Unit,
    onGuardar: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(titulo = stringResource(R.string.contrasena_titulo), onAtras = onAtras)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.pantalla, vertical = Espacio.s),
            verticalArrangement = Arrangement.spacedBy(Espacio.m),
        ) {
            Surface(color = CashinColores.fondoAvatar, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.contrasena_ayuda),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = Espacio.m, vertical = 10.dp),
                )
            }
            CashinCampoTexto(
                etiqueta = stringResource(R.string.campo_nueva_contrasena),
                valor = estado.nueva,
                onValorCambio = onNueva,
                esContrasena = true,
                error = estado.errorNueva?.let { stringResource(it) },
            )
            CashinCampoTexto(
                etiqueta = stringResource(R.string.campo_confirmar_contrasena),
                valor = estado.repetida,
                onValorCambio = onRepetida,
                esContrasena = true,
                accionIme = ImeAction.Done,
                onAccionIme = onGuardar,
                error = estado.errorRepetida?.let { stringResource(it) },
            )
            Text(
                stringResource(R.string.validacion_contrasena),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            estado.error?.let { Text(it.mensaje(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
        }
        CashinBoton(
            texto = stringResource(R.string.contrasena_guardar),
            onClick = onGuardar,
            cargando = estado.guardando,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Espacio.pantalla, vertical = Espacio.m),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun CambiarContrasenaPreview() = CashinTheme {
    CambiarContrasenaContenido(CambiarContrasenaUiState(errorRepetida = R.string.validacion_no_coinciden), {}, {}, {}, {})
}
