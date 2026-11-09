package org.ucb.cashin.ui.inicio

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.model.AvanceMeta
import org.ucb.cashin.model.Desafio
import org.ucb.cashin.model.EstadoMeta
import org.ucb.cashin.model.PeriodoDesafio
import org.ucb.cashin.model.ProgresoDesafio
import org.ucb.cashin.model.ReglaDesafio
import org.ucb.cashin.prediccion.Animo
import org.ucb.cashin.prediccion.Confianza
import org.ucb.cashin.prediccion.EstadoPlan
import org.ucb.cashin.prediccion.MensajeAvatar
import org.ucb.cashin.prediccion.PlanAhorro
import org.ucb.cashin.prediccion.TipoMensaje
import org.ucb.cashin.ui.components.AvatarImagen
import org.ucb.cashin.ui.components.BarraEstadisticas
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.ChipSube
import org.ucb.cashin.ui.components.ComoLoCalcule
import org.ucb.cashin.ui.components.Confeti
import org.ucb.cashin.ui.components.Emocion
import org.ucb.cashin.ui.components.EstadoCargando
import org.ucb.cashin.ui.components.EstadoError
import org.ucb.cashin.ui.components.RecursoCashi
import org.ucb.cashin.ui.components.TarjetaJuego
import org.ucb.cashin.ui.components.TiraCamino
import org.ucb.cashin.ui.components.emocion
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.components.texto
import org.ucb.cashin.ui.desafios.ProgresoDeDesafio
import org.ucb.cashin.ui.desafios.ResultadoDesafioDialogo
import org.ucb.cashin.ui.metas.alcanzada
import org.ucb.cashin.ui.metas.faltante
import org.ucb.cashin.ui.metas.porcentaje
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.ui.theme.Naranja
import org.ucb.cashin.ui.theme.NaranjaRelieve
import org.ucb.cashin.util.Formato
import java.math.BigDecimal

private val SEPARACION_BLOQUES = 10.dp
private val TAMANO_CASHI = 210.dp
private val FOCO_CONFETI = 300.dp

/**
 * Inicio corto: saludo con racha y nivel, consejo de Cashi, la meta principal con su camino,
 * cómo va el mes (HU-13) y un reto. HU-21: consejo del día. HU-23/24: celebraciones.
 */
@Composable
fun InicioScreen(
    onAbrirDesafios: () -> Unit,
    onMetaClick: (metaId: String) -> Unit,
    onNuevaMeta: () -> Unit,
    onAbrirRacha: () -> Unit = {},
    viewModel: InicioViewModel = viewModel(factory = InicioViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Se recarga cada vez que se entra a Inicio (por ejemplo, después de anotar).
    LaunchedEffect(Unit) { viewModel.cargar() }

    InicioContenido(
        estado = estado,
        onReintentar = viewModel::cargar,
        onMetaClick = onMetaClick,
        onNuevaMeta = onNuevaMeta,
        onAbrirDesafios = {
            viewModel.avisoNuevosVisto()
            onAbrirDesafios()
        },
        onAbrirRacha = onAbrirRacha,
    )

    // HU-23: si al abrir Inicio se cerró un desafío, se muestra la animación (cumplido o fallido).
    estado.resultadosDesafios.firstOrNull()?.let { desafio ->
        ResultadoDesafioDialogo(
            desafio = desafio,
            onCerrar = viewModel::resultadoVisto,
            onOtroReto = {
                viewModel.resultadoVisto()
                onAbrirDesafios()
            },
        )
    }
}

/** Solo dibuja. Muestra la carga, el error con "Reintentar" o el resumen. */
@Composable
fun InicioContenido(
    estado: InicioUiState,
    onReintentar: () -> Unit,
    onMetaClick: (metaId: String) -> Unit,
    onNuevaMeta: () -> Unit,
    onAbrirDesafios: () -> Unit,
    onAbrirRacha: () -> Unit = {},
) {
    when {
        estado.cargando -> EstadoCargando()

        estado.error != null -> EstadoError(
            mensaje = estado.error.mensaje(),
            onReintentar = onReintentar,
        )

        else -> Box(modifier = Modifier.fillMaxSize()) {
            var caminoVisto by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(Unit) { caminoVisto = true }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = Espacio.pantalla, end = Espacio.pantalla, top = Espacio.l, bottom = Espacio.m),
                verticalArrangement = Arrangement.spacedBy(SEPARACION_BLOQUES),
            ) {
                Encabezado(estado, onAbrirRacha)
                if (estado.hayDesafiosNuevos) AvisoDesafiosNuevos(onAbrirDesafios)
                MensajeDeCashi(estado)
                if (estado.metasDisponibles) {
                    val meta = estado.metaPrincipal
                    if (meta != null) {
                        TarjetaTuMeta(meta, estado.planMeta, animarEntrada = !caminoVisto, onClick = { onMetaClick(meta.metaId) })
                    } else {
                        TarjetaSinMetas(estado.tieneMetasCompletadas, onNuevaMeta)
                    }
                }
                TarjetaEsteMes(estado)
                TarjetaTuReto(estado.retoPrincipal, estado.progresoDesafios, onAbrirDesafios)
            }

            estado.subioANivel?.let { nivel -> key(nivel) { ConfetiDeNivel() } }
        }
    }
}

