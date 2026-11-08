package org.ucb.cashin.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import org.ucb.cashin.R
import org.ucb.cashin.prediccion.CadenciaPlan
import org.ucb.cashin.prediccion.EstadoPaso
import org.ucb.cashin.prediccion.EstadoPlan
import org.ucb.cashin.prediccion.PasoAhorro
import org.ucb.cashin.prediccion.PlanAhorro
import org.ucb.cashin.ui.theme.Blanco
import org.ucb.cashin.ui.theme.BordeCampo
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Crema
import org.ucb.cashin.ui.theme.CremaTarjeta
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.ui.theme.Naranja
import org.ucb.cashin.ui.theme.NaranjaClaro
import org.ucb.cashin.ui.theme.NaranjaRelieve
import org.ucb.cashin.ui.theme.Verde
import org.ucb.cashin.ui.theme.VerdeClaro
import org.ucb.cashin.ui.theme.VerdeMedio
import org.ucb.cashin.ui.theme.VerdeOscuro
import org.ucb.cashin.util.Formato
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.math.max as maxFloat
import kotlin.random.Random

/** Se acaba de anotar un ahorro: [numeroPaso] era el paso actual antes de guardarlo. */
data class CelebracionAhorro(val monto: BigDecimal, val numeroPaso: Int?)

private val X_PASOS = listOf(80.dp, 112.dp, 80.dp, 48.dp)
private val X_FILA = 164.dp
private val MARGEN_DERECHO = 16.dp
private val PRIMER_PASO = 44.dp
private val SEPARACION = 58.dp
private val HASTA_META = 112.dp
private val BAJO_META = 118.dp
private val ALTO_FILA = 48.dp
private val ALTO_ACTUAL = 88.dp
private val MONEDA = 58.dp
private val MONEDA_ACTUAL = 72.dp
private val MONEDA_META = 84.dp
private val MONEDA_LOGRADA = 96.dp
private val ANCHO_FINAL = 280.dp
internal val MARGEN_MONEDA = 6.dp
private val ALTO_ENCABEZADO = 48.dp
private val FORMA_FILA = RoundedCornerShape(14.dp)
private const val DURACION_CELEBRACION_MS = 2800L

/** Dónde va cada cosa: las filas crecen si el teléfono usa letra más grande. */
private class EsquemaCamino(plan: PlanAhorro, escalaLetra: Float) {
    val lograda = plan.estado == EstadoPlan.LOGRADA
    val filas: List<PasoAhorro> = if (lograda) plan.pasos else plan.pasos.filterNot { it.esMeta }
    val pasoFinal: PasoAhorro? = plan.pasos.firstOrNull { it.esMeta }.takeUnless { lograda }
    val indiceActual: Int? = filas.indexOfFirst { it.estado == EstadoPaso.ACTUAL }.takeIf { it >= 0 }
    val altoFila = ALTO_FILA * escalaLetra
    val altoActual = ALTO_ACTUAL * escalaLetra
    val ys: List<Dp>
    val yMeta: Dp
    val alto: Dp

    /** Hasta qué punto llega el verde; -1 si todavía no hay nada recorrido. */
    val hasta: Int = when {
        lograda -> filas.size
        indiceActual != null -> indiceActual
        else -> filas.indexOfLast { it.estado == EstadoPaso.COMPLETADO }
    }

    init {
        val separacion = max(SEPARACION, altoFila + 10.dp)
        val junto = altoActual / 2 + altoFila / 2 + 8.dp
        var y = PRIMER_PASO
        ys = filas.indices.map { i ->
            if (i > 0) y += if (indiceActual == i || indiceActual == i - 1) junto else separacion
            y
        }
        yMeta = if (filas.isEmpty()) 70.dp else y + HASTA_META
        alto = yMeta + BAJO_META * escalaLetra
    }

    val foco: Dp get() = indiceActual?.let { ys[it] } ?: yMeta

    fun xPaso(indice: Int): Dp = X_PASOS[indice % X_PASOS.size]
}

/** Altura dentro del camino del paso en el que está la persona: sirve para llevarla ahí. */
fun focoCamino(plan: PlanAhorro, escalaLetra: Float): Dp =
    ALTO_ENCABEZADO + EsquemaCamino(plan, escalaLetra.coerceAtLeast(1f)).foco

