package org.ucb.cashin.ui.reportes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import org.ucb.cashin.R
import org.ucb.cashin.model.GastoCategoriaMes
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.prediccion.DivisionGastoMes
import org.ucb.cashin.prediccion.FraseReporte
import org.ucb.cashin.prediccion.MesResumen
import org.ucb.cashin.prediccion.PorcionCategoria
import org.ucb.cashin.prediccion.ReporteCategorias
import org.ucb.cashin.prediccion.Tendencia
import org.ucb.cashin.prediccion.TipoFraseReporte
import org.ucb.cashin.prediccion.fraseGastos
import org.ucb.cashin.prediccion.fraseIngresos
import org.ucb.cashin.prediccion.fraseMeses
import org.ucb.cashin.prediccion.reportePorCategoria
import org.ucb.cashin.ui.components.AvatarBurbuja
import org.ucb.cashin.ui.components.BarraMes
import org.ucb.cashin.ui.components.Emocion
import org.ucb.cashin.ui.components.EstadoError
import org.ucb.cashin.ui.components.EstadoVacio
import org.ucb.cashin.ui.components.GraficoBarras
import org.ucb.cashin.ui.components.GraficoDona
import org.ucb.cashin.ui.components.PorcionDona
import org.ucb.cashin.ui.components.RecursoCashi
import org.ucb.cashin.ui.components.TarjetaJuego
import org.ucb.cashin.ui.components.colorCategoria
import org.ucb.cashin.ui.components.iconoCategoria
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.util.Formato
import java.math.BigDecimal
import java.time.YearMonth

/**
 * HU-14: pestaña Reportes de Historial. Tres vistas (Gastos | Ingresos | Meses) y un selector de mes.
 * [contarMovimientos] y [onVerMovimientos] vienen de Historial: así "Ver N movimientos" abre la
 * pestaña Movimientos ya filtrada, sin otra pantalla ni otra ruta.
 */
@Composable
fun ReportesScreen(
    onVerMovimientos: (tipo: TipoRegistro, mes: YearMonth, categoriaId: String) -> Unit,
    contarMovimientos: (tipo: TipoRegistro, mes: YearMonth, categoriaId: String) -> Int,
    onAnotar: () -> Unit,
    viewModel: ReportesViewModel = viewModel(factory = ReportesViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Se recarga cada vez que se entra a la pestaña (después de anotar, editar o eliminar).
    LaunchedEffect(Unit) { viewModel.cargar() }

    ReportesContenido(
        estado = estado,
        onReintentar = viewModel::cargar,
        onVistaCambio = viewModel::onVistaCambio,
        onMesAnterior = viewModel::onMesAnterior,
        onMesSiguiente = viewModel::onMesSiguiente,
        onCategoriaTocada = viewModel::onCategoriaTocada,
        onVerMovimientos = onVerMovimientos,
        contarMovimientos = contarMovimientos,
        onAnotar = onAnotar,
    )
}

/** Solo dibuja. Estados: cargando (esqueleto), error, mes vacío y datos. */
@Composable
fun ReportesContenido(
    estado: ReportesUiState,
    onReintentar: () -> Unit,
    onVistaCambio: (VistaReporte) -> Unit,
    onMesAnterior: () -> Unit,
    onMesSiguiente: () -> Unit,
    onCategoriaTocada: (categoriaId: String) -> Unit,
    onVerMovimientos: (tipo: TipoRegistro, mes: YearMonth, categoriaId: String) -> Unit,
    contarMovimientos: (tipo: TipoRegistro, mes: YearMonth, categoriaId: String) -> Int,
    onAnotar: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SelectorVista(
            vista = estado.vista,
            onVistaCambio = onVistaCambio,
            modifier = Modifier.padding(start = Espacio.pantalla, end = Espacio.pantalla, top = Espacio.m),
        )

        if (estado.error != null) {
            EstadoError(mensaje = estado.error.mensaje(), onReintentar = onReintentar)
            return@Column
        }

        SelectorMes(
            mes = estado.mes,
            puedeRetroceder = estado.puedeRetroceder && !estado.cargando,
            puedeAvanzar = estado.puedeAvanzar && !estado.cargando,
            onMesAnterior = onMesAnterior,
            onMesSiguiente = onMesSiguiente,
            modifier = Modifier.padding(horizontal = Espacio.m),
        )

        if (estado.cargando) {
            EsqueletoReporte()
            return@Column
        }

        val reporte = estado.reporteVisible
        when {
            reporte != null && reporte.vacio -> VacioCategorias(
                tipo = estado.tipoVisible,
                mes = estado.mes,
                esMesActual = estado.mes == estado.mesActual,
                onAnotar = onAnotar,
                modifier = Modifier.weight(1f),
            )

            reporte != null -> ContenidoCategorias(
                reporte = reporte,
                tipo = estado.tipoVisible,
                frase = if (estado.vista == VistaReporte.GASTOS) estado.fraseGastos else estado.fraseIngresos,
                seleccionadaId = estado.seleccionadaId,
                onCategoriaTocada = onCategoriaTocada,
                onVerMovimientos = onVerMovimientos,
                contarMovimientos = contarMovimientos,
            )

            estado.meses.isEmpty() -> EstadoVacio(
                titulo = stringResource(R.string.reportes_meses_vacio_titulo),
                mensaje = stringResource(R.string.reportes_meses_vacio_mensaje),
                recurso = RecursoCashi.PENSANDO,
                textoAccion = stringResource(R.string.reportes_vacio_accion),
                onAccion = onAnotar,
                modifier = Modifier.weight(1f),
            )

            else -> ContenidoMeses(
                meses = estado.meses,
                mes = estado.mes,
                division = estado.division,
                frase = estado.fraseMeses,
            )
        }
    }
}

