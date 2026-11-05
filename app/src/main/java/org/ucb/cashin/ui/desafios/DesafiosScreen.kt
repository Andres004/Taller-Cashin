package org.ucb.cashin.ui.desafios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.model.Desafio
import org.ucb.cashin.model.EstadoDesafio
import org.ucb.cashin.model.PeriodoDesafio
import org.ucb.cashin.model.ProgresoDesafio
import org.ucb.cashin.model.ReglaDesafio
import org.ucb.cashin.prediccion.PropuestaDesafio
import org.ucb.cashin.ui.components.AvatarBurbuja
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
import java.time.temporal.ChronoUnit

/** Desde el 80 % del límite la barra se pinta de rojo. */
private const val UMBRAL_RIESGO = 0.8f

/** XP que da cada periodo (lo decide el servidor en aceptar_desafio; aquí solo se muestra). */
private fun xpDe(periodo: PeriodoDesafio) = if (periodo == PeriodoDesafio.SEMANAL) 50 else 150

/**
 * HU-22 / HU-23: desafíos semanales y mensuales.
 * Arriba la celebración (o el ánimo) de lo que acaba de cerrarse; luego los activos con su
 * progreso; las propuestas del motor para el periodo libre; y el historial.
 */
@Composable
fun DesafiosScreen(
    onAtras: () -> Unit,
    viewModel: DesafiosViewModel = viewModel(factory = DesafiosViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Se evalúa cada vez que se entra: así un desafío vencido se cierra al abrir la pantalla.
    LaunchedEffect(Unit) { viewModel.cargar() }

    DesafiosContenido(
        estado = estado,
        onAtras = onAtras,
        onReintentar = viewModel::cargar,
        onAceptar = viewModel::aceptar,
        onDescartar = viewModel::descartar,
    )
}

/** Solo dibuja. Estados: cargando, error, vacío (sin activos ni historial) o con datos. */
@Composable
fun DesafiosContenido(
    estado: DesafiosUiState,
    onAtras: () -> Unit,
    onReintentar: () -> Unit,
    onAceptar: (PropuestaDesafio, String, String) -> Unit,
    onDescartar: (String) -> Unit,
) {
    var porDescartar by remember { mutableStateOf<Desafio?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(titulo = stringResource(R.string.perfil_desafios), onAtras = onAtras)

        when {
            estado.cargando -> EstadoCargando()

            estado.error != null -> EstadoError(mensaje = estado.error.mensaje(), onReintentar = onReintentar)

            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = Espacio.pantalla, vertical = Espacio.s),
                verticalArrangement = Arrangement.spacedBy(Espacio.m),
            ) {
                // HU-23: lo que la evaluación acaba de cerrar.
                items(estado.cumplidosRecientes, key = { "cumplido-${it.id}" }) { desafio ->
                    AvatarBurbuja(
                        mensaje = stringResource(R.string.desafios_celebrar, desafio.titulo, desafio.xpRecompensa),
                        emocion = Emocion.CELEBRANDO,
                    )
                }
                items(estado.fallidosRecientes, key = { "fallido-${it.id}" }) { desafio ->
                    AvatarBurbuja(
                        mensaje = stringResource(R.string.desafios_animo, desafio.titulo),
                        emocion = Emocion.NEUTRAL,
                    )
                }

                if (estado.vacio) {
                    item(key = "vacio") {
                        AvatarBurbuja(
                            mensaje = stringResource(R.string.desafios_vacio),
                            emocion = Emocion.FELIZ,
                        )
                    }
                }

                // Error al aceptar o descartar (por ejemplo "ya tienes uno activo").
                if (estado.errorAccionTexto != null || estado.errorAccion != null) {
                    item(key = "error-accion") {
                        Text(
                            text = estado.errorAccionTexto?.let { stringResource(it) }
                                ?: estado.errorAccion?.mensaje().orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                if (estado.activos.isNotEmpty()) {
                    item(key = "titulo-activos") { TituloSeccion(stringResource(R.string.desafios_activos)) }
                    items(estado.activos, key = { it.id }) { desafio ->
                        TarjetaActivo(
                            desafio = desafio,
                            progreso = estado.progreso[desafio.id],
                            descartando = estado.descartando == desafio.id,
                            onDescartar = { porDescartar = desafio },
                        )
                    }
                }

                seccionPropuestas(
                    periodo = PeriodoDesafio.SEMANAL,
                    propuestas = estado.propuestasSemanales,
                    aceptando = estado.aceptando,
                    onAceptar = onAceptar,
                )
                seccionPropuestas(
                    periodo = PeriodoDesafio.MENSUAL,
                    propuestas = estado.propuestasMensuales,
                    aceptando = estado.aceptando,
                    onAceptar = onAceptar,
                )

                if (estado.historial.isNotEmpty()) {
                    item(key = "titulo-historial") { TituloSeccion(stringResource(R.string.desafios_historial)) }
                    items(estado.historial, key = { "historial-${it.id}" }) { FilaHistorial(it) }
                }
            }
        }
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

/** "Nuevo desafío semanal/mensual" con sus propuestas. No aparece si ya hay uno activo de ese periodo. */
private fun LazyListScope.seccionPropuestas(
    periodo: PeriodoDesafio,
    propuestas: List<PropuestaDesafio>,
    aceptando: PropuestaDesafio?,
    onAceptar: (PropuestaDesafio, String, String) -> Unit,
) {
    if (propuestas.isEmpty()) return
    item(key = "titulo-propuestas-$periodo") {
        TituloSeccion(
            stringResource(
                if (periodo == PeriodoDesafio.SEMANAL) R.string.desafios_nuevo_semanal else R.string.desafios_nuevo_mensual,
            ),
        )
    }
    items(propuestas, key = { "propuesta-$periodo-${it.regla}" }) { propuesta ->
        TarjetaPropuesta(
            propuesta = propuesta,
            cargando = aceptando == propuesta,
            habilitado = aceptando == null,
            onAceptar = onAceptar,
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
 * Barra y "Bs 120,00 de Bs 175,00" (límites) o "3 de 5 días anotando" (días).
 * En rojo desde el 80 % del límite. También la usa la tarjeta "Desafío activo" de Inicio.
 */
@Composable
fun ProgresoDeDesafio(desafio: Desafio, progreso: ProgresoDesafio?) {
    val limite = desafio.montoLimite
    val gastado = progreso?.gastado ?: BigDecimal.ZERO
    val dias = progreso?.diasConRegistro ?: 0
    val objetivo = desafio.diasObjetivo ?: 0

    val avance = when {
        limite != null && limite.signum() > 0 -> gastado.toFloat() / limite.toFloat()
        objetivo > 0 -> dias.toFloat() / objetivo
        else -> 0f
    }
    val enRiesgo = limite != null && avance >= UMBRAL_RIESGO

    BarraProgreso(
        progreso = avance,
        color = if (enRiesgo) CashinColores.gasto else MaterialTheme.colorScheme.primary,
    )
    Text(
        text = if (limite != null) {
            stringResource(R.string.desafios_progreso_monto, Formato.monto(gastado), Formato.monto(limite))
        } else {
            stringResource(R.string.desafios_progreso_dias, dias, objetivo)
        },
        style = MaterialTheme.typography.labelLarge,
        color = if (enRiesgo) CashinColores.gasto else MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = Espacio.s),
    )
}

/** Un desafío en curso: título, descripción, días restantes, su progreso y "Descartar". */
@Composable
private fun TarjetaActivo(
    desafio: Desafio,
    progreso: ProgresoDesafio?,
    descartando: Boolean,
    onDescartar: () -> Unit,
) {
    val restantes = ChronoUnit.DAYS.between(Formato.hoy(), desafio.fechaFin).toInt().coerceAtLeast(0)

    CashinTarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
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

        CashinBoton(
            texto = stringResource(R.string.desafios_descartar),
            onClick = onDescartar,
            variante = VarianteBoton.TEXTO,
            cargando = descartando,
            modifier = Modifier.padding(top = Espacio.xs),
        )
    }
}

/** HU-22: una propuesta del motor, con la explicación de dónde sale el número y el botón "Aceptar". */
@Composable
private fun TarjetaPropuesta(
    propuesta: PropuestaDesafio,
    cargando: Boolean,
    habilitado: Boolean,
    onAceptar: (PropuestaDesafio, String, String) -> Unit,
) {
    val titulo = tituloDe(propuesta)
    val descripcion = descripcionDe(propuesta)

    CashinTarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.desafios_xp, xpDe(propuesta.periodo)),
                style = MaterialTheme.typography.labelLarge,
                color = CashinColores.xp,
            )
        }
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(top = Espacio.xs, bottom = Espacio.s),
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(end = Espacio.s),
            )
            Text(
                text = explicacionDe(propuesta),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        CashinBoton(
            texto = stringResource(R.string.desafios_aceptar),
            onClick = { onAceptar(propuesta, titulo, descripcion) },
            variante = VarianteBoton.SECUNDARIO,
            icono = Icons.Outlined.Check,
            cargando = cargando,
            habilitado = habilitado,
        )
    }
}

/** "✅ Alimentación bajo control  +50 XP" o "❌ Gasto total bajo control", con la fecha de cierre. */
@Composable
private fun FilaHistorial(desafio: Desafio) {
    val cumplido = desafio.estado == EstadoDesafio.CUMPLIDO
    CashinTarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (cumplido) R.string.desafios_cumplido else R.string.desafios_fallido,
                        desafio.titulo,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.desafios_hasta, Formato.fechaCorta(desafio.fechaFin)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            if (cumplido) {
                Text(
                    text = stringResource(R.string.desafios_xp, desafio.xpRecompensa),
                    style = MaterialTheme.typography.labelLarge,
                    color = CashinColores.xp,
                )
            }
        }
    }
}

