package org.ucb.cashin.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.ucb.cashin.ui.auth.BienvenidaScreen
import org.ucb.cashin.ui.auth.CrearCuentaScreen
import org.ucb.cashin.ui.auth.LoginScreen
import org.ucb.cashin.ui.auth.RecuperarScreen

/**
 * Flujo sin sesión. Empieza en Bienvenida y desde ahí:
 * Bienvenida → Crear cuenta | Iniciar sesión → Recuperar contraseña.
 */
@Composable
fun AuthNavHost() {
    val nav = rememberNavController()

    /**
     * Abre el Login dejando solo Bienvenida debajo. Sirve tanto si se llegó a Crear cuenta
     * desde Bienvenida (Login no está en la pila) como desde el Login (no lo repite).
     */
    val irALogin: () -> Unit = {
        nav.navigate(Rutas.Login) {
            popUpTo(Rutas.Bienvenida)
            launchSingleTop = true
        }
    }

    NavHost(navController = nav, startDestination = Rutas.Bienvenida) {
        composable<Rutas.Bienvenida> {
            BienvenidaScreen(
                onCrearCuenta = { nav.navigate(Rutas.CrearCuenta) { launchSingleTop = true } },
                onIniciarSesion = { nav.navigate(Rutas.Login) { launchSingleTop = true } },
            )
        }
        composable<Rutas.Login> {
            LoginScreen(
                onCrearCuenta = { nav.navigate(Rutas.CrearCuenta) { launchSingleTop = true } },
                onOlvideContrasena = { nav.navigate(Rutas.RecuperarContrasena) { launchSingleTop = true } },
                onAtras = { nav.popBackStack() },
            )
        }
        composable<Rutas.CrearCuenta> {
            CrearCuentaScreen(
                onIrALogin = irALogin,
                onAtras = { nav.popBackStack() },
            )
        }
        composable<Rutas.RecuperarContrasena> {
            RecuperarScreen(onAtras = { nav.popBackStack() })
        }
    }
}