// ------------------------------------------------------------------ Selectores

/** "Gastos | Ingresos | Meses" como un control segmentado. */
@Composable
private fun SelectorVista(
    vista: VistaReporte,
    onVistaCambio: (VistaReporte) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(modifier = Modifier.padding(3.dp)) {
            VistaReporte.entries.forEach { opcion ->
                val elegida = opcion == vista
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (elegida) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .selectable(selected = elegida, role = Role.Tab, onClick = { onVistaCambio(opcion) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(opcion.texto()),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (elegida) FontWeight.Bold else FontWeight.Medium,
                        color = if (elegida) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** ‹ Noviembre 2026 › — no deja pasar del mes actual ni ir antes de la primera anotación. */
@Composable
private fun SelectorMes(
    mes: YearMonth,
    puedeRetroceder: Boolean,
    puedeAvanzar: Boolean,
    onMesAnterior: () -> Unit,
    onMesSiguiente: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMesAnterior, enabled = puedeRetroceder) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.reportes_mes_anterior))
        }
        Text(
            text = Formato.mes(mes.atDay(1)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onMesSiguiente, enabled = puedeAvanzar) {
            Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.reportes_mes_siguiente))
        }
    }
}

// ------------------------------------------------------------------ Gastos e Ingresos

/** La dona sola y al centro; debajo, la lista en una tarjeta; al final, Cashi con la conclusión. */
@Composable
private fun ContenidoCategorias(
    reporte: ReporteCategorias,
    tipo: TipoRegistro,
    frase: FraseReporte?,
    seleccionadaId: String?,
    onCategoriaTocada: (categoriaId: String) -> Unit,
    onVerMovimientos: (tipo: TipoRegistro, mes: YearMonth, categoriaId: String) -> Unit,
    contarMovimientos: (tipo: TipoRegistro, mes: YearMonth, categoriaId: String) -> Int,
) {
    val respaldo = MaterialTheme.colorScheme.primary
    val seleccionada = reporte.porciones.firstOrNull { it.categoriaId == seleccionadaId }
    // Cambiar de mes o de vista vuelve a dibujar la dona y a mostrar la lista.
    val clave = tipo to reporte.mes
    val esGasto = tipo == TipoRegistro.GASTO

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Espacio.pantalla, vertical = Espacio.s),
        verticalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        GraficoDona(
            porciones = reporte.porciones.map { PorcionDona(it.categoriaId, it.total.toFloat(), colorCategoria(it.color, respaldo)) },
            seleccionadaId = seleccionadaId,
            onPorcionTocada = onCategoriaTocada,
            descripcion = stringResource(
                if (esGasto) R.string.reportes_dona_gastos else R.string.reportes_dona_ingresos,
                Formato.mes(reporte.mes.atDay(1)),
            ),
            claveAnimacion = clave,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) { progreso ->
            if (seleccionada != null) {
                CentroCategoria(seleccionada, esGasto)
            } else {
                CentroTotal(reporte, esGasto, progreso)
            }
        }

        if (seleccionada != null) {
            AccionVerMovimientos(
                porcion = seleccionada,
                cantidad = contarMovimientos(tipo, reporte.mes, seleccionada.categoriaId),
                onClick = { onVerMovimientos(tipo, reporte.mes, seleccionada.categoriaId) },
            )
        }

        TarjetaJuego(conRelleno = false) {
            Column(modifier = Modifier.padding(horizontal = Espacio.s, vertical = Espacio.xs)) {
                reporte.porciones.forEachIndexed { i, porcion ->
                    FilaLeyenda(
                        porcion = porcion,
                        color = colorCategoria(porcion.color, respaldo),
                        esGasto = esGasto,
                        elegida = porcion.categoriaId == seleccionadaId,
                        indice = i,
                        clave = clave,
                        onClick = { onCategoriaTocada(porcion.categoriaId) },
                    )
                }
            }
        }

        if (frase != null) {
            val (texto, recurso) = textoFrase(frase, reporte, reporte.mes)
            AvatarBurbuja(mensaje = texto, recurso = recurso, emocion = Emocion.FELIZ, tamanoAvatar = 88.dp)
        }
    }
}