/**
 * El camino hasta la meta: de arriba hacia abajo, con una fila igual por paso y el trofeo al final.
 * [animarEntrada] dibuja el sendero y hace rebotar las monedas la primera vez que se muestra.
 */
@Composable
fun CaminoMeta(
    plan: PlanAhorro,
    nombreMeta: String,
    animarEntrada: Boolean,
    onEntradaVista: () -> Unit,
    confetiAlEntrar: Boolean,
    celebracion: CelebracionAhorro?,
    onCelebracionVista: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        EncabezadoCamino(plan, Modifier.padding(horizontal = Espacio.pantalla))
        EscenaCamino(plan, nombreMeta, animarEntrada, onEntradaVista, confetiAlEntrar, celebracion, onCelebracionVista)
    }
}

@Composable
private fun EncabezadoCamino(plan: PlanAhorro, modifier: Modifier) {
    val total = plan.pasos.size
    val actual = plan.pasoActual?.numero
    Column(modifier = modifier.fillMaxWidth().height(ALTO_ENCABEZADO)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.camino_titulo),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (actual != null) {
                    stringResource(R.string.camino_paso_de, actual, total)
                } else {
                    stringResource(R.string.camino_completo)
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = NaranjaRelieve,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 5.dp),
        ) {
            plan.pasos.forEach { paso ->
                val color = when (paso.estado) {
                    EstadoPaso.COMPLETADO -> VerdeMedio
                    EstadoPaso.ACTUAL -> Naranja
                    EstadoPaso.PENDIENTE -> BordeCampo
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(9.dp)
                        .background(color, RoundedCornerShape(5.dp)),
                )
            }
        }
    }
}

