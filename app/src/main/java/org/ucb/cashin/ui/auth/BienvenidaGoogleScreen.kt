package org.ucb.cashin.ui.auth

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import org.ucb.cashin.data.PerfilRepository
import org.ucb.cashin.data.PreferenciasCuenta
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.Moneda
import org.ucb.cashin.model.Perfil
import org.ucb.cashin.model.PerfilEditable
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinCampoTexto
import org.ucb.cashin.ui.components.ImagenCashi
import org.ucb.cashin.ui.components.RecursoCashi
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.util.Validaciones
import java.time.Duration
import java.time.Instant

/** Una cuenta de Google nueva se reconoce porque su perfil se creó hace menos de esto. */
private val VENTANA_CUENTA_NUEVA: Duration = Duration.ofMinutes(15)

data class BienvenidaGoogleUiState(
    val perfil: Perfil? = null,
    val nombre: String = "",
    val moneda: Moneda = Moneda.BOB,
    val errorNombre: Int? = null,
    val guardando: Boolean = false,
    val error: ErrorApp? = null,
    val lista: Boolean = false,
)

/**
 * HU-02: la primera vez que alguien entra con Google se le muestra una pantalla corta para confirmar
 * su nombre (viene de Google) y elegir la moneda. Se muestra una sola vez por cuenta en este teléfono.
 */
class BienvenidaGoogleViewModel(
    private val auth: AuthRepository,
    private val perfilRepository: PerfilRepository,
    private val preferencias: PreferenciasCuenta,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BienvenidaGoogleUiState())
    val uiState: StateFlow<BienvenidaGoogleUiState> = _uiState.asStateFlow()

    /**
     * True si hay que mostrar la bienvenida: cuenta de Google, perfil recién creado y todavía no vista.
     * Deja el perfil cargado para la pantalla.
     */
    suspend fun debeMostrarse(): Boolean {
        val usuario = auth.usuarioIdActual() ?: return false
        if (!auth.esCuentaDeGoogle() || preferencias.bienvenidaGoogleVista(usuario)) return false
        val perfil = perfilRepository.obtener().getOrNull() ?: return false
        val creado = perfil.creadoEn ?: return false
        if (Duration.between(creado, Instant.now()) > VENTANA_CUENTA_NUEVA) {
            preferencias.marcarBienvenidaGoogle(usuario) // cuenta vieja en un teléfono nuevo: no se muestra
            return false
        }
        _uiState.update { it.copy(perfil = perfil, nombre = perfil.nombre, moneda = perfil.moneda) }
        return true
    }

    fun onNombre(valor: String) = _uiState.update { it.copy(nombre = valor.take(Validaciones.MAX_NOMBRE), errorNombre = null) }

    fun onMoneda(moneda: Moneda) = _uiState.update { it.copy(moneda = moneda) }

    /** Guarda nombre y moneda, y marca la bienvenida como vista. */
    fun empezar() {
        val estado = _uiState.value
        val perfil = estado.perfil ?: return terminar()
        if (estado.guardando) return
        if (!Validaciones.nombreValido(estado.nombre)) {
            _uiState.update { it.copy(errorNombre = R.string.validacion_nombre) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, error = null) }
            perfilRepository.actualizar(perfil.id, PerfilEditable(estado.nombre.trim(), estado.moneda, perfil.avatar))
                .onSuccess { terminar() }
                .onFailure { e -> _uiState.update { it.copy(guardando = false, error = e.comoErrorApp()) } }
        }
    }

    /** "Atrás" también la da por vista: no se vuelve a insistir. */
    fun terminar() {
        auth.usuarioIdActual()?.let(preferencias::marcarBienvenidaGoogle)
        _uiState.update { it.copy(guardando = false, lista = true) }
    }

    companion object {
        val Factory = fabricaDe { BienvenidaGoogleViewModel(it.authRepository, it.perfilRepository, it.preferenciasCuenta) }
    }
}

@Composable
fun BienvenidaGoogleScreen(onLista: () -> Unit, viewModel: BienvenidaGoogleViewModel) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(estado.lista) { if (estado.lista) onLista() }
    BackHandler { viewModel.terminar() }
    BienvenidaGoogleContenido(estado, viewModel::onNombre, viewModel::onMoneda, viewModel::empezar)
}

@Composable
fun BienvenidaGoogleContenido(
    estado: BienvenidaGoogleUiState,
    onNombre: (String) -> Unit,
    onMoneda: (Moneda) -> Unit,
    onEmpezar: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.pantalla, vertical = Espacio.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espacio.m),
        ) {
            ImagenCashi(RecursoCashi.BIENVENIDA, contentDescription = stringResource(R.string.avatar_descripcion), modifier = Modifier.size(150.dp))
            Text(
                text = stringResource(R.string.bienvenida_google_titulo, estado.nombre.trim().substringBefore(' ').ifBlank { estado.perfil?.nombre.orEmpty() }),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.bienvenida_google_subtitulo),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            CashinCampoTexto(
                etiqueta = stringResource(R.string.bienvenida_google_nombre),
                valor = estado.nombre,
                onValorCambio = onNombre,
                error = estado.errorNombre?.let { stringResource(it) },
                capitalizar = true,
            )
            Text(
                text = stringResource(R.string.editar_perfil_moneda),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
            )
            SelectorMonedaBienvenida(estado.moneda, onMoneda)
            estado.error?.let { Text(it.mensaje(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
        CashinBoton(
            texto = stringResource(R.string.bienvenida_google_empezar),
            onClick = onEmpezar,
            cargando = estado.guardando,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Espacio.pantalla, vertical = Espacio.m),
        )
    }
}

/** "Bs | $us" como control segmentado. */
@Composable
private fun SelectorMonedaBienvenida(moneda: Moneda, onMoneda: (Moneda) -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(3.dp)) {
            Moneda.entries.forEach { opcion ->
                val elegida = opcion == moneda
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (elegida) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .selectable(selected = elegida, role = Role.RadioButton, onClick = { onMoneda(opcion) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(if (opcion == Moneda.BOB) R.string.editar_perfil_moneda_bob else R.string.editar_perfil_moneda_usd),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (elegida) FontWeight.Bold else FontWeight.Medium,
                        color = if (elegida) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA, heightDp = 800)
@Composable
private fun BienvenidaGooglePreview() = CashinTheme {
    BienvenidaGoogleContenido(BienvenidaGoogleUiState(nombre = "Andres Mamani"), {}, {}, {})
}
