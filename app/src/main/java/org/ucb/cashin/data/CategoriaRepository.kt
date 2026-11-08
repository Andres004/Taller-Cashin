package org.ucb.cashin.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import org.ucb.cashin.model.Categoria
import org.ucb.cashin.model.ImportanciaCategoria
import org.ucb.cashin.model.NuevaCategoria
import org.ucb.cashin.model.TipoRegistro

/** Con estos códigos responde el servidor cuando una tabla no existe. */
private val CODIGOS_TABLA_INEXISTENTE = setOf("PGRST205", "42P01")

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
        }.decodeList<Categoria>().let { lista ->
            if (tipo == TipoRegistro.GASTO) conImportanciaElegida(lista) else lista
        }
    }

    /** Guarda la importancia que la persona eligió; las categorías del sistema no se modifican. */
    suspend fun fijarImportancia(usuarioId: String, categoriaId: String, importancia: Int): Result<Unit> = llamada {
        supabase.from("categoria_importancia").upsert(
            ImportanciaCategoria(usuarioId, categoriaId, importancia),
        ) {
            onConflict = "usuario_id,categoria_id"
        }
        Unit
    }

    /** Reemplaza el valor inicial por el que la persona eligió, si lo cambió. */
    private suspend fun conImportanciaElegida(lista: List<Categoria>): List<Categoria> {
        val elegidas = try {
            supabase.from("categoria_importancia").select()
                .decodeList<ImportanciaCategoria>()
                .associate { it.categoriaId to it.importancia }
        } catch (e: PostgrestRestException) {
            // La migración 005 aún no se ejecutó: la pantalla sigue funcionando con los valores iniciales.
            if (e.code in CODIGOS_TABLA_INEXISTENTE) emptyMap() else throw e
        }
        return lista.map { categoria ->
            elegidas[categoria.id]?.let { categoria.copy(importancia = it) } ?: categoria
        }
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