@Composable
private fun EscenaCamino(
    plan: PlanAhorro,
    nombreMeta: String,
    animarEntrada: Boolean,
    onEntradaVista: () -> Unit,
    confetiAlEntrar: Boolean,
    celebracion: CelebracionAhorro?,
    onCelebracionVista: () -> Unit,
) {
    val escalaLetra = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val esquema = remember(plan, escalaLetra) { EsquemaCamino(plan, escalaLetra) }
    val hapticos = LocalHapticFeedback.current
    var tocado by remember(plan) { mutableStateOf<Int?>(null) }
    var confetiEntrada by remember { mutableStateOf(confetiAlEntrar) }
    val avance = remember { Animatable(if (animarEntrada) 0f else esquema.hasta.coerceAtLeast(0).toFloat()) }

    LaunchedEffect(Unit) { onEntradaVista() }
    LaunchedEffect(esquema.hasta) {
        val destino = esquema.hasta.coerceAtLeast(0).toFloat()
        val duracion = if (avance.value == 0f) 900 else 400
        avance.animateTo(destino, tween(duracion, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(celebracion) {
        if (celebracion != null) {
            hapticos.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(DURACION_CELEBRACION_MS)
            onCelebracionVista()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(esquema.alto)
            .clipToBounds()
            .clickable(
                enabled = tocado != null,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { tocado = null },
    ) {
        val ancho = maxWidth
        val anchoFila = ancho - X_FILA - MARGEN_DERECHO
        val xMeta = ancho / 2

        Canvas(modifier = Modifier.fillMaxSize()) {
            val yMeta = esquema.yMeta.toPx()
            dibujarFondoCamino(yMeta)
            val puntos = esquema.filas.indices.map { Offset(esquema.xPaso(it).toPx(), esquema.ys[it].toPx()) } +
                Offset(xMeta.toPx(), yMeta)
            dibujarSendero(puntos, esquema.hasta, avance.value)
        }

        esquema.filas.forEachIndexed { i, paso ->
            key(paso.numero) {
                val x = esquema.xPaso(i)
                val y = esquema.ys[i]
                val recien = celebracion != null && paso.numero == celebracion.numeroPaso && paso.estado == EstadoPaso.COMPLETADO
                val alTocar = {
                    hapticos.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    tocado = if (tocado == i) null else i
                }
                if (recien) {
                    Box(
                        modifier = Modifier
                            .offset(x - 48.dp, y - 48.dp)
                            .size(96.dp)
                            .background(NaranjaClaro.copy(alpha = 0.9f), CircleShape),
                    )
                }
                if (paso.estado == EstadoPaso.ACTUAL) AroPulsante(Modifier.offset(x - 46.dp, y - 46.dp))
                MonedaDePaso(paso, x, y, tocado == i, recien, animarEntrada, 120 * i, alTocar)
                if (paso.estado == EstadoPaso.ACTUAL) {
                    FilaActual(paso, y, esquema.altoActual, anchoFila, animarEntrada, celebracion != null, alTocar)
                } else {
                    FilaPaso(paso, y, esquema.altoFila, anchoFila, recien, tocado == i, alTocar)
                }
            }
        }

        FinalDelCamino(esquema, xMeta, nombreMeta, min(ANCHO_FINAL, ancho - 32.dp), animarEntrada, 120 * esquema.filas.size)

        tocado?.let { i -> GloboPaso(esquema.filas[i], esquema.ys[i] + esquema.altoFila / 2 + 6.dp, anchoFila) }

        if (celebracion != null) {
            val indice = esquema.filas.indexOfFirst { it.numero == celebracion.numeroPaso && it.estado == EstadoPaso.COMPLETADO }
            val xChip = if (indice >= 0) esquema.xPaso(indice) else esquema.indiceActual?.let(esquema::xPaso) ?: xMeta
            val yChip = if (indice >= 0) esquema.ys[indice] else esquema.foco
            key(celebracion) {
                ChipSube(
                    texto = stringResource(R.string.camino_ganado, Formato.montoEntero(celebracion.monto)),
                    color = VerdeMedio,
                    modifier = Modifier.offset(xChip + 24.dp, yChip - 50.dp).zIndex(6f),
                )
                Confeti(foco = yChip, onFin = {})
            }
        }
        if (confetiEntrada) Confeti(foco = esquema.yMeta, onFin = { confetiEntrada = false })
    }
}

@Composable
private fun MonedaDePaso(
    paso: PasoAhorro,
    x: Dp,
    y: Dp,
    tocada: Boolean,
    recien: Boolean,
    animar: Boolean,
    retrasoMs: Int,
    onClick: () -> Unit,
) {
    val tamano = when {
        paso.estado == EstadoPaso.ACTUAL -> MONEDA_ACTUAL
        recien -> MONEDA + 4.dp
        else -> MONEDA
    }
    val estilo = when {
        paso.estado == EstadoPaso.COMPLETADO -> EstiloMoneda.ORO
        paso.estado == EstadoPaso.ACTUAL -> EstiloMoneda.ACTUAL
        tocada -> EstiloMoneda.TOCADA
        else -> EstiloMoneda.PENDIENTE
    }
    val escala = remember { Animatable(if (animar) 0.2f else 1f) }
    LaunchedEffect(Unit) {
        if (animar) {
            delay(retrasoMs.toLong())
            escala.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
        }
    }
    LaunchedEffect(tocada, recien) {
        if (tocada || recien) {
            escala.snapTo(0.85f)
            escala.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        }
    }
    MonedaPaso(
        estilo = estilo,
        tamano = tamano,
        modifier = Modifier
            .offset(x - tamano / 2 - MARGEN_MONEDA, y - tamano / 2 - MARGEN_MONEDA)
            .graphicsLayer {
                scaleX = escala.value
                scaleY = escala.value
            }
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        if (paso.estado == EstadoPaso.COMPLETADO) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.camino_hecho),
                tint = Blanco,
                modifier = Modifier.size(28.dp),
            )
        } else {
            Text(
                text = paso.numero.toString(),
                style = if (paso.estado == EstadoPaso.ACTUAL) {
                    MaterialTheme.typography.headlineMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
                color = if (paso.estado == EstadoPaso.ACTUAL) Blanco else NaranjaRelieve,
            )
        }
    }
}

internal enum class EstiloMoneda { ORO, ACTUAL, PENDIENTE, TOCADA, META }

/** Moneda del camino, con relieve abajo y su contenido al centro. */
@Composable
internal fun MonedaPaso(
    estilo: EstiloMoneda,
    tamano: Dp,
    modifier: Modifier = Modifier,
    escalaBorde: Float = 1f,
    contenido: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(tamano + MARGEN_MONEDA * 2)
            .padding(MARGEN_MONEDA)
            .drawBehind { dibujarMoneda(estilo, escalaBorde) },
    ) { contenido() }
}

private fun DrawScope.dibujarMoneda(estilo: EstiloMoneda, escalaBorde: Float) {
    val radio = size.minDimension / 2
    val u = 1.dp.toPx() * escalaBorde
    when (estilo) {
        EstiloMoneda.ORO, EstiloMoneda.ACTUAL -> {
            drawCircle(NaranjaRelieve, radio + 2 * u)
            drawCircle(NaranjaRelieve, radio, center.copy(y = center.y + 5 * u))
            drawCircle(if (estilo == EstiloMoneda.ORO) NaranjaClaro else CremaTarjeta, radio)
            drawCircle(Naranja, radio - 4 * u)
        }
        EstiloMoneda.PENDIENTE, EstiloMoneda.TOCADA -> {
            drawCircle(NaranjaClaro, radio, center.copy(y = center.y + 4 * u))
            drawCircle(CremaTarjeta, radio)
            val efecto = if (estilo == EstiloMoneda.PENDIENTE) PathEffect.dashPathEffect(floatArrayOf(6 * u, 4 * u)) else null
            drawCircle(Naranja, radio - 1.5f * u, style = Stroke(3 * u, pathEffect = efecto))
        }
        EstiloMoneda.META -> {
            drawCircle(NaranjaRelieve, radio, center.copy(y = center.y + 6 * u))
            drawCircle(Naranja, radio)
            drawCircle(CremaTarjeta, radio - 4 * u)
        }
    }
}

@Composable
internal fun AroPulsante(modifier: Modifier, tamano: Dp = 92.dp, grosor: Dp = 5.dp) {
    val transicion = rememberInfiniteTransition(label = "aro")
    val avance by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "pulso",
    )
    Box(
        modifier = modifier
            .size(tamano)
            .graphicsLayer {
                val escala = 0.82f + avance * 0.46f
                scaleX = escala
                scaleY = escala
                alpha = (1f - avance) * 0.44f
            }
            .border(grosor, Naranja, CircleShape),
    )
}

/** Una fila igual para cada paso: fecha a la izquierda y monto a la derecha. */
@Composable
private fun FilaPaso(
    paso: PasoAhorro,
    y: Dp,
    alto: Dp,
    ancho: Dp,
    recien: Boolean,
    tocada: Boolean,
    onClick: () -> Unit,
) {
    val borde = when {
        recien -> BorderStroke(2.dp, VerdeMedio)
        tocada -> BorderStroke(2.dp, Naranja)
        else -> BorderStroke(1.5.dp, BordeCampo)
    }
    Surface(
        onClick = onClick,
        shape = FORMA_FILA,
        color = CremaTarjeta,
        border = borde,
        modifier = Modifier
            .offset(X_FILA, y - alto / 2)
            .width(ancho)
            .height(alto),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 13.dp),
        ) {
            Text(
                text = Formato.fechaCorta(paso.fecha),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = Formato.montoEntero(paso.monto),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (paso.estado == EstadoPaso.COMPLETADO) CashinColores.ingreso else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
        }
    }
}

