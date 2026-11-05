package org.ucb.cashin.ui.desafios

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.model.Desafio
import org.ucb.cashin.model.EstadoDesafio
import org.ucb.cashin.model.PeriodoDesafio
import org.ucb.cashin.model.ProgresoDesafio
import org.ucb.cashin.model.ReglaDesafio
import org.ucb.cashin.prediccion.finDelPeriodo
import org.ucb.cashin.ui.components.AvatarBurbuja
import org.ucb.cashin.ui.components.AvatarImagen
import org.ucb.cashin.ui.components.BarraProgreso
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinTarjeta
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.Emocion
import org.ucb.cashin.ui.components.EstadoCargando
import org.ucb.cashin.ui.components.EstadoError
import org.ucb.cashin.ui.components.VarianteBoton
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.util.Formato
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.temporal.ChronoUnit

/** Desde el 80 % del límite la barra se pinta de rojo. */
private const val UMBRAL_RIESGO = 0.8f

/**
 * HU-22 / HU-23: los desafíos llegan solos (3 cada lunes y 1 cada día 1).
 * Arriba el aviso de desafíos nuevos y el resumen (cumplidos, XP ganado, seguidos); luego
 * "Desafíos de la semana" y "Desafío del mes" con su progreso y una pista; y el historial.
 * Cuando la evaluación cierra un desafío se muestra la animación de [ResultadoDesafioDialogo].
 *
 * Se abre sola (con [onAtras]) desde Inicio y Perfil, o como pestaña dentro de Metas (sin barra, [onAtras] = null).
 */
