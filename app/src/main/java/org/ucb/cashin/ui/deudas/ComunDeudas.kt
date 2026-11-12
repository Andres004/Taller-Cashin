package org.ucb.cashin.ui.deudas

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.ucb.cashin.R
import org.ucb.cashin.model.EstadoDeuda
import org.ucb.cashin.model.ResumenDeuda
import org.ucb.cashin.model.TipoDeuda
import org.ucb.cashin.prediccion.AvisoVencimiento
import org.ucb.cashin.prediccion.vencimientoDe
import org.ucb.cashin.ui.components.TarjetaJuego
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.ui.theme.NaranjaRelieve
import org.ucb.cashin.util.Formato
import java.time.LocalDate

/** El texto correcto según quién le debe a quién. */
@StringRes
fun TipoDeuda.texto(@StringRes debo: Int, @StringRes meDeben: Int): Int = if (this == TipoDeuda.DEBO) debo else meDeben

/** Lo que debes va en el rojo de gasto; lo que te deben, en el verde de ingreso. Siempre con texto. */
@Composable
fun TipoDeuda.color(): Color = if (this == TipoDeuda.DEBO) CashinColores.gasto else CashinColores.ingreso

/** Avance de 0 a 1: cuánto se pagó del total. */
fun ResumenDeuda.avance(): Float =
    if (montoTotal.signum() <= 0) 0f else (abonado.toFloat() / montoTotal.toFloat()).coerceIn(0f, 1f)

/**
 * Los errores de la base de datos (migración 009) llegan con un código en el mensaje.
 * Aquí se convierten en un texto amable; null si no es uno de esos.
 */
@StringRes
fun mensajeErrorDeuda(e: Throwable): Int? {
    val texto = generateSequence(e) { it.cause }.mapNotNull { it.message }.joinToString(" ")
    return when {
        "DEUDA_SALDO_NEGATIVO" in texto || "superan el monto" in texto -> R.string.deuda_error_bd_saldo
        "DEUDA_FECHA_INVALIDA" in texto -> R.string.deuda_error_bd_fecha
        "DEUDA_TIPO_CON_MOVIMIENTOS" in texto -> R.string.deuda_error_bd_tipo
        "DEUDA_SOLO_ULTIMO" in texto -> R.string.deuda_error_bd_ultimo
        else -> null
    }
}