/** El único paso destacado: cuánto toca ahora y hasta cuándo, con Cashi al lado. */
@Composable
private fun FilaActual(
    paso: PasoAhorro,
    y: Dp,
    alto: Dp,
    ancho: Dp,
    animar: Boolean,
    celebrando: Boolean,
    onClick: () -> Unit,
) {
    val salto = remember { Animatable(0f) }
    LaunchedEffect(celebrando) {
        if (animar || celebrando) {
            if (!celebrando) delay(900)
            salto.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 1000
                    -14f at 300
                    0f at 550
                    -6f at 720
                },
            )
        }
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = CremaTarjeta,
        border = BorderStroke(2.5.dp, Naranja),
        modifier = Modifier
            .offset(X_FILA, y - alto / 2)
            .width(ancho)
            .height(alto),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)) {
                Text(
                    text = stringResource(R.string.camino_proximo),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
                Text(
                    text = Formato.montoEntero(paso.monto),
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 23.sp, lineHeight = 28.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(R.string.camino_antes_de, Formato.fechaCorta(paso.fecha)),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
            }
            ImagenCashi(
                recurso = if (celebrando) RecursoCashi.CELEBRANDO else RecursoCashi.FELIZ,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 1.dp)
                    .size(58.dp)
                    .graphicsLayer { translationY = salto.value * density },
            )
        }
    }
}

