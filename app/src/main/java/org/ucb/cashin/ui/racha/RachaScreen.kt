package org.ucb.cashin.ui.racha

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.model.semanaDeRacha
import org.ucb.cashin.ui.components.AvatarBurbuja
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.Emocion
import org.ucb.cashin.ui.components.EstadoCargando
import org.ucb.cashin.ui.components.EstadoError
import org.ucb.cashin.ui.components.ImagenCashi
import org.ucb.cashin.ui.components.RecursoCashi
import org.ucb.cashin.ui.components.SemanaRacha
import org.ucb.cashin.ui.components.TarjetaJuego
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.ui.theme.Naranja
import java.time.LocalDate

/** HU-24: la racha con su momento propio: Cashi, los días seguidos y la semana. */
@Composable
fun RachaScreen(
    onAtras: () -> Unit,
    onAnotar: () -> Unit,
    viewModel: RachaViewModel = viewModel(factory = RachaViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    RachaContenido(estado = estado, onAtras = onAtras, onAnotar = onAnotar, onReintentar = viewModel::cargar)
}

@Composable
fun RachaContenido(
    estado: RachaUiState,
    onAtras: () -> Unit,
    onAnotar: () -> Unit,
    onReintentar: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(titulo = stringResource(R.string.racha_titulo), onAtras = onAtras)
        when {
            estado.cargando -> EstadoCargando()
            estado.error != null -> EstadoError(mensaje = estado.error.mensaje(), onReintentar = onReintentar)
            else -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Espacio.m),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Espacio.pantalla, vertical = Espacio.s),
            ) {
                Encabezado(estado)
                TarjetaJuego {
                    Text(
                        text = stringResource(R.string.racha_esta_semana),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = Espacio.s),
                    )
                    SemanaRacha(dias = estado.semana)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Espacio.m),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                ) {
                    Dato(
                        recurso = RecursoCashi.SUBIDA_NIVEL,
                        titulo = stringResource(R.string.perfil_racha_maxima),
                        valor = pluralStringResource(R.plurals.perfil_dias, estado.rachaMaxima, estado.rachaMaxima),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    Dato(
                        recurso = RecursoCashi.XP,
                        titulo = stringResource(R.string.perfil_nivel, estado.nivel),
                        valor = stringResource(R.string.perfil_xp, estado.xp),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                AvatarBurbuja(
                    mensaje = stringResource(R.string.racha_como_funciona),
                    emocion = Emocion.FELIZ,
                    tamanoAvatar = 64.dp,
                )
                if (!estado.anotoHoy) {
                    CashinBoton(texto = stringResource(R.string.historial_vacio_accion), onClick = onAnotar)
                }
            }
        }
    }
}

@Composable
private fun Encabezado(estado: RachaUiState) {
    val recurso = when {
        estado.racha == 0 -> RecursoCashi.PENSANDO
        !estado.anotoHoy -> RecursoCashi.PREOCUPADO
        else -> RecursoCashi.RACHA
    }
    ImagenCashi(
        recurso = recurso,
        contentDescription = stringResource(R.string.avatar_descripcion),
        modifier = Modifier.size(190.dp),
    )
    Text(
        text = pluralStringResource(R.plurals.perfil_dias, estado.racha, estado.racha),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = Naranja,
    )
    Text(
        text = stringResource(
            when {
                estado.racha == 0 -> R.string.racha_mensaje_sin_racha
                !estado.anotoHoy -> R.string.racha_mensaje_en_riesgo
                else -> R.string.racha_mensaje_viva
            },
        ),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Dato(recurso: RecursoCashi, titulo: String, valor: String, modifier: Modifier = Modifier) {
    TarjetaJuego(modifier = modifier) {
        ImagenCashi(recurso = recurso, contentDescription = null, modifier = Modifier.size(64.dp))
        Text(
            text = titulo,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.titleLarge,
            color = CashinColores.xp,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA, heightDp = 900)
@Composable
private fun RachaPreview() = CashinTheme {
    val hoy = LocalDate.of(2026, 11, 6)
    RachaContenido(
        estado = RachaUiState(racha = 4, rachaMaxima = 9, anotoHoy = true, nivel = 2, xp = 230, semana = semanaDeRacha(hoy, 4, true), cargando = false),
        onAtras = {},
        onAnotar = {},
        onReintentar = {},
    )
}