/* Plantillas (no IA generativa): título y descripción que se guardan, y la explicación que se muestra. */

@Composable
private fun cuando(periodo: PeriodoDesafio): String = stringResource(
    if (periodo == PeriodoDesafio.SEMANAL) R.string.desafios_esta_semana else R.string.desafios_este_mes,
)

@Composable
private fun tituloDe(propuesta: PropuestaDesafio): String = when (propuesta.regla) {
    ReglaDesafio.LIMITE_CATEGORIA -> stringResource(R.string.desafio_titulo_categoria, propuesta.categoriaNombre.orEmpty())
    ReglaDesafio.LIMITE_TOTAL -> stringResource(R.string.desafio_titulo_total)
    ReglaDesafio.DIAS_REGISTRO -> (propuesta.diasObjetivo ?: 0).let { pluralStringResource(R.plurals.desafio_titulo_dias, it, it) }
}

@Composable
private fun descripcionDe(propuesta: PropuestaDesafio): String {
    val limite = Formato.monto(propuesta.montoLimite ?: BigDecimal.ZERO)
    return when (propuesta.regla) {
        ReglaDesafio.LIMITE_CATEGORIA ->
            stringResource(R.string.desafio_desc_categoria, limite, propuesta.categoriaNombre.orEmpty(), cuando(propuesta.periodo))
        ReglaDesafio.LIMITE_TOTAL -> stringResource(R.string.desafio_desc_total, limite, cuando(propuesta.periodo))
        ReglaDesafio.DIAS_REGISTRO -> stringResource(R.string.desafio_desc_dias, propuesta.diasObjetivo ?: 0, cuando(propuesta.periodo))
    }
}

