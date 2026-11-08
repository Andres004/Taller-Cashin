package org.ucb.cashin.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.ucb.cashin.ui.theme.Crema
import org.ucb.cashin.ui.theme.CremaTarjeta
import org.ucb.cashin.ui.theme.Naranja
import org.ucb.cashin.ui.theme.NaranjaClaro
import org.ucb.cashin.ui.theme.Verde
import org.ucb.cashin.ui.theme.VerdeCategoria
import org.ucb.cashin.ui.theme.VerdeClaro
import org.ucb.cashin.ui.theme.VerdeMedio
import org.ucb.cashin.ui.theme.VerdeOscuro

/** El paisaje del camino: cielo, colinas, nubes y pinos alrededor de la meta. Medidas sobre un ancho de 360 dp. */
internal fun DrawScope.dibujarFondoCamino(yMeta: Float) {
    val u = 1.dp.toPx()
    val x = size.width / 360

    drawRect(Brush.verticalGradient(0f to Crema, 0.55f to NaranjaClaro, 1f to VerdeClaro))
    drawCircle(Naranja.copy(alpha = 0.14f), radius = 118 * u, center = Offset(size.width / 2, yMeta))

    val a = yMeta - 92 * u
    colina(VerdeClaro.copy(alpha = 0.75f)) {
        moveTo(0f, a + 40 * u)
        cubicTo(70 * x, a - 14 * u, 130 * x, a + 6 * u, 190 * x, a + 36 * u)
        cubicTo(250 * x, a + 66 * u, 310 * x, a + 18 * u, 360 * x, a - 10 * u)
    }
    val b = yMeta - 34 * u
    colina(VerdeCategoria.copy(alpha = 0.8f)) {
        moveTo(0f, b + 44 * u)
        cubicTo(90 * x, b + 6 * u, 170 * x, b + 60 * u, 250 * x, b + 38 * u)
        cubicTo(330 * x, b + 16 * u, 330 * x, b + 20 * u, 360 * x, b + 30 * u)
    }

    listOf(Triple(34f, 70f, 0.8f), Triple(26f, 318f, 0.7f)).forEach { (nx, ny, escala) ->
        if (ny * u < yMeta - 92 * u) nube(Offset(nx * u, ny * u), escala * u)
    }
    pino(Offset(38 * u, yMeta + 18 * u), 1.1f * u)
    pino(Offset(74 * u, yMeta + 44 * u), 0.85f * u)
    pino(Offset(size.width - 38 * u, yMeta + 14 * u), 1.15f * u)
    pino(Offset(size.width - 74 * u, yMeta + 46 * u), 0.8f * u)
}

/**
 * El sendero: punteado hasta la meta desde el punto [hasta], y verde lo recorrido.
 * [avance] es en unidades de punto (1,5 = a mitad del segundo tramo), así se puede animar.
 */
internal fun DrawScope.dibujarSendero(puntos: List<Offset>, hasta: Int, avance: Float) {
    if (puntos.size < 2) return
    val u = 1.dp.toPx()
    if (hasta < puntos.lastIndex) {
        val falta = ruta(puntos.drop(hasta.coerceAtLeast(0)))
        drawPath(falta, CremaTarjeta, style = Stroke(20 * u, cap = StrokeCap.Round))
        drawPath(
            falta,
            Naranja,
            style = Stroke(
                width = 5 * u,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(1 * u, 13 * u)),
            ),
        )
    }
    if (avance <= 0f) return
    val hecho = recorrido(puntos, avance)
    drawPath(hecho, VerdeOscuro.copy(alpha = 0.18f), style = Stroke(22 * u, cap = StrokeCap.Round))
    drawPath(hecho, VerdeMedio, style = Stroke(18 * u, cap = StrokeCap.Round))
}

private fun ruta(puntos: List<Offset>): Path = Path().apply {
    moveTo(puntos.first().x, puntos.first().y)
    puntos.zipWithNext { desde, hacia ->
        val medio = (desde.y + hacia.y) / 2
        cubicTo(desde.x, medio, hacia.x, medio, hacia.x, hacia.y)
    }
}

private fun recorrido(puntos: List<Offset>, avance: Float): Path {
    val completos = avance.toInt().coerceAtMost(puntos.lastIndex)
    val camino = ruta(puntos.take(completos + 1))
    val parte = avance - completos
    if (parte > 0f && completos < puntos.lastIndex) {
        val medida = PathMeasure()
        medida.setPath(ruta(listOf(puntos[completos], puntos[completos + 1])), false)
        val tramo = Path()
        medida.getSegment(0f, medida.length * parte, tramo, true)
        camino.addPath(tramo)
    }
    return camino
}

private fun DrawScope.colina(color: Color, borde: Path.() -> Unit) {
    val forma = Path().apply {
        borde()
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(forma, color)
}

private fun DrawScope.nube(centro: Offset, u: Float) {
    val color = CremaTarjeta.copy(alpha = 0.95f)
    fun ovalo(cx: Float, cy: Float, rx: Float, ry: Float) = drawOval(
        color,
        topLeft = centro + Offset((cx - rx) * u, (cy - ry) * u),
        size = Size(rx * 2 * u, ry * 2 * u),
    )
    ovalo(0f, 0f, 24f, 11f)
    ovalo(-13f, -7f, 12f, 10f)
    ovalo(12f, -9f, 15f, 12f)
}

private fun DrawScope.pino(base: Offset, u: Float) {
    fun triangulo(punta: Float, abajo: Float, medio: Float, color: Color) {
        drawPath(
            Path().apply {
                moveTo(base.x, base.y + punta * u)
                lineTo(base.x - medio * u, base.y + abajo * u)
                lineTo(base.x + medio * u, base.y + abajo * u)
                close()
            },
            color,
        )
    }
    drawRect(VerdeOscuro, topLeft = base + Offset(-2 * u, 0f), size = Size(4 * u, 9 * u))
    triangulo(-24f, 2f, 13f, VerdeMedio)
    triangulo(-34f, -12f, 10f, Verde)
}
