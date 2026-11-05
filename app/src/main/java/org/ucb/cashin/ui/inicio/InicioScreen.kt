package org.ucb.cashin.ui.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.model.Categoria
import org.ucb.cashin.model.Desafio
import org.ucb.cashin.model.PeriodoDesafio
import org.ucb.cashin.model.ProgresoDesafio
import org.ucb.cashin.model.ReglaDesafio
import org.ucb.cashin.model.Registro
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.prediccion.Animo
import org.ucb.cashin.prediccion.Confianza
import org.ucb.cashin.prediccion.MensajeAvatar
import org.ucb.cashin.prediccion.TipoMensaje
import org.ucb.cashin.ui.components.AvatarBurbuja
import org.ucb.cashin.ui.components.BarraProgreso
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinTarjeta
import org.ucb.cashin.ui.components.Emocion
import org.ucb.cashin.ui.components.EstadoCargando
import org.ucb.cashin.ui.components.EstadoError
import org.ucb.cashin.ui.components.FilaRegistro
import org.ucb.cashin.ui.components.VarianteBoton
import org.ucb.cashin.ui.components.emocion
import org.ucb.cashin.ui.components.texto
import org.ucb.cashin.ui.components.textoConfianza
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.desafios.ProgresoDeDesafio
import org.ucb.cashin.ui.registros.RegistroConCategoria
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.util.Formato
import java.math.BigDecimal
import java.time.Instant

/**
 * HU-13: Inicio con el saludo, los totales del mes, la racha, el XP y las últimas anotaciones.
 * HU-21: consejo del día de Cashi. HU-23/24: celebraciones de XP y nivel, desafío activo y racha en riesgo.
 */
@Composable
fun InicioScreen(
    onVerTodo: () -> Unit,
    onRegistroClick: (registroId: String) -> Unit,
    onAbrirDesafios: () -> Unit,
    viewModel: InicioViewModel = viewModel(factory = InicioViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Se recarga cada vez que se entra a Inicio (por ejemplo, después de anotar).
    LaunchedEffect(Unit) { viewModel.cargar() }

    InicioContenido(
        estado = estado,
        onReintentar = viewModel::cargar,
        onVerTodo = onVerTodo,
        onRegistroClick = onRegistroClick,
        onAbrirDesafios = onAbrirDesafios,
    )
}

/** Solo dibuja. Muestra la carga, el error con "Reintentar" o el resumen. */
@Composable
fun InicioContenido(
    estado: InicioUiState,
    onReintentar: () -> Unit,
    onVerTodo: () -> Unit,
    onRegistroClick: (registroId: String) -> Unit,
    onAbrirDesafios: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        when {
            estado.cargando -> EstadoCargando()

            estado.error != null -> EstadoError(
                mensaje = estado.error.mensaje(),
                onReintentar = onReintentar,
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = Espacio.pantalla, end = Espacio.pantalla, top = Espacio.l, bottom = Espacio.m),
                verticalArrangement = Arrangement.spacedBy(Espacio.m),
            ) {
                Encabezado(nombre = estado.nombre)

                Celebraciones(estado = estado)

                MensajeDelAvatar(estado = estado)

                TarjetaDelMes(estado = estado)

                estado.desafiosActivos.forEach { desafio ->
                    TarjetaDesafioActivo(
                        desafio = desafio,
                        progreso = estado.progresoDesafios[desafio.id],
                        onClick = onAbrirDesafios,
                    )
                }

                TarjetaPronostico(estado = estado)

                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(Espacio.m),
                ) {
                    TarjetaRacha(
                        racha = estado.racha,
                        enRiesgo = estado.rachaEnRiesgo,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    TarjetaNivel(
                        nivel = estado.nivel,
                        xp = estado.xp,
                        progreso = estado.progresoNivel,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }

                UltimasAnotaciones(
                    ultimas = estado.ultimas,
                    onVerTodo = onVerTodo,
                    onRegistroClick = onRegistroClick,
                )
            }
        }
    }
}