@Composable
private fun explicacionDe(propuesta: PropuestaDesafio): String {
    val limite = Formato.monto(propuesta.montoLimite ?: BigDecimal.ZERO)
    val base = Formato.monto(propuesta.basadoEn ?: BigDecimal.ZERO)
    return when (propuesta.regla) {
        ReglaDesafio.LIMITE_CATEGORIA -> stringResource(
            R.string.desafio_explica_categoria, propuesta.categoriaNombre.orEmpty(), base, limite, cuando(propuesta.periodo),
        )
        ReglaDesafio.LIMITE_TOTAL -> stringResource(R.string.desafio_explica_total, base, limite, cuando(propuesta.periodo))
        ReglaDesafio.DIAS_REGISTRO -> stringResource(R.string.desafio_explica_dias)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA, heightDp = 1100)
@Composable
private fun DesafiosPreview() = CashinTheme {
    val hoy = Formato.hoy()
    DesafiosContenido(
        estado = DesafiosUiState(
            cargando = false,
            activos = listOf(
                Desafio(
                    id = "d1", usuarioId = "u", titulo = "Alimentación bajo control",
                    descripcion = "Gasta como máximo Bs 175,00 en Alimentación esta semana.",
                    periodo = PeriodoDesafio.SEMANAL, regla = ReglaDesafio.LIMITE_CATEGORIA,
                    categoriaId = "c1", montoLimite = BigDecimal("175.00"),
                    fechaInicio = hoy.minusDays(3), fechaFin = hoy.plusDays(3),
                ),
            ),
            progreso = mapOf("d1" to ProgresoDesafio("d1", BigDecimal("150.00"), 4)),
            propuestasMensuales = listOf(
                PropuestaDesafio(
                    PeriodoDesafio.MENSUAL, ReglaDesafio.LIMITE_CATEGORIA, "c1", "Alimentación",
                    montoLimite = BigDecimal("765.00"), basadoEn = BigDecimal("900.00"),
                ),
                PropuestaDesafio(PeriodoDesafio.MENSUAL, ReglaDesafio.DIAS_REGISTRO, diasObjetivo = 20),
            ),
            historial = listOf(
                Desafio(
                    id = "d0", usuarioId = "u", titulo = "Anota 5 días", periodo = PeriodoDesafio.SEMANAL,
                    regla = ReglaDesafio.DIAS_REGISTRO, diasObjetivo = 5, fechaInicio = hoy.minusDays(10),
                    fechaFin = hoy.minusDays(4), estado = EstadoDesafio.CUMPLIDO,
                ),
            ),
            recienEvaluados = listOf(
                Desafio(
                    id = "d0", usuarioId = "u", titulo = "Anota 5 días", periodo = PeriodoDesafio.SEMANAL,
                    regla = ReglaDesafio.DIAS_REGISTRO, diasObjetivo = 5, fechaInicio = hoy.minusDays(10),
                    fechaFin = hoy.minusDays(4), estado = EstadoDesafio.CUMPLIDO,
                ),
            ),
        ),
        onAtras = {},
        onReintentar = {},
        onAceptar = { _, _, _ -> },
        onDescartar = {},
    )
}
