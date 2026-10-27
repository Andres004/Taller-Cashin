package org.ucb.cashin.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import org.ucb.cashin.model.Perfil
import org.ucb.cashin.model.PerfilEditable

/**
 * Lee y cambia el perfil del usuario: nombre, XP, nivel y racha.
 * Cada usuario solo puede ver y cambiar el suyo.
 */
class PerfilRepository(private val supabase: SupabaseClient) {

    /** El perfil de quien inició sesión. */
    suspend fun obtener(): Result<Perfil> = llamada {
        supabase.from("perfil").select().decodeSingle<Perfil>()
    }

    /**
     * Guarda el nombre, la moneda y el avatar, y devuelve el perfil actualizado.
     * El XP y la racha no se pueden cambiar desde la app: los maneja la base de datos.
     */
    suspend fun actualizar(id: String, cambios: PerfilEditable): Result<Perfil> = llamada {
        supabase.from("perfil").update(cambios) {
            filter { eq("id", id) }
            select()
        }.decodeSingle<Perfil>()
    }
}