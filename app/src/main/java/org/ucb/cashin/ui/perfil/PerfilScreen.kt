package org.ucb.cashin.ui.perfil

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.model.EstadisticasPerfil
import org.ucb.cashin.model.Perfil
import org.ucb.cashin.prediccion.AjusteRecordatorio
import org.ucb.cashin.ui.components.BarraProgreso
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinTopBar
import org.ucb.cashin.ui.components.TarjetaJuego
import org.ucb.cashin.ui.components.VarianteBoton
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.util.Formato
import java.math.BigDecimal
import java.time.LocalDate

/** Opciones del menú del Perfil. Cada una abre la pantalla de su módulo. */
enum class OpcionPerfil { MIS_CATEGORIAS, PLANIFICADOS, DEUDAS, DESAFIOS, REPORTES, RECORDATORIO }

/**
 * HU-05: perfil con avatar, nombre, correo, nivel, racha y "Tus números". El lápiz abre
 * "Editar perfil". HU-04: cerrar sesión. El menú lleva a cada módulo; quien abre esta
 * pantalla decide a qué ruta ir con [onOpcion].
 */
@Composable
fun PerfilScreen(
    onOpcion: (OpcionPerfil) -> Unit,
    onEditar: () -> Unit,
    onAbrirRacha: () -> Unit = {},
    viewModel: PerfilViewModel = viewModel(factory = PerfilViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Se recarga cada vez que se entra a la pestaña, para ver el XP, la racha y los números al día.
    LaunchedEffect(Unit) { viewModel.cargar() }

    PerfilContenido(
        estado = estado,
        onReintentar = viewModel::cargar,
        onEditar = onEditar,
        onOpcion = onOpcion,
        onCerrarSesion = viewModel::cerrarSesion,
        onAbrirRacha = onAbrirRacha,
    )
}

/** Solo dibuja. "Cerrar sesión" siempre está visible, aunque el perfil no cargue. */
@Composable
fun PerfilContenido(
    estado: PerfilUiState,
    onReintentar: () -> Unit,
    onEditar: () -> Unit,
    onOpcion: (OpcionPerfil) -> Unit,
    onCerrarSesion: () -> Unit,
    onAbrirRacha: () -> Unit = {},
) {
    var confirmarCierre by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        CashinTopBar(titulo = stringResource(R.string.nav_perfil))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.pantalla, vertical = Espacio.s),
            verticalArrangement = Arrangement.spacedBy(Espacio.m),
        ) {
            when {
                estado.cargando -> CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = Espacio.xl).align(Alignment.CenterHorizontally),
                )

                estado.errorCarga != null -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = estado.errorCarga.mensaje(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    CashinBoton(texto = stringResource(R.string.estado_reintentar), onClick = onReintentar, variante = VarianteBoton.TEXTO)
                }

                estado.perfil != null -> {
                    Cabecera(estado.perfil, estado.correo, estado.racha, onEditar, onAbrirRacha)
                    TarjetaNivel(estado.perfil)
                    TusNumeros(estado.estadisticas, estado.perfil.rachaMaxima)
                    TarjetaAhorrado(estado.estadisticas?.totalAhorrado)
                    MenuPerfil(estado.recordatorio, onOpcion)
                }
            }

            estado.error?.let {
                Text(it.mensaje(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            CashinBoton(
                texto = stringResource(R.string.perfil_cerrar_sesion),
                onClick = { confirmarCierre = true },
                variante = VarianteBoton.PELIGRO,
                icono = Icons.AutoMirrored.Outlined.Logout,
                cargando = estado.cerrandoSesion,
                modifier = Modifier.fillMaxWidth().padding(bottom = Espacio.m),
            )
        }
    }

    if (confirmarCierre) {
        AlertDialog(
            onDismissRequest = { confirmarCierre = false },
            title = { Text(stringResource(R.string.perfil_cerrar_titulo)) },
            text = { Text(stringResource(R.string.perfil_cerrar_mensaje)) },
            confirmButton = {
                TextButton(onClick = { confirmarCierre = false; onCerrarSesion() }) {
                    Text(stringResource(R.string.perfil_cerrar_sesion))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarCierre = false }) { Text(stringResource(R.string.accion_cancelar)) }
            },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }
}

/** Avatar, nombre, correo, chips de racha y nivel, y el lápiz que abre "Editar perfil". */
@Composable
private fun Cabecera(perfil: Perfil, correo: String, racha: Int, onEditar: () -> Unit, onAbrirRacha: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        CirculoAvatar(AvatarCashi.de(perfil.avatar), tamano = 84.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = perfil.nombre,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = correo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.xs), modifier = Modifier.padding(top = Espacio.xs)) {
                Chip(stringResource(R.string.perfil_chip_racha, racha), onClick = onAbrirRacha)
                Chip(stringResource(R.string.perfil_chip_nivel, perfil.nivel))
            }
        }
        Surface(
            onClick = onEditar,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.perfil_editar),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun Chip(texto: String, onClick: (() -> Unit)? = null) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = CashinColores.xp,
            modifier = Modifier.padding(horizontal = Espacio.s, vertical = 2.dp),
        )
    }
}

