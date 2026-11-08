package org.ucb.cashin.ui.desafios

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import org.ucb.cashin.R
import org.ucb.cashin.model.Desafio
import org.ucb.cashin.model.EstadoDesafio
import org.ucb.cashin.model.ReglaDesafio
import org.ucb.cashin.ui.components.AvatarImagen
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.Emocion
import org.ucb.cashin.ui.components.RecursoCashi
import org.ucb.cashin.ui.components.VarianteBoton
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.util.Formato
import java.math.BigDecimal
import kotlin.math.sin
import kotlin.random.Random

/**
 * HU-23: lo que se ve cuando la evaluación cierra un desafío.
 * - Cumplido: lluvia de confeti, Cashi salta celebrando y el XP sube contando de 0 a +50/+150.
 * - Fallido: Cashi se sacude preocupado, dice por qué no salió y anima a intentar otro reto.
 * [onOtroReto] agrega el botón "Ver mis desafíos" (por ejemplo, desde Inicio abre Desafíos).
 */
@Composable
fun ResultadoDesafioDialogo(
    desafio: Desafio,
    onCerrar: () -> Unit,
    onOtroReto: (() -> Unit)? = null,
) {
    val cumplido = desafio.estado == EstadoDesafio.CUMPLIDO

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (cumplido) Confeti(modifier = Modifier.fillMaxSize())

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Espacio.pantalla),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Espacio.s),
                    modifier = Modifier.padding(Espacio.l),
                ) {
                    if (cumplido) CashiCelebrando() else CashiPreocupado()

                    Text(
                        text = stringResource(if (cumplido) R.string.desafios_resultado_cumplido else R.string.desafios_resultado_fallido),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (cumplido) MaterialTheme.colorScheme.secondary else CashinColores.gasto,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = desafio.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )

                    if (cumplido) {
                        XpQueSube(xp = desafio.xpRecompensa)
                        Text(
                            text = stringResource(R.string.desafios_resultado_cumplido_mensaje),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Text(
                            text = motivoFallido(desafio),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(R.string.desafios_resultado_fallido_mensaje),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        )
                    }

                    CashinBoton(
                        texto = stringResource(if (cumplido) R.string.desafios_resultado_genial else R.string.desafios_resultado_entendido),
                        onClick = onCerrar,
                        modifier = Modifier.padding(top = Espacio.s),
                    )
                    if (onOtroReto != null) {
                        CashinBoton(
                            texto = stringResource(R.string.desafios_resultado_ver),
                            onClick = onOtroReto,
                            variante = VarianteBoton.TEXTO,
                        )
                    }
                }
            }
        }
    }
}

/** "Te pasaste del límite de Bs 175,00.", "Te faltaron días…", "No llegaste a 10 movimientos."… */
@Composable
fun motivoFallido(desafio: Desafio): String {
    val objetivo = desafio.diasObjetivo ?: 0
    return when (desafio.regla) {
        ReglaDesafio.LIMITE_CATEGORIA, ReglaDesafio.LIMITE_TOTAL ->
            stringResource(R.string.desafios_motivo_limite, Formato.monto(desafio.montoLimite ?: BigDecimal.ZERO))
        ReglaDesafio.DIAS_REGISTRO -> pluralStringResource(R.plurals.desafios_motivo_dias, objetivo, objetivo)
        ReglaDesafio.CANTIDAD_REGISTROS -> stringResource(R.string.desafios_motivo_movimientos, objetivo)
        ReglaDesafio.DIAS_SIN_GASTO -> stringResource(R.string.desafios_motivo_sin_gasto, objetivo)
        ReglaDesafio.AHORRO_META -> pluralStringResource(R.plurals.desafios_motivo_ahorros, objetivo, objetivo)
    }
}

/** Cashi entra "rebotando" y luego queda saltando suave. */
@Composable
private fun CashiCelebrando() {
    val escala = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) {
        escala.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    val salto by rememberInfiniteTransition(label = "salto").animateFloat(
        initialValue = 0f,
        targetValue = -14f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 650), RepeatMode.Reverse),
        label = "salto",
    )
    AvatarImagen(
        emocion = Emocion.CELEBRANDO,
        tamano = 128.dp,
        recurso = RecursoCashi.DESAFIO_CUMPLIDO,
        modifier = Modifier.graphicsLayer {
            scaleX = escala.value
            scaleY = escala.value
            translationY = salto * density
        },
    )
}

/** Cashi niega con la cabeza (se sacude de lado a lado) al aparecer. */
@Composable
private fun CashiPreocupado() {
    val sacudida = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(150)
        listOf(-14f, 14f, -10f, 10f, -5f, 0f).forEach { sacudida.animateTo(it, tween(durationMillis = 80)) }
    }
    AvatarImagen(
        emocion = Emocion.PREOCUPADO,
        tamano = 120.dp,
        modifier = Modifier.graphicsLayer { translationX = sacudida.value * density },
    )
}

/** "+50 XP" que cuenta desde 0, en naranja y grande. */
@Composable
private fun XpQueSube(xp: Int) {
    var objetivo by remember { mutableIntStateOf(0) }
    LaunchedEffect(xp) {
        delay(300)
        objetivo = xp
    }
    val mostrado by animateIntAsState(targetValue = objetivo, animationSpec = tween(durationMillis = 1200), label = "xp")
    Text(
        text = stringResource(R.string.desafios_xp, mostrado),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = CashinColores.xp,
    )
}

/** Un papelito del confeti: dónde empieza, qué tan rápido cae y cómo gira. */
private data class Papelito(
    val x: Float,
    val desfase: Float,
    val velocidad: Float,
    val tamano: Float,
    val giro: Float,
    val color: Color,
)

/** Confeti cayendo sin parar con los colores de Cashin (dibujado con Canvas, sin librerías). */
@Composable
private fun Confeti(modifier: Modifier = Modifier) {
    val colores = listOf(
        CashinColores.xp,
        MaterialTheme.colorScheme.primary,
        CashinColores.ingreso,
        CashinColores.progreso,
        MaterialTheme.colorScheme.secondary,
    )
    val papelitos = remember {
        List(CANTIDAD_PAPELITOS) { i ->
            Papelito(
                x = Random.nextFloat(),
                desfase = Random.nextFloat(),
                velocidad = 0.6f + Random.nextFloat() * 0.8f,
                tamano = 14f + Random.nextFloat() * 14f,
                giro = Random.nextFloat() * 360f,
                color = colores[i % colores.size],
            )
        }
    }

    // Tiempo continuo en segundos (así el confeti nunca "salta" al reiniciar una animación).
    var segundos by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val inicio = withFrameNanos { it }
        while (true) withFrameNanos { segundos = (it - inicio) / 1_000_000_000f }
    }

    Canvas(modifier = modifier) {
        papelitos.forEach { p ->
            val avance = (segundos * p.velocidad * 0.3f + p.desfase) % 1f
            val y = avance * (size.height + 80f) - 40f
            val x = p.x * size.width + sin(segundos * 2f + p.desfase * 10f) * 24f
            rotate(degrees = p.giro + segundos * 180f * p.velocidad, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color,
                    topLeft = Offset(x - p.tamano / 2, y - p.tamano / 4),
                    size = Size(p.tamano, p.tamano / 2),
                )
            }
        }
    }
}

private const val CANTIDAD_PAPELITOS = 80
