package org.ucb.cashin.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import org.ucb.cashin.R
import org.ucb.cashin.model.DiaRacha
import org.ucb.cashin.model.EstadoDiaRacha
import org.ucb.cashin.model.RecompensaAnotar
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.model.semanaDeRacha
import org.ucb.cashin.ui.theme.BordeCampo
import org.ucb.cashin.ui.theme.Blanco
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.CremaTarjeta
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.ui.theme.Naranja
import org.ucb.cashin.util.Formato
import java.time.format.TextStyle
import java.util.Locale

private const val MS_PREMIO_VISIBLE = 1900L
private const val MS_PREMIO_SALIDA = 260L
private val LOCALE_ES = Locale.forLanguageTag("es")

/** Premio corto al anotar: Cashi aparece abajo con el XP y se va solo, sin frenar a nadie. */
@Composable
fun PremioRapido(
    recompensa: RecompensaAnotar,
    onFin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember(recompensa) { mutableStateOf(false) }
    val hapticos = LocalHapticFeedback.current
    LaunchedEffect(recompensa) {
        hapticos.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        visible = true
        delay(MS_PREMIO_VISIBLE)
        visible = false
        delay(MS_PREMIO_SALIDA)
        onFin()
    }
    val recurso = when {
        recompensa.tipo == TipoRegistro.INGRESO -> RecursoCashi.AHORRO
        recompensa.xpGanado > 0 -> RecursoCashi.XP
        else -> RecursoCashi.FELIZ
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        TarjetaJuego(
            conRelleno = false,
            modifier = Modifier.padding(horizontal = Espacio.pantalla, vertical = Espacio.s),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacio.s),
                modifier = Modifier.padding(horizontal = Espacio.s, vertical = 6.dp),
            ) {
                ImagenCashi(recurso = recurso, contentDescription = null, modifier = Modifier.size(64.dp))
                Text(
                    text = stringResource(R.string.anotar_guardado),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                if (recompensa.xpGanado > 0) {
                    Text(
                        text = stringResource(R.string.desafios_xp, recompensa.xpGanado),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CashinColores.xp,
                    )
                }
            }
        }
    }
}

/** La primera anotación del día: Cashi abraza la llama, la racha sube y se ve la semana. */
@Composable
fun CelebracionRacha(
    recompensa: RecompensaAnotar,
    onCerrar: () -> Unit,
    onVerRacha: () -> Unit,
) {
    val hapticos = LocalHapticFeedback.current
    val escala = remember { Animatable(0.3f) }
    var confeti by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        hapticos.performHapticFeedback(HapticFeedbackType.LongPress)
        escala.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
    }

    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                if (confeti) Confeti(foco = 300.dp, onFin = { confeti = false })
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Espacio.s, Alignment.CenterVertically),
                    modifier = Modifier.fillMaxSize().padding(Espacio.pantalla),
                ) {
                    ImagenCashi(
                        recurso = RecursoCashi.RACHA,
                        contentDescription = stringResource(R.string.avatar_descripcion),
                        modifier = Modifier
                            .size(220.dp)
                            .graphicsLayer {
                                scaleX = escala.value
                                scaleY = escala.value
                            },
                    )
                    NumeroQueSube(valor = recompensa.racha)
                    Text(
                        text = if (recompensa.racha <= 1) {
                            stringResource(R.string.racha_empezo)
                        } else {
                            pluralStringResource(R.plurals.racha_dias_seguidos, recompensa.racha)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.racha_vuelve_manana),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    val hoy = Formato.hoy()
                    SemanaRacha(
                        dias = semanaDeRacha(hoy, recompensa.racha, anotoHoy = true),
                        modifier = Modifier.padding(vertical = Espacio.s),
                    )
                    if (recompensa.xpGanado > 0) {
                        Text(
                            text = stringResource(R.string.desafios_xp, recompensa.xpGanado),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CashinColores.xp,
                        )
                    }
                    CashinBoton(
                        texto = stringResource(R.string.racha_seguir),
                        onClick = onCerrar,
                        modifier = Modifier.padding(top = Espacio.s),
                    )
                    CashinBoton(
                        texto = stringResource(R.string.racha_ver),
                        onClick = onVerRacha,
                        variante = VarianteBoton.TEXTO,
                    )
                }
            }
        }
    }
}

/** El número grande de la racha, que cuenta desde el día anterior. */
@Composable
private fun NumeroQueSube(valor: Int) {
    var objetivo by remember { mutableIntStateOf((valor - 1).coerceAtLeast(0)) }
    LaunchedEffect(valor) {
        delay(450)
        objetivo = valor
    }
    val mostrado by animateIntAsState(objetivo, tween(500), label = "racha")
    Text(
        text = mostrado.toString(),
        style = MaterialTheme.typography.displayLarge,
        fontWeight = FontWeight.Bold,
        color = Naranja,
    )
}

/** Lunes a domingo: cada día con su letra y un círculo. Lo logrado lleva un visto, no solo color. */
@Composable
fun SemanaRacha(dias: List<DiaRacha>, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        dias.forEach { dia ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = dia.fecha.dayOfWeek.getDisplayName(TextStyle.NARROW, LOCALE_ES).uppercase(LOCALE_ES),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (dia.estado == EstadoDiaRacha.PENDIENTE_HOY) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CirculoDia(dia.estado)
            }
        }
    }
}

@Composable
private fun CirculoDia(estado: EstadoDiaRacha) {
    val logrado = estado == EstadoDiaRacha.LOGRADO
    val fondo: Color = if (logrado) Naranja else CremaTarjeta
    val borde = when (estado) {
        EstadoDiaRacha.LOGRADO, EstadoDiaRacha.PENDIENTE_HOY -> Naranja
        else -> BordeCampo
    }
    Surface(
        shape = CircleShape,
        color = fondo,
        border = BorderStroke(if (estado == EstadoDiaRacha.PENDIENTE_HOY) 3.dp else 2.dp, borde),
        modifier = Modifier.size(36.dp),
    ) {
        if (logrado) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.racha_dia_logrado),
                    tint = Blanco,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun SemanaRachaPreview() = CashinTheme {
    SemanaRacha(semanaDeRacha(java.time.LocalDate.of(2026, 11, 4), racha = 2, anotoHoy = false))
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun PremioRapidoPreview() = CashinTheme {
    PremioRapido(
        recompensa = RecompensaAnotar(TipoRegistro.GASTO, xpGanado = 10, racha = 4, primeraDelDia = false),
        onFin = {},
    )
}
