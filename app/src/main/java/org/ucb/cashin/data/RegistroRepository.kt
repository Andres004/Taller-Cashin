package org.ucb.cashin.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import org.ucb.cashin.model.NuevoRegistro
import org.ucb.cashin.model.Registro

/**
 * Guarda, lee, edita y borra los gastos e ingresos del usuario.
 * Cada usuario solo puede ver y cambiar sus propias anotaciones.
 */
class RegistroRepository(private val supabase: SupabaseClient) {

    /** Guarda una anotación nueva y la devuelve ya creada. */
    suspend fun crear(nuevo: NuevoRegistro): Result<Registro> = llamada {
        supabase.from("registro").insert(nuevo) {
            select()
        }.decodeSingle<Registro>()
    }

    /** Anotaciones de la más reciente a la más antigua. Con un límite, solo las últimas. */
    suspend fun listar(limite: Long? = null): Result<List<Registro>> = llamada {
        supabase.from("registro").select {
            order("fecha_hora", Order.DESCENDING)
            if (limite != null) limit(limite)
        }.decodeList<Registro>()
    }

    /** Una anotación por su id, para poder editarla. */
    suspend fun obtener(id: String): Result<Registro> = llamada {
        supabase.from("registro").select {
            filter { eq("id", id) }
        }.decodeSingle<Registro>()
    }

    /**
     * Guarda los cambios de una anotación.
     * Hay que mantener su fecha original para no moverla a "ahora".
     */
    suspend fun actualizar(id: String, cambios: NuevoRegistro): Result<Registro> = llamada {
        supabase.from("registro").update(cambios) {
            filter { eq("id", id) }
            select()
        }.decodeSingle<Registro>()
    }

    /** Borra una anotación. */
    suspend fun eliminar(id: String): Result<Unit> = llamada {
        supabase.from("registro").delete {
            filter { eq("id", id) }
        }
        Unit
    }
}