/** "Vence en 3 días", "Venció el 5 nov.", "Pagada el 20 nov."... con color de fondo según la urgencia. */
@Composable
fun ChipAviso(deuda: ResumenDeuda, hoy: LocalDate, modifier: Modifier = Modifier) {
    val pagada = deuda.estado == EstadoDeuda.PAGADA
    val vencimiento = vencimientoDe(deuda.fechaLimite, pagada, hoy)
    val (texto, fondo, color) = when {
        pagada -> Triple(
            stringResource(R.string.deudas_aviso_pagada, deuda.fechaCancelacion?.let { Formato.fechaCorta(it) }.orEmpty()),
            CashinColores.ingreso.copy(alpha = 0.12f),
            CashinColores.ingreso,
        )
        vencimiento == null || vencimiento.aviso == AvisoVencimiento.SIN_FECHA -> Triple(
            stringResource(R.string.deudas_aviso_sin_fecha),
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        vencimiento.aviso == AvisoVencimiento.VENCIDA -> Triple(
            stringResource(R.string.deudas_aviso_vencida, Formato.fechaCorta(deuda.fechaLimite!!)),
            CashinColores.gasto.copy(alpha = 0.14f),
            CashinColores.gasto,
        )
        vencimiento.aviso == AvisoVencimiento.VENCE_HOY -> Triple(
            stringResource(R.string.deudas_aviso_hoy),
            MaterialTheme.colorScheme.tertiaryContainer,
            NaranjaRelieve,
        )
        vencimiento.aviso == AvisoVencimiento.VENCE_PRONTO -> Triple(
            pluralStringResource(R.plurals.deudas_aviso_pronto, vencimiento.dias.toInt(), vencimiento.dias.toInt()),
            MaterialTheme.colorScheme.tertiaryContainer,
            NaranjaRelieve,
        )
        else -> Triple(
            stringResource(R.string.deudas_aviso_normal, Formato.fechaCorta(deuda.fechaLimite!!)),
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Surface(color = fondo, shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = Espacio.s, vertical = 2.dp),
        )
    }
}

/** Barra gruesa de avance con el color del tipo de deuda. */
@Composable
fun BarraDeuda(avance: Float, color: Color, modifier: Modifier = Modifier, alto: Int = 10) {
    LinearProgressIndicator(
        progress = { avance },
        color = color,
        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
        modifier = modifier
            .fillMaxWidth()
            .height(alto.dp)
            .clip(RoundedCornerShape(alto.dp)),
    )
}

/**
 * Tarjeta de una deuda en la lista: persona, para qué fue (o cuántos movimientos tiene),
 * lo que falta "de" el total, la barra de avance y el aviso de vencimiento.
 */
@Composable
fun TarjetaDeuda(deuda: ResumenDeuda, hoy: LocalDate, onClick: () -> Unit) {
    val color = deuda.tipo.color()
    val pagada = deuda.estado == EstadoDeuda.PAGADA
    TarjetaJuego(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deuda.contraparte,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtituloTarjeta(deuda),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formato.montoRedondo(if (pagada) deuda.montoTotal else deuda.pendiente),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (pagada) MaterialTheme.colorScheme.onSurfaceVariant else color,
                    textAlign = TextAlign.End,
                )
                if (!pagada) {
                    Text(
                        text = stringResource(R.string.deudas_de_total, Formato.montoRedondo(deuda.montoTotal)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        BarraDeuda(deuda.avance(), color, Modifier.padding(vertical = Espacio.s))
        ChipAviso(deuda, hoy)
    }
}

/** "Préstamo para la laptop" o, si tiene varios movimientos, "2 préstamos · 1 devolución". */
@Composable
private fun subtituloTarjeta(deuda: ResumenDeuda): String {
    if (deuda.cantidadAumentos == 0 && deuda.cantidadAbonos == 0 || deuda.cantidadAumentos == 0 && !deuda.descripcion.isNullOrBlank()) {
        return deuda.descripcion?.takeIf { it.isNotBlank() } ?: ""
    }
    val prestamos = deuda.cantidadAumentos + 1
    val partes = mutableListOf(pluralStringResource(R.plurals.deudas_resumen_aumentos, prestamos, prestamos))
    if (deuda.cantidadAbonos > 0) {
        partes += pluralStringResource(
            deuda.tipo.texto(R.plurals.deudas_resumen_abonos_debo, R.plurals.deudas_resumen_abonos_me_deben),
            deuda.cantidadAbonos,
            deuda.cantidadAbonos,
        )
    }
    return partes.joinToString(" · ")
}

/**
 * Campo de fecha: muestra "Hoy", "Ayer" o la fecha, y abre el calendario al tocarlo.
 * Con [onQuitar] aparece una ✕ para dejarlo vacío (solo la fecha límite es opcional).
 */
@Composable
fun CampoFechaDeuda(
    etiqueta: String,
    fecha: LocalDate?,
    textoVacio: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    onQuitar: (() -> Unit)? = null,
    descripcionQuitar: String? = null,
) {
    Column(modifier = modifier) {
        // Igual que la etiqueta de CashinCampoTexto, para que los campos queden alineados.
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
        )
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacio.s),
                modifier = Modifier.padding(start = Espacio.m, end = Espacio.xs).heightIn(min = 52.dp),
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = fecha?.let { Formato.encabezadoDia(it) } ?: textoVacio,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (fecha == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                if (fecha != null && onQuitar != null) {
                    androidx.compose.material3.IconButton(onClick = onQuitar) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Outlined.Close,
                            contentDescription = descripcionQuitar,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = Espacio.xs),
            )
        }
    }
}

/**
 * Calendario que solo deja elegir entre [desde] y [hasta] (null = sin tope).
 * Trabaja con milisegundos en UTC a la medianoche, por eso se convierte con ZoneOffset.UTC.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CalendarioDeuda(
    fecha: LocalDate?,
    desde: LocalDate?,
    hasta: LocalDate?,
    onConfirmar: (LocalDate) -> Unit,
    onCancelar: () -> Unit,
) {
    val minimo = desde?.aMilisUtc()
    val maximo = hasta?.aMilisUtc()
    val calendario = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = fecha?.aMilisUtc(),
        initialDisplayedMonthMillis = (fecha ?: hasta ?: desde ?: Formato.hoy()).aMilisUtc(),
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                (minimo == null || utcTimeMillis >= minimo) && (maximo == null || utcTimeMillis <= maximo)
        },
    )
    val elegida = calendario.selectedDateMillis
    val colores = androidx.compose.material3.DatePickerDefaults.colors(
        containerColor = MaterialTheme.colorScheme.background,
        selectedDayContainerColor = MaterialTheme.colorScheme.secondary,
        selectedDayContentColor = MaterialTheme.colorScheme.onSecondary,
        todayDateBorderColor = MaterialTheme.colorScheme.secondary,
        todayContentColor = MaterialTheme.colorScheme.secondary,
    )
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onCancelar,
        colors = colores,
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    if (elegida != null) {
                        onConfirmar(java.time.Instant.ofEpochMilli(elegida).atZone(java.time.ZoneOffset.UTC).toLocalDate())
                    }
                },
                enabled = elegida != null,
            ) { Text(stringResource(R.string.accion_aceptar)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onCancelar) { Text(stringResource(R.string.accion_cancelar)) }
        },
    ) {
        androidx.compose.material3.DatePicker(state = calendario, showModeToggle = false, colors = colores)
    }
}

private fun LocalDate.aMilisUtc(): Long = atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
