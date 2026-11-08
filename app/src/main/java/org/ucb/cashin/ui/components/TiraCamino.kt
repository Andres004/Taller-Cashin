package org.ucb.cashin.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.ucb.cashin.R
import org.ucb.cashin.prediccion.CadenciaPlan
import org.ucb.cashin.prediccion.EstadoPaso
import org.ucb.cashin.prediccion.EstadoPlan
import org.ucb.cashin.prediccion.PasoAhorro
import org.ucb.cashin.prediccion.PlanAhorro
import org.ucb.cashin.prediccion.pasosResumidos
import org.ucb.cashin.ui.theme.Blanco
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.CremaTarjeta
import org.ucb.cashin.ui.theme.Naranja
import org.ucb.cashin.ui.theme.NaranjaClaro
import org.ucb.cashin.ui.theme.NaranjaRelieve
import org.ucb.cashin.ui.theme.VerdeClaro
import java.math.BigDecimal
import java.time.LocalDate

private val ALTO_TIRA = 92.dp
private val MARGEN_TIRA = 36.dp
private val ALTURAS_TIRA = listOf(50.dp, 40.dp, 46.dp, 40.dp, 46.dp)
private val MONEDA_TIRA = 40.dp
private val MONEDA_TIRA_ACTUAL = 54.dp
private val MONEDA_TIRA_META = 52.dp
private val MONEDA_TIRA_LOGRADA = 60.dp
private const val ESCALA_TIRA = 0.68f

/**
 * El camino en una sola fila para Inicio: los pasos cercanos y el trofeo al final.
 * Con [plan] null o sin datos solo se ve el trofeo, sin pasos ni montos.
 */
@Composable
fun TiraCamino(
    plan: PlanAhorro?,
    lograda: Boolean,
    animarEntrada: Boolean,
    modifier: Modifier = Modifier,
) {
    val pasos = plan?.pasosResumidos().orEmpty()
    val hasta = when {
        lograda -> pasos.size
        pasos.isEmpty() -> 0
        else -> pasos.indexOfFirst { it.estado == EstadoPaso.ACTUAL }
            .takeIf { it >= 0 } ?: pasos.indexOfLast { it.estado == EstadoPaso.COMPLETADO }
    }
    val avance = remember { Animatable(if (animarEntrada) 0f else hasta.coerceAtLeast(0).toFloat()) }
    LaunchedEffect(hasta) {
        avance.animateTo(hasta.coerceAtLeast(0).toFloat(), tween(900, easing = FastOutSlowInEasing))
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(ALTO_TIRA)
            .clipToBounds(),
    ) {
        // Sin pasos, el sendero sale desde un punto invisible a la izquierda hasta el trofeo.
        val cantidad = (pasos.size + 1).coerceAtLeast(2)
        val tramo = (maxWidth - MARGEN_TIRA * 2) / (cantidad - 1)
        val x = { i: Int -> MARGEN_TIRA + tramo * i }
        val y = { i: Int -> ALTURAS_TIRA[i % ALTURAS_TIRA.size] }

        Canvas(modifier = Modifier.fillMaxSize()) {
            dibujarFondoTira()
            val puntos = List(cantidad) { Offset(x(it).toPx(), y(it).toPx()) }
            dibujarSendero(puntos, hasta, avance.value, ESCALA_TIRA)
        }

        pasos.forEachIndexed { i, paso -> key(paso.numero) { NodoTira(paso, x(i), y(i)) } }
        FinalTira(lograda, x(cantidad - 1), y(cantidad - 1))
    }
}

private fun DrawScope.dibujarFondoTira() {
    val u = size.width / 324
    val v = 1.dp.toPx()
    drawRect(Brush.verticalGradient(0f to CremaTarjeta, 1f to NaranjaClaro))
    drawPath(
        Path().apply {
            moveTo(0f, 74 * v)
            cubicTo(60 * u, 52 * v, 120 * u, 86 * v, 190 * u, 72 * v)
            cubicTo(260 * u, 58 * v, 290 * u, 56 * v, size.width, 68 * v)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        },
        VerdeClaro.copy(alpha = 0.8f),
    )
}

@Composable
private fun NodoTira(paso: PasoAhorro, x: Dp, y: Dp) {
    val actual = paso.estado == EstadoPaso.ACTUAL
    val tamano = if (actual) MONEDA_TIRA_ACTUAL else MONEDA_TIRA
    val estilo = when (paso.estado) {
        EstadoPaso.COMPLETADO -> EstiloMoneda.ORO
        EstadoPaso.ACTUAL -> EstiloMoneda.ACTUAL
        EstadoPaso.PENDIENTE -> EstiloMoneda.PENDIENTE
    }
    if (actual) AroPulsante(Modifier.offset(x - 36.dp, y - 36.dp), tamano = 72.dp, grosor = 4.dp)
    MonedaPaso(
        estilo = estilo,
        tamano = tamano,
        escalaBorde = ESCALA_TIRA,
        modifier = Modifier.offset(x - tamano / 2 - MARGEN_MONEDA, y - tamano / 2 - MARGEN_MONEDA),
    ) {
        if (paso.estado == EstadoPaso.COMPLETADO) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.camino_hecho),
                tint = Blanco,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(
                text = paso.numero.toString(),
                style = if (actual) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (actual) Blanco else NaranjaRelieve,
            )
        }
    }
}

@Composable
private fun FinalTira(lograda: Boolean, x: Dp, y: Dp) {
    val tamano = if (lograda) MONEDA_TIRA_LOGRADA else MONEDA_TIRA_META
    MonedaPaso(
        estilo = EstiloMoneda.META,
        tamano = tamano,
        escalaBorde = ESCALA_TIRA,
        modifier = Modifier.offset(x - tamano / 2 - MARGEN_MONEDA, y - tamano / 2 - MARGEN_MONEDA),
    ) {
        if (lograda) {
            ImagenCashi(
                recurso = RecursoCashi.META_LOGRADA,
                contentDescription = stringResource(R.string.camino_tu_meta),
                modifier = Modifier.size(tamano - 8.dp).clip(CircleShape),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = stringResource(R.string.camino_tu_meta),
                tint = Naranja,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFCF3)
@Composable
private fun TiraCaminoPreview() = CashinTheme {
    fun paso(numero: Int, estado: EstadoPaso, esMeta: Boolean = false) =
        PasoAhorro(numero, LocalDate.of(2026, 11, 30), BigDecimal("240"), BigDecimal("240"), estado, esMeta)
    TiraCamino(
        plan = PlanAhorro(
            EstadoPlan.ALCANZA, CadenciaPlan.MENSUAL, 1, BigDecimal("240"),
            listOf(
                paso(1, EstadoPaso.COMPLETADO), paso(2, EstadoPaso.COMPLETADO), paso(3, EstadoPaso.ACTUAL),
                paso(4, EstadoPaso.PENDIENTE), paso(5, EstadoPaso.PENDIENTE, esMeta = true),
            ),
            null,
        ),
        lograda = false,
        animarEntrada = false,
    )
}
