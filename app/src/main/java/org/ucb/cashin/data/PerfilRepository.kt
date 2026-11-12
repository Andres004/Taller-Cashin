package org.ucb.cashin.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.ucb.cashin.model.EstadisticasPerfil
import org.ucb.cashin.model.Moneda
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

    /** Los números del perfil (función estadisticas_perfil, migración 010). */
    suspend fun estadisticas(): Result<EstadisticasPerfil> = llamada {
        supabase.postgrest.rpc("estadisticas_perfil").decodeSingle<EstadisticasPerfil>()
    }

    /**
     * "Editar perfil": nombre, moneda, avatar y día de cobro. El día de cobro solo se envía si cambió,
     * así cambiar el nombre o el avatar funciona aunque la migración 010 todavía no esté ejecutada.
     */
    suspend fun actualizarTodo(
        id: String,
        nombre: String,
        moneda: Moneda,
        avatar: String,
        diaCobro: Int?,
        diaCobroCambio: Boolean,
    ): Result<Perfil> = llamada {
        val cambios = buildJsonObject {
            put("nombre", nombre)
            put("moneda", moneda.name)
            put("avatar", avatar)
            if (diaCobroCambio) {
                if (diaCobro == null) put("dia_cobro", JsonNull) else put("dia_cobro", diaCobro)
            }
        }
        supabase.from("perfil").update(cambios) {
            filter { eq("id", id) }
            select()
        }.decodeSingle<Perfil>()
    }
}