@Composable
fun DesafiosScreen(
    onAtras: (() -> Unit)?,
    viewModel: DesafiosViewModel = viewModel(factory = DesafiosViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Cada vez que se entra: se cierran los vencidos y, si empezó la semana o el mes, llegan los nuevos.
    LaunchedEffect(Unit) { viewModel.cargar() }

    DesafiosContenido(
        estado = estado,
        onAtras = onAtras,
        onReintentar = viewModel::cargar,
        onDescartar = viewModel::descartar,
        onResultadoVisto = viewModel::resultadoVisto,
        onNuevosVistos = viewModel::nuevosVistos,
    )
}

/** Solo dibuja. Estados: cargando, error, sin desafíos todavía o con datos. */
@Composable
fun DesafiosContenido(
    estado: DesafiosUiState,
    onAtras: (() -> Unit)?,
    onReintentar: () -> Unit,
    onDescartar: (String) -> Unit,
    onResultadoVisto: () -> Unit,
    onNuevosVistos: () -> Unit,
) {
    var porDescartar by remember { mutableStateOf<Desafio?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (onAtras != null) CashinTopBar(titulo = stringResource(R.string.perfil_desafios), onAtras = onAtras)

        when {
            estado.cargando -> EstadoCargando()

            estado.error != null -> EstadoError(mensaje = estado.error.mensaje(), onReintentar = onReintentar)

            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = Espacio.pantalla, vertical = Espacio.s),
                verticalArrangement = Arrangement.spacedBy(Espacio.m),
            ) {
                if (estado.nuevos.isNotEmpty()) {
                    item(key = "nuevos") { AvisoNuevosDesafios(onCerrar = onNuevosVistos) }
                }

                if (estado.historial.isNotEmpty()) {
                    item(key = "resumen") { TarjetaResumen(estado) }
                }

                estado.errorAccion?.let { error ->
                    item(key = "error-accion") {
                        Text(text = error.mensaje(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                }

                if (estado.activos.isEmpty()) {
                    item(key = "vacio") {
                        AvatarBurbuja(mensaje = stringResource(R.string.desafios_vacio), emocion = Emocion.FELIZ)
                    }
                }

                seccionActivos(
                    periodo = PeriodoDesafio.SEMANAL,
                    desafios = estado.semanales,
                    estado = estado,
                    onDescartar = { porDescartar = it },
                )
                seccionActivos(
                    periodo = PeriodoDesafio.MENSUAL,
                    desafios = estado.mensuales,
                    estado = estado,
                    onDescartar = { porDescartar = it },
                )

                if (estado.historial.isNotEmpty()) {
                    item(key = "titulo-historial") { TituloSeccion(stringResource(R.string.desafios_historial)) }
                    items(estado.historial, key = { "historial-${it.id}" }) { TarjetaHistorial(it) }
                }
            }
        }
    }

    // HU-23: animación del primer desafío recién cerrado; al cerrarla aparece el siguiente.
    estado.resultadosPorMostrar.firstOrNull()?.let { desafio ->
        ResultadoDesafioDialogo(desafio = desafio, onCerrar = onResultadoVisto)
    }

    porDescartar?.let { desafio ->
        AlertDialog(
            onDismissRequest = { porDescartar = null },
            title = { Text(stringResource(R.string.desafios_descartar_titulo)) },
            text = { Text(stringResource(R.string.desafios_descartar_mensaje, desafio.titulo)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        porDescartar = null
                        onDescartar(desafio.id)
                    },
                ) {
                    Text(stringResource(R.string.desafios_descartar), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { porDescartar = null }) {
                    Text(stringResource(R.string.accion_cancelar))
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }
}

/** "Desafíos de la semana · Se renuevan el lunes 16 nov" o "Desafío del mes · Se renueva el 1 dic", con sus tarjetas. */
private fun LazyListScope.seccionActivos(
    periodo: PeriodoDesafio,
    desafios: List<Desafio>,
    estado: DesafiosUiState,
    onDescartar: (Desafio) -> Unit,
) {
    if (desafios.isEmpty()) return
    item(key = "titulo-$periodo") {
        val proximo = finDelPeriodo(periodo, Formato.hoy()).plusDays(1)
        Column {
            TituloSeccion(
                stringResource(if (periodo == PeriodoDesafio.SEMANAL) R.string.desafios_de_la_semana else R.string.desafios_del_mes),
            )
            Text(
                text = stringResource(
                    if (periodo == PeriodoDesafio.SEMANAL) R.string.desafios_renuevan_lunes else R.string.desafios_renuevan_mes,
                    Formato.fechaCorta(proximo),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    items(desafios, key = { it.id }) { desafio ->
        TarjetaActivo(
            desafio = desafio,
            progreso = estado.progreso[desafio.id],
            nuevo = desafio.id in estado.nuevos,
            descartando = estado.descartando == desafio.id,
            onDescartar = { onDescartar(desafio) },
        )
    }
}

@Composable
private fun TituloSeccion(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = Espacio.s),
    )
}

/**
 * "¡Nuevos desafíos disponibles!": Cashi celebra (late suave) cuando llegan los retos
 * de la semana o del mes. Se cierra con la X.
 */
@Composable
fun AvisoNuevosDesafios(onCerrar: (() -> Unit)?, onVer: (() -> Unit)? = null) {
    val latido by rememberInfiniteTransition(label = "latido").animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 700), RepeatMode.Reverse),
        label = "latido",
    )
    CashinTarjeta(color = MaterialTheme.colorScheme.tertiaryContainer, onClick = onVer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarImagen(
                emocion = Emocion.CELEBRANDO,
                tamano = 64.dp,
                modifier = Modifier.graphicsLayer {
                    scaleX = latido
                    scaleY = latido
                },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Espacio.s),
            ) {
                Text(
                    text = stringResource(R.string.desafios_nuevos_titulo),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = stringResource(R.string.desafios_nuevos_mensaje),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                if (onVer != null) {
                    Text(
                        text = stringResource(R.string.desafios_nuevos_ver),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = Espacio.xs),
                    )
                }
            }
            if (onCerrar != null) {
                IconButton(onClick = onCerrar) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = stringResource(R.string.accion_cerrar))
                }
            }
        }
    }
}

/** 🏆 cumplidos · ⭐ XP ganado · 🔥 cumplidos seguidos. */
@Composable
private fun TarjetaResumen(estado: DesafiosUiState) {
    CashinTarjeta(color = MaterialTheme.colorScheme.tertiaryContainer) {
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Dato(icono = Icons.Outlined.EmojiEvents, valor = estado.cumplidos.toString(), etiqueta = stringResource(R.string.desafios_dato_cumplidos))
            Dato(icono = Icons.Outlined.Star, valor = stringResource(R.string.desafios_xp, estado.xpGanado), etiqueta = stringResource(R.string.desafios_dato_xp))
            Dato(
                icono = Icons.Outlined.LocalFireDepartment,
                valor = estado.cumplidosSeguidos.toString(),
                etiqueta = stringResource(R.string.desafios_dato_seguidos),
            )
        }
    }
}