/** El mes actual en pequeño ("Octubre 2026") y debajo el saludo en grande. */
@Composable
private fun Encabezado(nombre: String) {
    Column {
        Text(
            text = Formato.mes(Formato.hoy()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.inicio_saludo, nombre),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/**
 * HU-23 / HU-24: Cashi celebra lo que pasó desde la última vez que se abrió Inicio:
 * desafíos cumplidos ("+50 XP") y subida de nivel. Se muestra solo en esa carga.
 */
@Composable
private fun Celebraciones(estado: InicioUiState) {
    estado.subioANivel?.let { nivel ->
        AvatarBurbuja(
            mensaje = stringResource(R.string.inicio_subiste_nivel, nivel),
            emocion = Emocion.CELEBRANDO,
        )
    }
    estado.desafiosCumplidos.forEach { desafio ->
        AvatarBurbuja(
            mensaje = stringResource(R.string.desafios_celebrar, desafio.titulo, desafio.xpRecompensa),
            emocion = Emocion.CELEBRANDO,
        )
    }
}

/**
 * HU-21: consejo del día. El motor (prediccion/Consejos.kt) elige por prioridad: racha en riesgo,
 * desafío en riesgo, categoría que sube, gasto más rápido, meta cerca, racha buena o gasto normal.
 */
@Composable
private fun MensajeDelAvatar(estado: InicioUiState) {
    val mensajeIa = estado.mensajeIa ?: return
    AvatarBurbuja(
        mensaje = mensajeIa.texto(),
        emocion = mensajeIa.animo.emocion(),
    )
}

/** "Desafío activo": título y progreso. Al tocarla abre la pantalla de Desafíos. */
@Composable
private fun TarjetaDesafioActivo(desafio: Desafio, progreso: ProgresoDesafio?, onClick: () -> Unit) {
    CashinTarjeta(onClick = onClick) {
        Text(
            text = stringResource(R.string.inicio_desafio_activo),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = desafio.titulo,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = Espacio.xs, bottom = Espacio.s),
        )
        ProgresoDeDesafio(desafio = desafio, progreso = progreso)
    }
}

/**
 * Tarjeta verde oscuro del mes: la diferencia en grande y debajo los gastos e ingresos.
 * Es solo una diferencia entre lo anotado, no un saldo (Cashin no maneja dinero).
 */
@Composable
private fun TarjetaDelMes(estado: InicioUiState) {
    val textoSuave = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f)

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Espacio.m)) {
            Text(
                text = stringResource(R.string.inicio_diferencia),
                style = MaterialTheme.typography.labelMedium,
                color = textoSuave,
            )
            Text(
                text = Formato.monto(estado.diferencia),
                style = MaterialTheme.typography.displaySmall,
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = Espacio.s),
                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.2f),
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                TotalDelMes(
                    etiqueta = stringResource(R.string.inicio_gastos),
                    monto = estado.totalGastos,
                    icono = Icons.Filled.NorthEast,
                    modifier = Modifier.weight(1f),
                )
                TotalDelMes(
                    etiqueta = stringResource(R.string.inicio_ingresos),
                    monto = estado.totalIngresos,
                    icono = Icons.Filled.SouthWest,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * IA predictiva en Inicio:
 * - "Al ritmo actual terminarás octubre con ~Bs X de gastos" (en rojo si supera el máximo pronosticado).
 * - "Próximo mes: entre Bs mín y Bs máx".
 * - La confianza del pronóstico.
 * Sin un mes completo de historia, explica que todavía no puede pronosticar.
 */
@Composable
private fun TarjetaPronostico(estado: InicioUiState) {
    val hoy = Formato.hoy()

    CashinTarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .padding(end = Espacio.s)
                    .size(20.dp),
            )
            Text(
                text = stringResource(R.string.inicio_pronostico),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (!estado.hayPronostico) {
            Text(
                text = stringResource(R.string.inicio_pronostico_sin_datos),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = Espacio.s),
            )
        } else {
            Text(
                text = stringResource(
                    R.string.inicio_pronostico_mes,
                    Formato.mes(hoy).substringBefore(' ').lowercase(),
                    Formato.monto(estado.proyeccionMes),
                ),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (estado.gastaMasRapido) CashinColores.gasto else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = Espacio.s),
            )

            if (estado.pronosticoProximo.confianza != Confianza.SIN_DATOS) {
                Text(
                    text = stringResource(
                        R.string.inicio_pronostico_proximo,
                        Formato.monto(estado.pronosticoProximo.minimo),
                        Formato.monto(estado.pronosticoProximo.maximo),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = Espacio.xs),
                )
            }

            Text(
                text = textoConfianza(estado.pronosticoMes.confianza, estado.pronosticoMes.mesesUsados),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = Espacio.xs),
            )
        }
    }
}