/** Nivel actual, XP total y cuánto falta para el siguiente nivel. */
@Composable
private fun TarjetaNivel(perfil: Perfil) {
    val xpFaltante = Perfil.XP_POR_NIVEL - perfil.xp % Perfil.XP_POR_NIVEL
    TarjetaJuego {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.perfil_nivel, perfil.nivel),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.perfil_xp, perfil.xp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = CashinColores.xp,
            )
        }
        BarraProgreso(progreso = perfil.progresoNivel, modifier = Modifier.padding(vertical = Espacio.s))
        Text(
            text = stringResource(R.string.perfil_xp_siguiente, xpFaltante, perfil.nivel + 1),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "Tus números": 6 tarjetas en una cuadrícula de 3 × 2. Sin datos todavía, cada número es "—". */
@Composable
private fun TusNumeros(estadisticas: EstadisticasPerfil?, rachaMaxima: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
        Text(
            text = stringResource(R.string.perfil_tus_numeros),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        val numeros = listOf(
            estadisticas?.diasUsando to R.string.perfil_stat_dias,
            estadisticas?.anotaciones to R.string.perfil_stat_anotaciones,
            (estadisticas?.mejorRacha ?: rachaMaxima) to R.string.perfil_stat_mejor_racha,
            estadisticas?.desafiosCumplidos to R.string.perfil_stat_desafios,
            estadisticas?.metasLogradas to R.string.perfil_stat_metas,
            estadisticas?.deudasSaldadas to R.string.perfil_stat_deudas,
        )
        numeros.chunked(3).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                fila.forEach { (valor, texto) -> TarjetaNumero(valor, texto, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun TarjetaNumero(valor: Int?, @StringRes texto: Int, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.heightIn(min = 84.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = Espacio.xs, vertical = Espacio.s),
        ) {
            Text(
                text = valor?.toString() ?: stringResource(R.string.perfil_stat_sin_datos),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(texto),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** "Ahorraste en tus metas · Bs 2.480". */
@Composable
private fun TarjetaAhorrado(total: BigDecimal?) {
    TarjetaJuego {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.perfil_ahorrado),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = total?.let { Formato.montoRedondo(it) } ?: stringResource(R.string.perfil_stat_sin_datos),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CashinColores.ingreso,
            )
        }
    }
}

/** Lista de opciones con ícono, nombre y una flecha ">". El recordatorio dice si está encendido. */
@Composable
private fun MenuPerfil(recordatorio: AjusteRecordatorio, onOpcion: (OpcionPerfil) -> Unit) {
    val linea = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, linea),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            OpcionPerfil.entries.forEachIndexed { indice, opcion ->
                if (indice > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = Espacio.m), color = linea)
                FilaMenu(
                    opcion = opcion,
                    detalle = if (opcion == OpcionPerfil.RECORDATORIO) textoRecordatorio(recordatorio) else null,
                    onClick = { onOpcion(opcion) },
                )
            }
        }
    }
}

@Composable
private fun textoRecordatorio(ajuste: AjusteRecordatorio): String {
    if (!ajuste.activo) return stringResource(R.string.perfil_recordatorio_apagado)
    val hora = "%02d:%02d".format(ajuste.hora, ajuste.minuto)
    return stringResource(
        if (ajuste.soloLaborables) R.string.perfil_recordatorio_activo_laborables else R.string.perfil_recordatorio_activo,
        hora,
    )
}

@Composable
private fun FilaMenu(opcion: OpcionPerfil, detalle: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = Espacio.m, vertical = Espacio.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        Icon(imageVector = opcion.icono(), contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(opcion.etiqueta()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
            if (detalle != null) {
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@StringRes
private fun OpcionPerfil.etiqueta(): Int = when (this) {
    OpcionPerfil.MIS_CATEGORIAS -> R.string.perfil_mis_categorias
    OpcionPerfil.PLANIFICADOS -> R.string.perfil_planificados
    OpcionPerfil.DEUDAS -> R.string.perfil_deudas
    OpcionPerfil.DESAFIOS -> R.string.perfil_desafios
    OpcionPerfil.REPORTES -> R.string.perfil_reportes
    OpcionPerfil.RECORDATORIO -> R.string.perfil_recordatorio
}

private fun OpcionPerfil.icono(): ImageVector = when (this) {
    OpcionPerfil.MIS_CATEGORIAS -> Icons.Outlined.GridView
    OpcionPerfil.PLANIFICADOS -> Icons.Outlined.CalendarMonth
    OpcionPerfil.DEUDAS -> Icons.Outlined.SwapHoriz
    OpcionPerfil.DESAFIOS -> Icons.Outlined.EmojiEvents
    OpcionPerfil.REPORTES -> Icons.Outlined.PieChart
    OpcionPerfil.RECORDATORIO -> Icons.Outlined.NotificationsNone
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF7EA, heightDp = 1200)
@Composable
private fun PerfilPreview() = CashinTheme {
    PerfilContenido(
        estado = PerfilUiState(
            correo = "andres@correo.com",
            perfil = Perfil(id = "u", nombre = "Andres", avatar = "cool", xp = 480, rachaActual = 4, rachaMaxima = 12, ultimoRegistro = LocalDate.now()),
            racha = 4,
            estadisticas = EstadisticasPerfil(72, 186, 12, 5, 1, 2, BigDecimal("2480")),
            recordatorio = AjusteRecordatorio(activo = true),
            cargando = false,
        ),
        onReintentar = {}, onEditar = {}, onOpcion = {}, onCerrarSesion = {},
    )
}