/** Centro sin nada elegido: "Gastaste / Bs 2.350 / ▲ Bs 138 vs. octubre". El total sube con la dona. */
@Composable
private fun CentroTotal(reporte: ReporteCategorias, esGasto: Boolean, progreso: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(150.dp)) {
        Text(
            text = stringResource(if (esGasto) R.string.reportes_gastaste else R.string.reportes_recibiste),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = Formato.montoRedondo(reporte.total.multiply(BigDecimal(progreso.toDouble()))),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )
        if (reporte.totalAnterior.signum() > 0) {
            val anterior = mesEnMinuscula(reporte.mes.minusMonths(1))
            val monto = Formato.montoRedondo(reporte.diferencia.abs())
            val (texto, color) = when (reporte.tendencia) {
                Tendencia.SUBE -> stringResource(R.string.reportes_centro_sube, monto, anterior) to colorCambio(Tendencia.SUBE, esGasto)
                Tendencia.BAJA -> stringResource(R.string.reportes_centro_baja, monto, anterior) to colorCambio(Tendencia.BAJA, esGasto)
                else -> stringResource(R.string.reportes_centro_igual, anterior) to MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                text = texto,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Centro con una categoría elegida: "Alimentación / Bs 640 / 27 % de tus gastos". */
@Composable
private fun CentroCategoria(porcion: PorcionCategoria, esGasto: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(140.dp)) {
        Text(
            text = porcion.nombre,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = Formato.montoRedondo(porcion.total),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )
        Text(
            text = stringResource(
                if (esGasto) R.string.reportes_porcentaje_gastos else R.string.reportes_porcentaje_ingresos,
                porcion.porcentaje,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "🍴 Alimentación · Ver 14 movimientos →": abre Movimientos filtrado por la categoría. */
@Composable
private fun AccionVerMovimientos(porcion: PorcionCategoria, cantidad: Int, onClick: () -> Unit) {
    val color = colorCategoria(porcion.color, MaterialTheme.colorScheme.primary)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(2.dp, color),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Espacio.m, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacio.s),
        ) {
            Icon(iconoCategoria(porcion.icono), contentDescription = null, tint = color)
            Text(
                text = porcion.nombre,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = pluralStringResource(R.plurals.reportes_ver_movimientos, cantidad, cantidad),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Una fila de la lista: color, nombre, %, monto y el cambio frente al mes anterior.
 * Entra deslizándose, 40 ms después de la fila de arriba. Toda la fila se puede tocar.
 */
@Composable
private fun FilaLeyenda(
    porcion: PorcionCategoria,
    color: Color,
    esGasto: Boolean,
    elegida: Boolean,
    indice: Int,
    clave: Any,
    onClick: () -> Unit,
) {
    val visible = remember(clave) { MutableTransitionState(false) }
    LaunchedEffect(clave) {
        delay(40L * indice)
        visible.targetState = true
    }
    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 2 },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (elegida) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = Espacio.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacio.s),
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(
                text = porcion.nombre,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.reportes_porcentaje, porcion.porcentaje),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(36.dp),
            )
            Text(
                text = Formato.montoRedondo(porcion.total),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(76.dp),
            )
            TextoCambio(porcion, esGasto, Modifier.width(52.dp))
        }
    }
}

/** "▲ 18 %", "▼ 10 %", "igual" o "nueva". Siempre con texto, no solo con color. */
@Composable
private fun TextoCambio(porcion: PorcionCategoria, esGasto: Boolean, modifier: Modifier) {
    val (texto, color) = when (porcion.tendencia) {
        Tendencia.SUBE -> stringResource(R.string.reportes_cambio_sube, porcion.cambioPorcentaje ?: 0) to colorCambio(Tendencia.SUBE, esGasto)
        Tendencia.BAJA -> stringResource(R.string.reportes_cambio_baja, porcion.cambioPorcentaje ?: 0) to colorCambio(Tendencia.BAJA, esGasto)
        Tendencia.IGUAL -> stringResource(R.string.reportes_cambio_igual) to MaterialTheme.colorScheme.onSurfaceVariant
        Tendencia.NUEVA -> stringResource(R.string.reportes_cambio_nueva) to MaterialTheme.colorScheme.secondary
    }
    Text(
        text = texto,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        textAlign = TextAlign.End,
        maxLines = 1,
        modifier = modifier,
    )
}

/** En gastos, subir es malo (rojo); en ingresos, subir es bueno (verde). */
@Composable
private fun colorCambio(tendencia: Tendencia, esGasto: Boolean): Color {
    val esMalo = (tendencia == Tendencia.SUBE) == esGasto
    return if (esMalo) CashinColores.gasto else CashinColores.ingreso
}

/** Mes sin gastos (o sin ingresos): Cashi pensando y un botón para anotar. */
@Composable
private fun VacioCategorias(
    tipo: TipoRegistro,
    mes: YearMonth,
    esMesActual: Boolean,
    onAnotar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val esGasto = tipo == TipoRegistro.GASTO
    val titulo = when {
        esMesActual && esGasto -> stringResource(R.string.reportes_vacio_gastos_titulo)
        esMesActual -> stringResource(R.string.reportes_vacio_ingresos_titulo)
        esGasto -> stringResource(R.string.reportes_vacio_gastos_otro_mes, mesEnMinuscula(mes))
        else -> stringResource(R.string.reportes_vacio_ingresos_otro_mes, mesEnMinuscula(mes))
    }
    EstadoVacio(
        titulo = titulo,
        mensaje = stringResource(if (esGasto) R.string.reportes_vacio_gastos_mensaje else R.string.reportes_vacio_ingresos_mensaje),
        recurso = RecursoCashi.PENSANDO,
        textoAccion = if (esMesActual) stringResource(R.string.reportes_vacio_accion) else null,
        onAccion = if (esMesActual) onAnotar else null,
        modifier = modifier,
    )
}

// ------------------------------------------------------------------ Meses

/** Barras de los últimos meses, lo que sobró y la división del gasto del mes. */
@Composable
private fun ContenidoMeses(
    meses: List<MesResumen>,
    mes: YearMonth,
    division: DivisionGastoMes?,
    frase: FraseReporte?,
) {
    val colorIngreso = MaterialTheme.colorScheme.primary
    val colorGasto = CashinColores.gasto
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Espacio.pantalla, vertical = Espacio.s),
        verticalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        TarjetaJuego {
            TituloTarjeta(stringResource(R.string.reportes_meses_titulo), stringResource(R.string.reportes_meses_subtitulo))
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.m), modifier = Modifier.padding(vertical = Espacio.xs)) {
                MarcaLeyenda(colorIngreso, stringResource(R.string.reportes_leyenda_ingresos))
                MarcaLeyenda(colorGasto, stringResource(R.string.reportes_leyenda_gastos))
            }
            GraficoBarras(
                barras = meses.map {
                    BarraMes(mesCorto(it.mes), it.ingresos.toFloat(), it.gastos.toFloat(), destacado = it.mes == mes)
                },
                descripcion = stringResource(R.string.reportes_barras_desc),
                colorIngreso = colorIngreso,
                colorGasto = colorGasto,
                claveAnimacion = mes,
                modifier = Modifier.padding(top = Espacio.s),
            )
        }

        TarjetaJuego {
            TituloTarjeta(stringResource(R.string.reportes_sobro_titulo), null)
            val recientes = meses.takeLast(3).reversed()
            val maximo = recientes.maxOf { it.sobrante.abs() }.takeIf { it.signum() > 0 } ?: BigDecimal.ONE
            recientes.forEach { FilaSobrante(it, maximo) }
        }

        if (division != null) {
            TarjetaDivision(division, mes)
        }

        if (frase != null) {
            val (texto, recurso) = textoFrase(frase, null, mes)
            AvatarBurbuja(mensaje = texto, recurso = recurso, emocion = Emocion.FELIZ, tamanoAvatar = 80.dp)
        }
    }
}

