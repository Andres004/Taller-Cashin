package org.ucb.cashin.data

import android.content.Context

/**
 * Datos de la cuenta que se guardan solo en el teléfono.
 * Hoy: si ya se mostró la bienvenida de la primera vez con Google (una vez por usuario).
 */
class PreferenciasCuenta(private val contexto: Context) {

    private val prefs get() = contexto.getSharedPreferences("cuenta", Context.MODE_PRIVATE)

    fun bienvenidaGoogleVista(usuarioId: String): Boolean = prefs.getBoolean("bienvenida_google_$usuarioId", false)

    fun marcarBienvenidaGoogle(usuarioId: String) {
        prefs.edit().putBoolean("bienvenida_google_$usuarioId", true).apply()
    }
}
