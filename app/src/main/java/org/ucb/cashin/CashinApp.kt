package org.ucb.cashin

import android.app.Application
import org.ucb.cashin.di.AppContainer

/** Punto de entrada de la app: crea una sola vez el contenedor de dependencias. */
class CashinApp : Application() {

    /** null solo si falta configurar local.properties (ver README). */
    var container: AppContainer? = null
        private set

    override fun onCreate() {
        super.onCreate()
        if (AppContainer.estaConfigurado) container = AppContainer()
    }

    /** Para ViewModels: falla con un mensaje claro si se usa sin configurar. */
    fun requireContainer(): AppContainer =
        checkNotNull(container) { "Falta SUPABASE_URL/SUPABASE_KEY en local.properties" }
}
