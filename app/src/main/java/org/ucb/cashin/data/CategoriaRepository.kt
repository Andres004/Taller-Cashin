package org.ucb.cashin.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import org.ucb.cashin.model.Categoria
import org.ucb.cashin.model.NuevaCategoria
import org.ucb.cashin.model.TipoRegistro

/**
 * Lee y administra las categorías del usuario: las del sistema y las que él creó.
 */
class CategoriaRepository(private val supabase: SupabaseClient) {

    /** Categorías activas de un tipo, para elegir al anotar un gasto o ingreso. */
    suspend fun listar(tipo: TipoRegistro): Result<List<Categoria>> = llamada {
        supabase.from("categoria").select {
            filter {
                eq("tipo", tipo.name)
                eq("archivada", false)
            }
            order("nombre", Order.ASCENDING)
        }.decodeList<Categoria>()
    }

    /** Categorías de un tipo con las archivadas incluidas, para la pantalla Mis categorías. */
    suspend fun listarConArchivadas(tipo: TipoRegistro): Result<List<Categoria>> = llamada {
        supabase.from("categoria").select {
            filter { eq("tipo", tipo.name) }
            order("nombre", Order.ASCENDING)
        }.decodeList<Categoria>()
    }

    /** HU-11: crea una categoría propia. La BD rechaza el nombre repetido para el mismo tipo. */
    suspend fun crear(categoria: NuevaCategoria): Result<Unit> = llamada {
        supabase.from("categoria").insert(categoria)
        Unit
    }

    /** HU-12: archiva (o desarchiva) una categoría. No se borra: el historial la sigue mostrando. */
    suspend fun archivar(id: String, archivada: Boolean): Result<Unit> = llamada {
        supabase.from("categoria").update(mapOf("archivada" to archivada)) {
            filter { eq("id", id) }
        }
        Unit
    }

    /** Todas las categorías, incluso las archivadas, para mostrar el historial completo. */
    suspend fun listarTodas(): Result<List<Categoria>> = llamada {
        supabase.from("categoria").select().decodeList<Categoria>()
    }
}