@Composable
private fun Dato(icono: ImageVector, valor: String, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icono, contentDescription = null, tint = CashinColores.xp, modifier = Modifier.size(24.dp))
        Text(
            text = valor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

/** "Semanal · 2 nov – 8 nov". */
@Composable
fun textoPeriodo(desafio: Desafio): String = stringResource(
    R.string.desafios_periodo_rango,
    stringResource(if (desafio.periodo == PeriodoDesafio.SEMANAL) R.string.desafios_semanal else R.string.desafios_mensual),
    Formato.fechaCorta(desafio.fechaInicio),
    Formato.fechaCorta(desafio.fechaFin),
)

/** Lo logrado en un reto que cuenta algo (días, movimientos, días sin gasto o ahorros). */
private fun logrado(desafio: Desafio, progreso: ProgresoDesafio?): Int = when (desafio.regla) {
    ReglaDesafio.DIAS_REGISTRO -> progreso?.diasConRegistro ?: 0
    ReglaDesafio.CANTIDAD_REGISTROS -> progreso?.cantidadRegistros ?: 0
    ReglaDesafio.DIAS_SIN_GASTO -> progreso?.diasSinGasto ?: 0
    ReglaDesafio.AHORRO_META -> progreso?.ahorros ?: 0
    ReglaDesafio.LIMITE_CATEGORIA, ReglaDesafio.LIMITE_TOTAL -> 0
}

/**
 * Barra (animada) y "Bs 120,00 de Bs 175,00" (límites) o "3 de 5 días anotando", "4 de 10 movimientos"…
 * En rojo desde el 80 % del límite. También la usa la tarjeta "Tus desafíos" de Inicio.
 */
@Composable
fun ProgresoDeDesafio(desafio: Desafio, progreso: ProgresoDesafio?) {
    val limite = desafio.montoLimite
    val gastado = progreso?.gastado ?: BigDecimal.ZERO
    val hecho = logrado(desafio, progreso)
    val objetivo = desafio.diasObjetivo ?: 0

    val avance = when {
        limite != null && limite.signum() > 0 -> gastado.toFloat() / limite.toFloat()
        objetivo > 0 -> hecho.toFloat() / objetivo
        else -> 0f
    }
    val enRiesgo = limite != null && avance >= UMBRAL_RIESGO
    val avanceAnimado by animateFloatAsState(targetValue = avance, animationSpec = tween(durationMillis = 900), label = "avance")

    BarraProgreso(
        progreso = avanceAnimado,
        color = if (enRiesgo) CashinColores.gasto else MaterialTheme.colorScheme.primary,
    )
    Text(
        text = when (desafio.regla) {
            ReglaDesafio.LIMITE_CATEGORIA, ReglaDesafio.LIMITE_TOTAL ->
                stringResource(R.string.desafios_progreso_monto, Formato.monto(gastado), Formato.monto(limite ?: BigDecimal.ZERO))
            ReglaDesafio.DIAS_REGISTRO -> stringResource(R.string.desafios_progreso_dias, hecho, objetivo)
            ReglaDesafio.CANTIDAD_REGISTROS -> stringResource(R.string.desafios_progreso_movimientos, hecho, objetivo)
            ReglaDesafio.DIAS_SIN_GASTO -> stringResource(R.string.desafios_progreso_sin_gasto, hecho, objetivo)
            ReglaDesafio.AHORRO_META -> stringResource(R.string.desafios_progreso_ahorros, hecho, objetivo)
        },
        style = MaterialTheme.typography.labelLarge,
        color = if (enRiesgo) CashinColores.gasto else MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = Espacio.s),
    )
}

/**
 * Pista para cumplir el reto:
 * - Límites: "Te quedan Bs 55,00: unos Bs 18,33 por día." o "¡Cuidado! Solo te quedan Bs 20,00."
 * - Los que cuentan algo: cuánto falta, o que ya está logrado / ya no alcanzan los días.
 */