/** Ícono en un círculo claro y, al lado, la etiqueta y el monto. Va dentro de la tarjeta verde. */
@Composable
private fun TotalDelMes(etiqueta: String, monto: BigDecimal, icono: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.s),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.14f)),
        ) {
            Icon(imageVector = icono, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f),
            )
            Text(
                text = Formato.monto(monto),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

/** HU-24: días seguidos anotando. Si hoy todavía no anotó, la racha se muestra "en riesgo" en rojo. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TarjetaRacha(racha: Int, enRiesgo: Boolean, modifier: Modifier = Modifier) {
    CashinTarjeta(modifier = modifier) {
        Text(
            text = stringResource(R.string.inicio_racha),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = racha.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = if (enRiesgo) CashinColores.gasto else CashinColores.racha,
        )
        Text(
            text = if (enRiesgo) {
                stringResource(R.string.inicio_racha_en_riesgo)
            } else {
                pluralStringResource(R.plurals.inicio_dias_seguidos, racha)
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (enRiesgo) CashinColores.gasto else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TarjetaNivel(nivel: Int, xp: Int, progreso: Float, modifier: Modifier = Modifier) {
    CashinTarjeta(modifier = modifier) {
        Text(
            text = stringResource(R.string.inicio_nivel, nivel),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.inicio_xp, xp),
            style = MaterialTheme.typography.headlineMedium,
            color = CashinColores.xp,
        )
        BarraProgreso(progreso = progreso, modifier = Modifier.padding(top = Espacio.s))
    }
}

/** Las últimas 5 anotaciones, con el mismo diseño que el Historial, y el botón "Ver todo". */
@Composable
private fun UltimasAnotaciones(
    ultimas: List<RegistroConCategoria>,
    onVerTodo: () -> Unit,
    onRegistroClick: (registroId: String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.inicio_ultimas),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            CashinBoton(
                texto = stringResource(R.string.inicio_ver_todo),
                onClick = onVerTodo,
                variante = VarianteBoton.TEXTO,
            )
        }

        if (ultimas.isEmpty()) {
            Text(
                text = stringResource(R.string.inicio_sin_anotaciones),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ultimas.forEach { anotacion ->
                FilaRegistro(
                    registro = anotacion.registro,
                    categoria = anotacion.categoria,
                    onClick = { onRegistroClick(anotacion.registro.id) },
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA, heightDp = 900)
@Composable
private fun InicioPreview() = CashinTheme {
    val comida = Categoria(id = "c1", nombre = "Alimentación", tipo = TipoRegistro.GASTO, icono = "restaurant", color = "#4CAF50")
    InicioContenido(
        estado = InicioUiState(
            nombre = "Carlos",
            totalGastos = BigDecimal("2050.00"),
            totalIngresos = BigDecimal("3200.00"),
            racha = 4,
            anotoHoy = true,
            nivel = 2,
            xp = 230,
            progresoNivel = 0.15f,
            mensajeIa = MensajeAvatar(TipoMensaje.RACHA_BUENA, Animo.FELIZ, dias = 4),
            desafiosActivos = listOf(
                Desafio(
                    id = "d1", usuarioId = "u", titulo = "Alimentación bajo control",
                    periodo = PeriodoDesafio.SEMANAL, regla = ReglaDesafio.LIMITE_CATEGORIA,
                    categoriaId = "c1", montoLimite = BigDecimal("175.00"),
                    fechaInicio = Formato.hoy().minusDays(2), fechaFin = Formato.hoy().plusDays(4),
                ),
            ),
            progresoDesafios = mapOf("d1" to ProgresoDesafio("d1", BigDecimal("90.00"), 3)),
            ultimas = listOf(
                RegistroConCategoria(
                    Registro(id = "1", usuarioId = "u", categoriaId = "c1", tipo = TipoRegistro.GASTO, monto = BigDecimal("20.00"), fechaHora = Instant.now(), nota = "Almuerzo"),
                    comida,
                ),
            ),
            cargando = false,
        ),
        onReintentar = {},
        onVerTodo = {},
        onRegistroClick = {},
        onAbrirDesafios = {},
    )
}