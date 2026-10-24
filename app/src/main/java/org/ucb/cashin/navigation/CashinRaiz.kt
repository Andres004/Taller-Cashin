package org.ucb.cashin.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ucb.cashin.data.EstadoSesion
import org.ucb.cashin.ui.auth.SesionViewModel
import org.ucb.cashin.ui.inicio.SplashScreen

/**
 * Raíz de la app. Según la sesión muestra:
 *  - CARGANDO  → Splash (mientras Supabase lee el token guardado)
 *  - SIN_SESION → flujo de cuenta (login, crear cuenta, recuperar)
 *  - CON_SESION → app principal con la barra inferior
 * Al iniciar o cerrar sesión el cambio es automático: ninguna pantalla navega a mano.
 */
@Composable
fun CashinRaiz(sesionViewModel: SesionViewModel = viewModel(factory = SesionViewModel.Factory)) {
    val estado by sesionViewModel.estado.collectAsStateWithLifecycle()
    Crossfade(targetState = estado, label = "sesion") { actual ->
        when (actual) {
            EstadoSesion.CARGANDO -> SplashScreen()
            EstadoSesion.SIN_SESION -> AuthNavHost()
            EstadoSesion.CON_SESION -> PrincipalNavHost()
        }
    }
}