@Composable
private fun PistaDelReto(desafio: Desafio, progreso: ProgresoDesafio?, diasQueQuedan: Int) {
    val limite = desafio.montoLimite
    val (texto, alerta) = if (limite != null) {
        val gastado = progreso?.gastado ?: BigDecimal.ZERO
        val queda = limite - gastado
        val avance = if (limite.signum() > 0) gastado.toFloat() / limite.toFloat() else 0f
        when {
            queda.signum() <= 0 -> stringResource(R.string.desafios_pista_sin_margen) to true
            avance >= UMBRAL_RIESGO -> stringResource(R.string.desafios_pista_cuidado, Formato.monto(queda)) to true
            else -> {
                val porDia = queda.divide(BigDecimal(diasQueQuedan.coerceAtLeast(1)), 2, RoundingMode.DOWN)
                stringResource(R.string.desafios_pista_por_dia, Formato.monto(queda), Formato.monto(porDia)) to false
            }
        }
    } else {
        val faltan = (desafio.diasObjetivo ?: 0) - logrado(desafio, progreso)
        // Solo días (anotar o sin gasto) dependen de cuántos días quedan; movimientos y ahorros no.
        val dependeDeLosDias = desafio.regla == ReglaDesafio.DIAS_REGISTRO || desafio.regla == ReglaDesafio.DIAS_SIN_GASTO
        when {
            faltan <= 0 -> stringResource(R.string.desafios_pista_listo) to false
            dependeDeLosDias && faltan > diasQueQuedan -> stringResource(R.string.desafios_pista_no_alcanza) to true
            else -> pluralStringResource(
                when (desafio.regla) {
                    ReglaDesafio.CANTIDAD_REGISTROS -> R.plurals.desafios_pista_movimientos
                    ReglaDesafio.DIAS_SIN_GASTO -> R.plurals.desafios_pista_sin_gasto
                    ReglaDesafio.AHORRO_META -> R.plurals.desafios_pista_ahorros
                    else -> R.plurals.desafios_pista_dias
                },
                faltan,
                faltan,
            ) to false
        }
    }
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (alerta) CashinColores.gasto else MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = Espacio.xs),
    )
}

/** Un desafío en curso: periodo, NUEVO, título, descripción, días que quedan, progreso, pista y "Descartar". */
@Composable
private fun TarjetaActivo(
    desafio: Desafio,
    progreso: ProgresoDesafio?,
    nuevo: Boolean,
    descartando: Boolean,
    onDescartar: () -> Unit,
) {
    val restantes = ChronoUnit.DAYS.between(Formato.hoy(), desafio.fechaFin).toInt().coerceAtLeast(0)

    CashinTarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = textoPeriodo(desafio),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (nuevo) EtiquetaNuevo()
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Espacio.xs)) {
            Text(
                text = desafio.titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.desafios_xp, desafio.xpRecompensa),
                style = MaterialTheme.typography.labelLarge,
                color = CashinColores.xp,
            )
        }
        desafio.descripcion?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = Espacio.xs),
            )
        }
        Text(
            text = if (restantes == 0) {
                stringResource(R.string.desafios_termina_hoy)
            } else {
                pluralStringResource(R.plurals.desafios_quedan_dias, restantes, restantes)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = Espacio.xs, bottom = Espacio.s),
        )

        ProgresoDeDesafio(desafio = desafio, progreso = progreso)
        // Cuenta hoy también: "quedan 3 días" + hoy = 4 días para gastar o anotar.
        PistaDelReto(desafio = desafio, progreso = progreso, diasQueQuedan = restantes + 1)

        CashinBoton(
            texto = stringResource(R.string.desafios_descartar),
            onClick = onDescartar,
            variante = VarianteBoton.TEXTO,
            cargando = descartando,
            modifier = Modifier.padding(top = Espacio.xs),
        )
    }
}

/** Etiqueta naranja "NUEVO" en los desafíos que acaban de llegar. */
@Composable
private fun EtiquetaNuevo() {
    Text(
        text = stringResource(R.string.desafios_nuevo),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onTertiary,
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(CashinColores.xp)
            .padding(horizontal = Espacio.s, vertical = Espacio.xs),
    )
}