@Composable
private fun ConfetiDeNivel() {
    var visible by remember { mutableStateOf(true) }
    val hapticos = LocalHapticFeedback.current
    LaunchedEffect(Unit) { hapticos.performHapticFeedback(HapticFeedbackType.LongPress) }
    if (visible) Confeti(foco = FOCO_CONFETI, onFin = { visible = false })
}

/** El saludo y debajo la racha y el nivel; al subir de nivel, los XP ganados suben al costado. */
@Composable
private fun Encabezado(estado: InicioUiState, onAbrirRacha: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.inicio_saludo, estado.nombre),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            BarraEstadisticas(
                racha = estado.racha,
                rachaEnRiesgo = estado.rachaEnRiesgo,
                nivel = estado.nivel,
                xp = estado.xp,
                resaltarNivel = estado.subioANivel != null,
                onRachaClick = onAbrirRacha,
            )
            Spacer(Modifier.weight(1f))
            estado.xpGanado?.let { xp ->
                key(xp) { ChipSube(stringResource(R.string.inicio_xp_ganado, xp), Naranja) }
            }
        }
    }
}

/** HU-22: una sola línea, solo cuando llegan retos. */
@Composable
private fun AvisoDesafiosNuevos(onVer: () -> Unit) {
    Surface(
        onClick = onVer,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.desafios_nuevos_titulo),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.inicio_ver),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

/**
 * HU-21 / HU-24: Cashi grande, con el consejo del día en un globo; al subir de nivel celebra.
 * Queda apoyado sobre la tarjeta siguiente, que tapa el corte de las poses de medio cuerpo.
 */
@Composable
private fun MensajeDeCashi(estado: InicioUiState) {
    val nivel = estado.subioANivel
    val consejo = estado.mensajeIa
    val emocion = when {
        nivel != null -> Emocion.CELEBRANDO
        consejo != null -> consejo.animo.emocion()
        else -> Emocion.FELIZ
    }
    val texto = when {
        nivel != null -> stringResource(R.string.inicio_subiste_nivel, nivel)
        consejo != null -> consejo.texto()
        else -> stringResource(R.string.inicio_cashi_saludo)
    }
    val colorGlobo = CashinColores.fondoAvatar
    val escala = remember { Animatable(0.75f) }
    LaunchedEffect(Unit) {
        escala.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = SEPARACION_BLOQUES + 2.dp),
    ) {
        Surface(
            color = colorGlobo,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        Canvas(modifier = Modifier.size(width = 24.dp, height = 12.dp)) {
            val cola = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2, size.height)
                close()
            }
            drawPath(cola, colorGlobo)
        }
        AvatarImagen(
            emocion = emocion,
            tamano = TAMANO_CASHI,
            recurso = if (nivel != null) RecursoCashi.SUBIDA_NIVEL else emocion.recurso,
            modifier = Modifier.graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 1f)
                scaleX = escala.value
                scaleY = escala.value
            },
        )
    }
}