@Composable
private fun TituloTarjeta(titulo: String, subtitulo: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (subtitulo != null) {
            Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MarcaLeyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(texto, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}

/** "Nov ▬▬▬▬▬▬▬ Bs 850": la barra se mide contra el mes con más diferencia. */
@Composable
private fun FilaSobrante(mes: MesResumen, maximo: BigDecimal) {
    val aFavor = mes.sobrante.signum() >= 0
    val fraccion = (mes.sobrante.abs().toFloat() / maximo.toFloat()).coerceIn(0f, 1f)
    val color = if (aFavor) CashinColores.ingreso else CashinColores.gasto
    Row(
        modifier = Modifier.fillMaxWidth().height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.s),
    ) {
        Text(mesCorto(mes.mes), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(36.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraccion)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (aFavor) MaterialTheme.colorScheme.primary else CashinColores.gasto),
            )
        }
        Text(
            text = (if (aFavor) "" else "- ") + Formato.montoRedondo(mes.sobrante.abs()),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.End,
            modifier = Modifier.width(84.dp),
        )
    }
}

/** Una barra partida en fijos, variables y recortable, y debajo cuánto es cada parte. */
@Composable
private fun TarjetaDivision(division: DivisionGastoMes, mes: YearMonth) {
    val partes = listOf(
        Triple(stringResource(R.string.reportes_fijos), division.fijo, MaterialTheme.colorScheme.secondary),
        Triple(stringResource(R.string.reportes_variables), division.variable, MaterialTheme.colorScheme.primary),
        Triple(stringResource(R.string.reportes_recortable), division.recortable, MaterialTheme.colorScheme.tertiary),
    )
    TarjetaJuego {
        TituloTarjeta(stringResource(R.string.reportes_division_titulo, mesEnMinuscula(mes)), null)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Espacio.s)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            partes.filter { it.second.signum() > 0 }.forEach { (_, monto, color) ->
                Box(modifier = Modifier.weight(monto.toFloat()).height(14.dp).background(color))
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            partes.forEachIndexed { i, (texto, monto, color) ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = when (i) {
                        0 -> Alignment.Start
                        1 -> Alignment.CenterHorizontally
                        else -> Alignment.End
                    },
                ) {
                    MarcaLeyenda(color, texto)
                    Text(
                        text = Formato.montoRedondo(monto),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Cargando

/** Aro y filas grises del mismo tamaño que el reporte: la pantalla no salta al llegar los datos. */
@Composable
private fun EsqueletoReporte() {
    val gris = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Espacio.pantalla, vertical = Espacio.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        Canvas(modifier = Modifier.size(232.dp)) {
            val ancho = 36.dp.toPx()
            drawCircle(color = gris, radius = (size.minDimension - ancho - 8.dp.toPx()) / 2f, style = Stroke(ancho))
        }
        TarjetaJuego {
            listOf(0.5f, 0.4f, 0.55f, 0.35f).forEach { fraccion ->
                FilaEsqueleto(gris, fraccion)
            }
        }
    }
}

@Composable
private fun ColumnScope.FilaEsqueleto(gris: Color, fraccion: Float) {
    Row(
        modifier = Modifier.fillMaxWidth().height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.s),
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(gris))
        Box(Modifier.fillMaxWidth(fraccion).height(12.dp).clip(RoundedCornerShape(6.dp)).background(gris))
        Spacer(Modifier.weight(1f))
        Box(Modifier.width(60.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(gris))
    }
}

// ------------------------------------------------------------------ Textos

/**
 * Convierte la frase calculada en prediccion/ en texto y en la pose de Cashi que la acompaña:
 * preocupado si un gasto subió, celebrando si bajó, feliz con los ingresos y ahorrando en Meses.
 */
@Composable
private fun textoFrase(frase: FraseReporte, reporte: ReporteCategorias?, mes: YearMonth): Pair<String, RecursoCashi> {
    val monto = frase.monto?.let { Formato.montoRedondo(it) } ?: ""
    val anterior = mesEnMinuscula(mes.minusMonths(1))
    val sinAnterior = reporte != null && reporte.totalAnterior.signum() == 0
    return when (frase.tipo) {
        TipoFraseReporte.CATEGORIA_SUBIO ->
            stringResource(R.string.reportes_frase_categoria_subio, frase.nombre.orEmpty(), monto, anterior) to RecursoCashi.PREOCUPADO
        TipoFraseReporte.GASTO_BAJO ->
            stringResource(R.string.reportes_frase_gasto_bajo, monto, anterior) to RecursoCashi.CELEBRANDO
        TipoFraseReporte.GASTO_PARECIDO ->
            (if (sinAnterior) stringResource(R.string.reportes_frase_sin_anterior)
            else stringResource(R.string.reportes_frase_gasto_parecido, anterior)) to RecursoCashi.COOL
        TipoFraseReporte.INGRESO_EXTRA -> {
            val base = stringResource(
                R.string.reportes_frase_ingreso_extra,
                mesEnMinuscula(mes), monto, frase.nombre.orEmpty(),
            )
            conSobrante(base, frase.sobrante) to RecursoCashi.FELIZ
        }
        TipoFraseReporte.INGRESOS_PARECIDOS -> {
            val base = if (sinAnterior) stringResource(R.string.reportes_frase_sin_anterior)
            else stringResource(R.string.reportes_frase_ingresos_parecidos, anterior)
            conSobrante(base, frase.monto) to RecursoCashi.FELIZ
        }
        TipoFraseReporte.MEJOR_MES -> {
            val texto = frase.mesReferencia?.let {
                stringResource(R.string.reportes_frase_mejor_mes_desde, Formato.nombreMes(mes.atDay(1)), mesEnMinuscula(it), monto)
            } ?: stringResource(R.string.reportes_frase_mejor_mes, Formato.nombreMes(mes.atDay(1)), monto)
            texto to RecursoCashi.AHORRO
        }
        TipoFraseReporte.SOBRA -> stringResource(R.string.reportes_frase_sobra, mesEnMinuscula(mes), monto) to RecursoCashi.AHORRO
        TipoFraseReporte.NO_SOBRA -> stringResource(R.string.reportes_frase_no_sobra, mesEnMinuscula(mes), monto) to RecursoCashi.PREOCUPADO
        TipoFraseReporte.SIN_DATOS -> stringResource(R.string.reportes_meses_vacio_titulo) to RecursoCashi.PENSANDO
    }
}

@Composable
private fun conSobrante(base: String, sobrante: BigDecimal?): String =
    if (sobrante == null) base else base + " " + stringResource(R.string.reportes_frase_quedan, Formato.montoRedondo(sobrante))

/** "noviembre" */
private fun mesEnMinuscula(mes: YearMonth): String = Formato.nombreMes(mes.atDay(1)).lowercase()

/** "Nov" */
private fun mesCorto(mes: YearMonth): String = Formato.nombreMes(mes.atDay(1)).take(3)

private fun VistaReporte.texto(): Int = when (this) {
    VistaReporte.GASTOS -> R.string.reportes_vista_gastos
    VistaReporte.INGRESOS -> R.string.reportes_vista_ingresos
    VistaReporte.MESES -> R.string.reportes_vista_meses
}

// ------------------------------------------------------------------ Vista previa

private fun filaEjemplo(mes: YearMonth, id: String, nombre: String, color: String, total: String) =
    GastoCategoriaMes(usuarioId = "u", mes = mes.atDay(1), categoriaId = id, nombre = nombre, color = color, total = BigDecimal(total))

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA, heightDp = 900)
@Composable
private fun ReportesPreview() = CashinTheme {
    val nov = YearMonth.of(2026, 11)
    val oct = nov.minusMonths(1)
    val filas = listOf(
        filaEjemplo(nov, "1", "Vivienda", "#2E7D32", "900"), filaEjemplo(oct, "1", "Vivienda", "#2E7D32", "900"),
        filaEjemplo(nov, "2", "Alimentación", "#E08A2C", "640"), filaEjemplo(oct, "2", "Alimentación", "#E08A2C", "542"),
        filaEjemplo(nov, "3", "Transporte", "#1F6E8C", "280"), filaEjemplo(oct, "3", "Transporte", "#1F6E8C", "310"),
        filaEjemplo(nov, "4", "Entretenimiento", "#8E5BA8", "250"), filaEjemplo(oct, "4", "Entretenimiento", "#8E5BA8", "190"),
    )
    val gastos = reportePorCategoria(filas, nov)
    ReportesContenido(
        estado = ReportesUiState(
            mes = nov, mesActual = nov, primerMes = oct,
            gastos = gastos,
            ingresos = reportePorCategoria(emptyList(), nov),
            fraseGastos = fraseGastos(gastos),
            fraseIngresos = fraseIngresos(reportePorCategoria(emptyList(), nov), null),
            fraseMeses = fraseMeses(emptyList()),
            cargando = false,
        ),
        onReintentar = {}, onVistaCambio = {}, onMesAnterior = {}, onMesSiguiente = {}, onCategoriaTocada = {},
        onVerMovimientos = { _, _, _ -> }, contarMovimientos = { _, _, _ -> 14 }, onAnotar = {},
    )
}