/**
 * Un desafío terminado.
 * - Cumplido: trofeo, "¡Cumplido!" y el XP ganado.
 * - Fallido: carita triste y por qué no salió.
 */
@Composable
private fun TarjetaHistorial(desafio: Desafio) {
    val cumplido = desafio.estado == EstadoDesafio.CUMPLIDO
    val acento = if (cumplido) CashinColores.ingreso else CashinColores.gasto

    CashinTarjeta(color = acento.copy(alpha = 0.08f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (cumplido) Icons.Outlined.EmojiEvents else Icons.Outlined.SentimentDissatisfied,
                contentDescription = null,
                tint = if (cumplido) CashinColores.xp else CashinColores.gasto,
                modifier = Modifier
                    .padding(end = Espacio.s)
                    .size(32.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = desafio.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = textoPeriodo(desafio),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (cumplido) {
                    stringResource(R.string.desafios_xp, desafio.xpRecompensa)
                } else {
                    stringResource(R.string.desafios_estado_fallido)
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (cumplido) CashinColores.xp else CashinColores.gasto,
            )
        }
        Text(
            text = if (cumplido) stringResource(R.string.desafios_estado_cumplido) else motivoFallido(desafio),
            style = MaterialTheme.typography.bodyMedium,
            color = acento,
            modifier = Modifier.padding(top = Espacio.s),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA, heightDp = 1500)
@Composable
private fun DesafiosPreview() = CashinTheme {
    val hoy = Formato.hoy()
    val lunes = hoy.with(java.time.DayOfWeek.MONDAY)
    val semanal = Desafio(
        id = "d1", usuarioId = "u", titulo = "Alimentación bajo control",
        descripcion = "Gasta como máximo Bs 175,00 en Alimentación esta semana. Sueles gastar unos Bs 206,96.",
        periodo = PeriodoDesafio.SEMANAL, regla = ReglaDesafio.LIMITE_CATEGORIA,
        categoriaId = "c1", montoLimite = BigDecimal("175.00"),
        fechaInicio = lunes, fechaFin = lunes.plusDays(6),
    )
    DesafiosContenido(
        estado = DesafiosUiState(
            cargando = false,
            activos = listOf(
                semanal,
                semanal.copy(
                    id = "d2", titulo = "Anota 10 movimientos", descripcion = "Anota al menos 10 gastos o ingresos esta semana.",
                    regla = ReglaDesafio.CANTIDAD_REGISTROS, categoriaId = null, montoLimite = null, diasObjetivo = 10,
                ),
                semanal.copy(
                    id = "d3", titulo = "2 días sin gastos", descripcion = "Logra 2 días sin anotar ningún gasto esta semana.",
                    regla = ReglaDesafio.DIAS_SIN_GASTO, categoriaId = null, montoLimite = null, diasObjetivo = 2,
                ),
                semanal.copy(
                    id = "d4", titulo = "Anota 20 días", descripcion = "Anota algo al menos 20 días distintos este mes.",
                    periodo = PeriodoDesafio.MENSUAL, regla = ReglaDesafio.DIAS_REGISTRO, categoriaId = null,
                    montoLimite = null, diasObjetivo = 20, xpRecompensa = 150,
                    fechaInicio = hoy.withDayOfMonth(1), fechaFin = hoy.withDayOfMonth(hoy.lengthOfMonth()),
                ),
            ),
            progreso = mapOf(
                "d1" to ProgresoDesafio("d1", BigDecimal("150.00"), 4),
                "d2" to ProgresoDesafio("d2", BigDecimal.ZERO, 3, cantidadRegistros = 6),
                "d3" to ProgresoDesafio("d3", BigDecimal.ZERO, 3, diasSinGasto = 1),
                "d4" to ProgresoDesafio("d4", BigDecimal.ZERO, 5),
            ),
            nuevos = setOf("d1", "d2", "d3", "d4"),
            historial = listOf(
                semanal.copy(id = "h1", estado = EstadoDesafio.CUMPLIDO, fechaInicio = lunes.minusDays(7), fechaFin = lunes.minusDays(1)),
            ),
        ),
        onAtras = {},
        onReintentar = {},
        onDescartar = {},
        onResultadoVisto = {},
        onNuevosVistos = {},
    )
}
