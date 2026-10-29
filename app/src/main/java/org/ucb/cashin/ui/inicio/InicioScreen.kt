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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.R
import org.ucb.cashin.model.Categoria
import org.ucb.cashin.model.Registro
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.ui.components.AvatarBurbuja
import org.ucb.cashin.ui.components.BarraProgreso
import org.ucb.cashin.ui.components.CashinBoton
import org.ucb.cashin.ui.components.CashinTarjeta
import org.ucb.cashin.ui.components.Emocion
import org.ucb.cashin.ui.components.EstadoCargando
import org.ucb.cashin.ui.components.EstadoError
import org.ucb.cashin.ui.components.FilaRegistro
import org.ucb.cashin.ui.components.VarianteBoton
import org.ucb.cashin.ui.components.mensaje
import org.ucb.cashin.ui.registros.RegistroConCategoria
import org.ucb.cashin.ui.theme.CashinColores
import org.ucb.cashin.ui.theme.CashinTheme
import org.ucb.cashin.ui.theme.Espacio
import org.ucb.cashin.util.Formato
import java.math.BigDecimal
import java.time.Instant

/**
 * HU-13: Inicio con el saludo, los totales del mes, la racha, el XP y las últimas anotaciones.
 */
@Composable
fun InicioScreen(
    onVerTodo: () -> Unit,
    onRegistroClick: (registroId: String) -> Unit,
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
    )
}

/** Solo dibuja. Muestra la carga, el error con "Reintentar" o el resumen. */
@Composable
fun InicioContenido(
    estado: InicioUiState,
    onReintentar: () -> Unit,
    onVerTodo: () -> Unit,
    onRegistroClick: (registroId: String) -> Unit,
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

                MensajeDelAvatar(racha = estado.racha, anotoHoy = estado.anotoHoy)

                TarjetaDelMes(estado = estado)

                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(Espacio.m),
                ) {
                    TarjetaRacha(
                        racha = estado.racha,
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

/** Salchi anima a anotar o celebra la racha, según si ya se anotó algo hoy. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MensajeDelAvatar(racha: Int, anotoHoy: Boolean) {
    if (anotoHoy) {
        AvatarBurbuja(
            mensaje = pluralStringResource(R.plurals.inicio_avatar_racha, racha, racha),
            emocion = Emocion.CELEBRANDO,
        )
    } else {
        AvatarBurbuja(
            mensaje = stringResource(R.string.inicio_avatar_sin_registro_hoy),
            emocion = Emocion.FELIZ,
        )
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TarjetaRacha(racha: Int, modifier: Modifier = Modifier) {
    CashinTarjeta(modifier = modifier) {
        Text(
            text = stringResource(R.string.inicio_racha),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = racha.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = CashinColores.racha,
        )
        Text(
            text = pluralStringResource(R.plurals.inicio_dias_seguidos, racha),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    )
}