package org.ucb.cashin.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.GrisTexto
import org.ucb.cashin.ui.theme.Naranja
import org.ucb.cashin.ui.theme.VerdeIngreso
import org.ucb.cashin.ui.theme.VerdeOscuro
import kotlin.math.atan2
import kotlin.math.sqrt

/** Una porción de la dona: [valor] es su peso (no hace falta que sumen 100). */
data class PorcionDona(
    val id: String,
    val valor: Float,
    val color: Color,
)

/** Opacidad de las porciones que no están elegidas cuando hay una elegida. */
private const val ALFA_ATENUADA = 0.28f

/** Separación entre porciones, en grados. */
private const val SEPARACION_GRADOS = 1.4f

/**
 * Dona dibujada con Canvas (HU-14). Al aparecer se dibuja en sentido horario en 600 ms;
 * [centro] recibe ese avance (0 a 1) para que el total pueda subir junto con el dibujo.
 * Tocar una porción llama a [onPorcionTocada]: la elegida crece y las demás se atenúan.
 * [claveAnimacion]: cuando cambia (otro mes, otra vista), la dona se vuelve a dibujar.
 */
@Composable
fun GraficoDona(
    porciones: List<PorcionDona>,
    seleccionadaId: String?,
    onPorcionTocada: (id: String) -> Unit,
    descripcion: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 232.dp,
    grosor: Dp = 36.dp,
    claveAnimacion: Any? = null,
    centro: @Composable (progreso: Float) -> Unit,
) {
    val progreso = remember { Animatable(0f) }
    LaunchedEffect(claveAnimacion) {
        progreso.snapTo(0f)
        progreso.animateTo(1f, tween(durationMillis = 600, easing = FastOutSlowInEasing))
    }
    val haptico = LocalHapticFeedback.current

    // Una animación por porción: cuánto crece y cuánto se atenúa.
    val crecimientos = porciones.map { porcion ->
        key(porcion.id) {
            animateFloatAsState(
                targetValue = if (porcion.id == seleccionadaId) 1f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "crecimiento",
            ).value
        }
    }
    val alfas = porciones.map { porcion ->
        key(porcion.id) {
            animateFloatAsState(
                targetValue = if (seleccionadaId == null || porcion.id == seleccionadaId) 1f else ALFA_ATENUADA,
                label = "alfa",
            ).value
        }
    }
    val total = porciones.sumOf { it.valor.toDouble() }.toFloat()

    Box(
        modifier = modifier
            .size(tamano)
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(porciones) {
                    detectTapGestures { toque ->
                        val id = porcionEn(toque, Size(size.width.toFloat(), size.height.toFloat()), grosor.toPx(), porciones, total)
                        if (id != null) {
                            haptico.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPorcionTocada(id)
                        }
                    }
                },
        ) {
            if (total <= 0f) return@Canvas
            val extra = 8.dp.toPx()
            val ancho = grosor.toPx()
            val radio = (size.minDimension - ancho - extra) / 2f
            val esquina = Offset(center.x - radio, center.y - radio)
            val area = Size(radio * 2, radio * 2)
            val avance = 360f * progreso.value
            var inicio = 0f

            porciones.forEachIndexed { i, porcion ->
                val barrido = 360f * porcion.valor / total
                // Solo lo que ya alcanzó la animación de entrada.
                val visible = (minOf(inicio + barrido, avance) - inicio).coerceAtLeast(0f)
                val separacion = if (porciones.size > 1) SEPARACION_GRADOS else 0f
                val dibujado = (visible - separacion).coerceAtLeast(0f)
                if (dibujado > 0f) {
                    drawArc(
                        color = porcion.color.copy(alpha = alfas[i]),
                        startAngle = -90f + inicio + separacion / 2f,
                        sweepAngle = dibujado,
                        useCenter = false,
                        topLeft = esquina,
                        size = area,
                        style = Stroke(width = ancho + extra * crecimientos[i]),
                    )
                }
                inicio += barrido
            }
        }
        centro(progreso.value)
    }
}

/** Qué porción quedó bajo el dedo, o null si se tocó el centro o fuera del aro. */
private fun porcionEn(toque: Offset, area: Size, grosor: Float, porciones: List<PorcionDona>, total: Float): String? {
    if (total <= 0f) return null
    val dx = toque.x - area.width / 2f
    val dy = toque.y - area.height / 2f
    val distancia = sqrt(dx * dx + dy * dy)
    val radioExterno = area.minDimension / 2f
    if (distancia < radioExterno - grosor * 1.6f || distancia > radioExterno) return null
    // 0° arriba y en sentido horario, igual que el dibujo.
    val angulo = (Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 90f + 360f) % 360f
    var acumulado = 0f
    porciones.forEach { porcion ->
        acumulado += 360f * porcion.valor / total
        if (angulo <= acumulado) return porcion.id
    }
    return porciones.lastOrNull()?.id
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA)
@Composable
private fun GraficoDonaPreview() = CashinTheme {
    var elegida by remember { mutableStateOf<String?>(null) }
    GraficoDona(
        porciones = listOf(
            PorcionDona("1", 900f, VerdeIngreso),
            PorcionDona("2", 640f, Naranja),
            PorcionDona("3", 280f, VerdeOscuro),
            PorcionDona("4", 250f, GrisTexto),
        ),
        seleccionadaId = elegida,
        onPorcionTocada = { elegida = if (elegida == it) null else it },
        descripcion = "Gastos por categoría",
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Gastaste", style = MaterialTheme.typography.bodySmall)
            Text("Bs 2.070", style = MaterialTheme.typography.headlineSmall)
        }
    }
}
