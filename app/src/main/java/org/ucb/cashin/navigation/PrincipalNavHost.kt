package org.ucb.cashin.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import org.ucb.cashin.R
import org.ucb.cashin.ui.inicio.InicioScreen
import org.ucb.cashin.ui.metas.MetasScreen
import org.ucb.cashin.ui.perfil.PerfilScreen
import org.ucb.cashin.ui.registros.AnotarScreen
import org.ucb.cashin.ui.registros.HistorialScreen
import kotlin.reflect.KClass

/** Pestañas de la barra inferior: Inicio · Historial · (+) Anotar · Metas · Perfil. */
private enum class Pestana(
    val ruta: Any,
    val clase: KClass<*>,
    @StringRes val etiqueta: Int,
    val icono: ImageVector,
) {
    INICIO(Rutas.Inicio, Rutas.Inicio::class, R.string.nav_inicio, Icons.Outlined.Home),
    HISTORIAL(Rutas.Historial, Rutas.Historial::class, R.string.nav_historial, Icons.AutoMirrored.Outlined.ReceiptLong),
    ANOTAR(Rutas.Anotar, Rutas.Anotar::class, R.string.nav_anotar, Icons.Filled.AddCircle),
    METAS(Rutas.Metas, Rutas.Metas::class, R.string.nav_metas, Icons.Outlined.Flag),
    PERFIL(Rutas.Perfil, Rutas.Perfil::class, R.string.nav_perfil, Icons.Outlined.Person),
}

/**
 * App con sesión iniciada. Las pantallas internas de cada módulo (detalle de meta,
 * mis categorías, desafíos...) se registran en este mismo NavHost.
 */
@Composable
fun PrincipalNavHost() {
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val destino = entrada?.destination

    // Mensajes cortos que siguen visibles aunque se cambie de pantalla (ej.: "¡Anotado! +10 XP").
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val textoAnotado = stringResource(R.string.anotar_guardado)
    val textoAnotadoXp = stringResource(R.string.anotar_guardado_xp)
    val textoCambiosGuardados = stringResource(R.string.anotar_cambios_guardados)
    val textoEliminada = stringResource(R.string.anotar_eliminada)

    fun mostrarMensaje(texto: String) {
        scope.launch { snackbar.showSnackbar(texto) }
    }

    /** Cambia de pestaña recordando dónde se quedó cada una. */
    fun irAPestana(ruta: Any) {
        nav.navigate(ruta) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Pestana.entries.forEach { pestana ->
                    val seleccionada = destino?.hierarchy?.any { it.hasRoute(pestana.clase) } == true
                    val esAnotar = pestana == Pestana.ANOTAR
                    NavigationBarItem(
                        selected = seleccionada,
                        onClick = { irAPestana(pestana.ruta) },
                        icon = {
                            Icon(
                                imageVector = pestana.icono,
                                contentDescription = stringResource(pestana.etiqueta),
                                tint = if (esAnotar) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                                modifier = if (esAnotar) Modifier.size(40.dp) else Modifier,
                            )
                        },
                        label = if (esAnotar) null else { { Text(stringResource(pestana.etiqueta)) } },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.secondary,
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Rutas.Inicio,
            modifier = Modifier.padding(padding),
        ) {
            composable<Rutas.Inicio> { InicioScreen() }

            composable<Rutas.Historial> {
                HistorialScreen(
                    onRegistroClick = { registroId -> nav.navigate(Rutas.EditarRegistro(registroId)) },
                )
            }

            composable<Rutas.Anotar> {
                AnotarScreen(
                    onGuardado = { ganoXp ->
                        irAPestana(Rutas.Inicio)
                        mostrarMensaje(if (ganoXp) textoAnotadoXp else textoAnotado)
                    },
                )
            }

            composable<Rutas.EditarRegistro> { entradaActual ->
                val ruta = entradaActual.toRoute<Rutas.EditarRegistro>()
                AnotarScreen(
                    registroId = ruta.registroId,
                    onGuardado = {
                        nav.popBackStack()
                        mostrarMensaje(textoCambiosGuardados)
                    },
                    onEliminado = {
                        nav.popBackStack()
                        mostrarMensaje(textoEliminada)
                    },
                    onAtras = { nav.popBackStack() },
                )
            }

            composable<Rutas.Metas> { MetasScreen() }
            composable<Rutas.Perfil> { PerfilScreen() }
        }
    }
}