@Composable
private fun TarjetaTuMeta(meta: AvanceMeta, plan: PlanAhorro?, animarEntrada: Boolean, onClick: () -> Unit) {
    val sinCamino = plan == null || plan.estado == EstadoPlan.SIN_DATOS
    val paso = plan?.pasoActual
    val (principal, secundario) = when {
        meta.alcanzada -> stringResource(R.string.metas_objetivo_llegado) to stringResource(R.string.inicio_marcala)
        plan?.estado == EstadoPlan.SIN_DATOS -> stringResource(R.string.plan_sin_datos_titulo) to stringResource(R.string.plan_sin_datos)
        paso != null -> stringResource(
            if (paso.esMeta) R.string.camino_ultimo else R.string.metas_proximo_ahorro,
            Formato.montoEntero(paso.monto),
            Formato.fechaCorta(paso.fecha),
        ) to null
        else -> stringResource(R.string.metas_te_faltan, Formato.monto(meta.faltante)) to null
    }

    TarjetaJuego(onClick = onClick, conRelleno = false) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 4.dp)) {
            Text(
                text = stringResource(R.string.inicio_tu_meta),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = meta.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.inicio_porcentaje, meta.porcentaje),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NaranjaRelieve,
                )
            }
        }
        TiraCamino(plan = plan, lograda = meta.alcanzada, animarEntrada = animarEntrada)
        Column(modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 6.dp)) {
            Text(
                text = principal,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (secundario != null) {
                Text(
                    text = secundario,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(if (meta.alcanzada || sinCamino) R.string.inicio_ver_meta else R.string.inicio_ver_camino),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun TarjetaSinMetas(tieneCompletadas: Boolean, onNuevaMeta: () -> Unit) {
    TarjetaJuego {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Outlined.EmojiEvents,
                contentDescription = null,
                tint = CashinColores.xp,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = stringResource(if (tieneCompletadas) R.string.inicio_sin_activas_titulo else R.string.metas_vacio_titulo),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.metas_vacio_mensaje),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Espacio.xs, bottom = Espacio.s),
            )
            CashinBoton(
                texto = stringResource(if (tieneCompletadas) R.string.metas_nueva else R.string.metas_vacio_accion),
                onClick = onNuevaMeta,
            )
        }
    }
}

/**
 * HU-13 + "¿Me alcanza este mes?": primero la respuesta, debajo gastos, ingresos y diferencia.
 * Es solo una diferencia entre lo anotado, no un saldo (Cashin no maneja dinero).
 */