@Composable
private fun FinalDelCamino(
    esquema: EsquemaCamino,
    x: Dp,
    nombreMeta: String,
    ancho: Dp,
    animar: Boolean,
    retrasoMs: Int,
) {
    val y = esquema.yMeta
    val tamano = if (esquema.lograda) MONEDA_LOGRADA else MONEDA_META
    val escala = remember { Animatable(if (animar) 0.2f else 1f) }
    LaunchedEffect(Unit) {
        if (animar) {
            delay(retrasoMs.toLong())
            escala.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
        }
    }
    RayosMeta(girar = esquema.lograda, modifier = Modifier.offset(x - 70.dp, y - 70.dp))
    MonedaPaso(
        estilo = EstiloMoneda.META,
        tamano = tamano,
        modifier = Modifier
            .offset(x - tamano / 2 - MARGEN_MONEDA, y - tamano / 2 - MARGEN_MONEDA)
            .graphicsLayer {
                scaleX = escala.value
                scaleY = escala.value
            },
    ) {
        if (esquema.lograda) {
            ImagenCashi(
                recurso = RecursoCashi.META_LOGRADA,
                contentDescription = stringResource(R.string.camino_tu_meta),
                modifier = Modifier.size(tamano - 10.dp).clip(CircleShape),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = stringResource(R.string.camino_tu_meta),
                tint = Naranja,
                modifier = Modifier.size(50.dp),
            )
        }
    }
    val paso = esquema.pasoFinal
    Surface(
        shape = FORMA_FILA,
        color = CremaTarjeta,
        border = BorderStroke(1.5.dp, BordeCampo),
        modifier = Modifier
            .offset(x - ancho / 2, y + tamano / 2 + 8.dp)
            .width(ancho),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = Espacio.s, vertical = 6.dp),
        ) {
            Text(
                text = nombreMeta,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = when {
                    esquema.lograda -> stringResource(R.string.camino_llegaste)
                    paso != null -> stringResource(
                        R.string.camino_ultimo,
                        Formato.montoEntero(paso.monto),
                        Formato.fechaCorta(paso.fecha),
                    )
                    else -> stringResource(R.string.camino_cada_paso)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RayosMeta(girar: Boolean, modifier: Modifier) {
    val transicion = rememberInfiniteTransition(label = "rayos")
    val giro by transicion.animateFloat(
        initialValue = 0f,
        targetValue = if (girar) 360f else 0f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "giro",
    )
    Canvas(modifier = modifier.size(140.dp).graphicsLayer { rotationZ = giro }) {
        val color = Naranja.copy(alpha = 0.32f)
        val pincel = Brush.radialGradient(
            0f to color,
            0.4f to color,
            0.72f to color.copy(alpha = 0f),
            center = center,
            radius = size.minDimension / 2,
        )
        repeat(12) { drawArc(pincel, it * 30f, 12f, useCenter = true) }
    }
}

/** Lo que tendrá (o tenía) al llegar a ese paso. */
@Composable
private fun GloboPaso(paso: PasoAhorro, y: Dp, ancho: Dp) {
    Box(modifier = Modifier.offset(X_FILA, y).width(ancho).zIndex(5f)) {
        Box(
            modifier = Modifier
                .offset(24.dp, (-5).dp)
                .size(12.dp)
                .graphicsLayer { rotationZ = 45f }
                .background(VerdeOscuro),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VerdeOscuro, RoundedCornerShape(16.dp))
                .padding(horizontal = 13.dp, vertical = 9.dp),
        ) {
            Text(
                text = stringResource(
                    if (paso.estado == EstadoPaso.COMPLETADO) R.string.camino_llevabas else R.string.camino_al_llegar,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Crema,
            )
            Text(
                text = Formato.montoEntero(paso.acumulado),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Crema,
            )
        }
    }
}

/** "+ Bs 240" o "+ 30 XP" que sube y se desvanece. */
@Composable
internal fun ChipSube(texto: String, color: Color, modifier: Modifier = Modifier) {
    val avance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { avance.animateTo(1f, tween(1400, easing = LinearEasing)) }
    Text(
        text = texto,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = Blanco,
        modifier = modifier
            .graphicsLayer {
                val t = avance.value
                translationY = (16f - 40f * t) * density
                alpha = minOf(t * 4f, 1f) * if (t > 0.85f) (1f - t) / 0.15f else 1f
            }
            .background(color, RoundedCornerShape(14.dp))
            .padding(horizontal = 11.dp, vertical = 5.dp),
    )
}

private class PiezaConfeti(val x: Float, val y: Float, val velocidad: Float, val retardo: Float, val indiceColor: Int)

@Composable
internal fun Confeti(foco: Dp, onFin: () -> Unit) {
    val avance = remember { Animatable(0f) }
    val colores = remember { listOf(Naranja, Verde, VerdeClaro, VerdeMedio) }
    val piezas = remember {
        val azar = Random(7)
        List(28) { PiezaConfeti(azar.nextFloat(), azar.nextFloat(), azar.nextFloat(), azar.nextFloat(), azar.nextInt(4)) }
    }
    LaunchedEffect(Unit) {
        avance.animateTo(1f, tween(2400, easing = LinearEasing))
        onFin()
    }
    Canvas(modifier = Modifier.fillMaxSize().zIndex(7f)) {
        val u = 1.dp.toPx()
        val t = avance.value
        piezas.forEach { pieza ->
            val propio = (t * 1.25f - pieza.retardo * 0.25f).coerceIn(0f, 1f)
            if (propio > 0f) {
                val centro = Offset(
                    x = pieza.x * size.width,
                    y = foco.toPx() - 300 * u + pieza.y * 420 * u + propio * 130 * u * (0.6f + pieza.velocidad),
                )
                rotate(propio * 320f, centro) {
                    drawRect(
                        color = colores[pieza.indiceColor],
                        topLeft = centro - Offset(6 * u, 3 * u),
                        size = Size(12 * u, 6 * u),
                        alpha = maxFloat(0f, if (propio > 0.75f) (1f - propio) / 0.25f else 1f),
                    )
                }
            }
        }
    }
}

private fun pasoDeEjemplo(numero: Int, fecha: LocalDate, monto: String, acumulado: String, estado: EstadoPaso, esMeta: Boolean = false) =
    PasoAhorro(numero, fecha, BigDecimal(monto), BigDecimal(acumulado), estado, esMeta)

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA, heightDp = 760)
@Composable
private fun CaminoMetaPreview() = CashinTheme {
    CaminoMeta(
        plan = PlanAhorro(
            estado = EstadoPlan.ALCANZA,
            cadencia = CadenciaPlan.MENSUAL,
            mesesPorPaso = 1,
            ahorroPorPaso = BigDecimal("240.00"),
            pasos = listOf(
                pasoDeEjemplo(1, LocalDate.of(2026, 9, 25), "250", "250", EstadoPaso.COMPLETADO),
                pasoDeEjemplo(2, LocalDate.of(2026, 10, 27), "801", "1051", EstadoPaso.COMPLETADO),
                pasoDeEjemplo(3, LocalDate.of(2026, 11, 30), "240", "1291", EstadoPaso.ACTUAL),
                pasoDeEjemplo(4, LocalDate.of(2026, 12, 31), "240", "1531", EstadoPaso.PENDIENTE),
                pasoDeEjemplo(5, LocalDate.of(2027, 5, 1), "969", "2500", EstadoPaso.PENDIENTE, esMeta = true),
            ),
            fechaEstimada = LocalDate.of(2027, 5, 1),
        ),
        nombreMeta = "Celular nuevo",
        animarEntrada = false,
        onEntradaVista = {},
        confetiAlEntrar = false,
        celebracion = null,
        onCelebracionVista = {},
    )
}
