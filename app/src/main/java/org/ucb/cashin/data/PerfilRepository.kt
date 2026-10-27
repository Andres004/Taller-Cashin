package org.ucb.cashin.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import org.ucb.cashin.model.Perfil

/**
 * Lee el perfil del usuario: nombre, XP, nivel y racha.
 * Cada usuario solo puede ver el suyo.
 */
class PerfilRepository(private val supabase: SupabaseClient) {

    /** El perfil de quien inició sesión. */
    suspend fun obtener(): Result<Perfil> = llamada {
        supabase.from("perfil").select().decodeSingle<Perfil>()
    }
}