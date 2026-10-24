package org.ucb.cashin.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.ucb.cashin.ui.auth.CrearCuentaScreen
import org.ucb.cashin.ui.auth.LoginScreen
import org.ucb.cashin.ui.auth.RecuperarScreen

/** Flujo sin sesión: Login ⇄ Crear cuenta ⇄ Recuperar contraseña. */
@Composable
fun AuthNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Rutas.Login) {
        composable<Rutas.Login> {
            LoginScreen(
                onCrearCuenta = { nav.navigate(Rutas.CrearCuenta) { launchSingleTop = true } },
                onOlvideContrasena = { nav.navigate(Rutas.RecuperarContrasena) { launchSingleTop = true } },
            )
        }
        composable<Rutas.CrearCuenta> {
            CrearCuentaScreen(onIrALogin = { nav.popBackStack(Rutas.Login, inclusive = false) })
        }
        composable<Rutas.RecuperarContrasena> {
            RecuperarScreen(onAtras = { nav.popBackStack() })
        }
    }
}
