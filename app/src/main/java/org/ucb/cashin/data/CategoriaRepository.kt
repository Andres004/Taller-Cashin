package org.ucb.cashin.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import org.ucb.cashin.model.Categoria
import org.ucb.cashin.model.TipoRegistro

/**
 * Lee las categorías del usuario: las del sistema y las que él creó.
 */
class CategoriaRepository(private val supabase: SupabaseClient) {

    /** Categorías activas de un tipo, para elegir al anotar un gasto o ingreso. */
    suspend fun listar(tipo: TipoRegistro): Result<List<Categoria>> = llamada {
        supabase.from("categoria").select {
            filter {
                eq("tipo", tipo.name)
                eq("archivada", false)
            }
        }.decodeList<Categoria>()
    }

    /** Todas las categorías, incluso las archivadas, para mostrar el historial completo. */
    suspend fun listarTodas(): Result<List<Categoria>> = llamada {
        supabase.from("categoria").select().decodeList<Categoria>()
    }
}