@Composable
private fun TarjetaEsteMes(estado: InicioUiState) {
    val color = MaterialTheme.colorScheme.onSecondary
    val suave = color.copy(alpha = 0.82f)
    val sobrante = estado.sobranteDelMes
    val redondeado = sobrante?.abs()?.let { Formato.aproximado(it) }
    val (frase, detalle) = when {
        !estado.hayPronostico -> stringResource(R.string.inicio_pronostico_sin_datos) to null
        sobrante == null -> stringResource(R.string.inicio_alcanza_sin_ingresos, Formato.montoAproximado(estado.proyeccionMes)) to null
        redondeado?.signum() == 0 -> stringResource(R.string.inicio_alcanza_justo) to null
        sobrante.signum() > 0 -> stringResource(R.string.inicio_alcanza_si) to
            stringResource(R.string.inicio_alcanza_sobra, Formato.montoAproximado(sobrante))
        else -> stringResource(R.string.inicio_alcanza_no) to
            stringResource(R.string.inicio_alcanza_falta, Formato.montoAproximado(sobrante.abs()))
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondary,
        contentColor = color,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = if (estado.hayPronostico) 2.dp else 12.dp)) {
            Text(
                text = stringResource(R.string.inicio_este_mes, Formato.nombreMes(Formato.hoy())),
                style = MaterialTheme.typography.bodySmall,
                color = suave,
            )
            Text(
                text = frase,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 1.dp),
            )
            if (detalle != null) {
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = Espacio.s), color = color.copy(alpha = 0.2f))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TotalDelMes(stringResource(R.string.inicio_gastos), estado.totalGastos, Modifier.weight(1f))
                TotalDelMes(stringResource(R.string.inicio_ingresos), estado.totalIngresos, Modifier.weight(1f))
                TotalDelMes(stringResource(R.string.inicio_diferencia_corta), estado.diferencia, Modifier.weight(1f))
            }
            if (estado.hayPronostico) {
                val meses = estado.pronosticoMes.mesesUsados
                val proximo = estado.pronosticoProximo
                val usados = pluralStringResource(R.plurals.inicio_como_calcule, meses, meses)
                ComoLoCalcule(
                    detalle = if (proximo.confianza != Confianza.SIN_DATOS) {
                        usados + " " + stringResource(
                            R.string.inicio_como_proximo,
                            Formato.montoAproximado(proximo.minimo),
                            Formato.montoAproximado(proximo.maximo),
                        )
                    } else {
                        usados
                    },
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun TotalDelMes(etiqueta: String, monto: BigDecimal, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.82f),
        )
        Text(
            text = Formato.monto(monto),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Un solo reto; los demás están en "Ver todos". Si todavía no llegaron, avisa que llegan cada lunes. */
@Composable
private fun TarjetaTuReto(reto: Desafio?, progreso: Map<String, ProgresoDesafio>, onClick: () -> Unit) {
    TarjetaJuego(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.EmojiEvents,
                contentDescription = null,
                tint = CashinColores.xp,
                modifier = Modifier
                    .padding(end = Espacio.s)
                    .size(22.dp),
            )
            Text(
                text = stringResource(R.string.inicio_tu_reto),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.inicio_ver_desafios),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (reto == null) {
            Text(
                text = stringResource(R.string.inicio_sin_desafios),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = Espacio.s),
            )
        } else {
            Text(
                text = reto.titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = Espacio.s, bottom = Espacio.xs),
            )
            ProgresoDeDesafio(desafio = reto, progreso = progreso[reto.id])
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA, heightDp = 800)
@Composable
private fun InicioPreview() = CashinTheme {
    InicioContenido(
        estado = InicioUiState(
            nombre = "Carlos",
            totalGastos = BigDecimal("2050.00"),
            totalIngresos = BigDecimal("3200.00"),
            racha = 4,
            anotoHoy = true,
            nivel = 2,
            xp = 230,
            mensajeIa = MensajeAvatar(TipoMensaje.RACHA_BUENA, Animo.FELIZ, dias = 4),
            metaPrincipal = AvanceMeta(
                metaId = "1", usuarioId = "u", nombre = "Celular nuevo",
                montoObjetivo = BigDecimal("2500.00"), estado = EstadoMeta.ACTIVA, totalAhorrado = BigDecimal("1051.00"),
            ),
            metasDisponibles = true,
            retoPrincipal = Desafio(
                id = "d1", usuarioId = "u", titulo = "Alimentación bajo control",
                periodo = PeriodoDesafio.SEMANAL, regla = ReglaDesafio.LIMITE_CATEGORIA,
                categoriaId = "c1", montoLimite = BigDecimal("175.00"),
                fechaInicio = Formato.hoy().minusDays(2), fechaFin = Formato.hoy().plusDays(4),
            ),
            progresoDesafios = mapOf("d1" to ProgresoDesafio("d1", BigDecimal("90.00"), 3)),
            hayDesafiosNuevos = true,
            cargando = false,
        ),
        onReintentar = {},
        onMetaClick = {},
        onNuevaMeta = {},
        onAbrirDesafios = {},
    )
}
