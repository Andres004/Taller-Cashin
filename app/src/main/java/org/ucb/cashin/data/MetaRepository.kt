package org.ucb.cashin.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable
import org.ucb.cashin.model.AporteMeta
import org.ucb.cashin.model.AvanceMeta
import org.ucb.cashin.model.EstadoMeta
import org.ucb.cashin.model.Meta
import org.ucb.cashin.model.NuevaMeta
import org.ucb.cashin.model.NuevoAporteMeta

/** Para cancelar una meta sin borrar sus ahorros anotados. */
@Serializable
private data class EstadoDeMeta(val estado: EstadoMeta)

/**
 * Módulo M6 · Metas (HU-17, HU-18, HU-19). Una meta es solo información:
 * Cashin no guarda ni mueve dinero, la persona anota cuánto fue apartando.
 */
class MetaRepository(private val supabase: SupabaseClient) {

    /**
     * HU-19: metas con su total ahorrado (vista v_avance_meta, que calcula la base de datos).
     * Las canceladas no se muestran.
     */
    suspend fun listarAvances(): Result<List<AvanceMeta>> = llamada {
        supabase.from("v_avance_meta").select {
            filter { neq("estado", EstadoMeta.CANCELADA.name) }
            order("nombre", Order.ASCENDING)
        }.decodeList<AvanceMeta>()
    }

    /** El avance de una sola meta, para su pantalla de detalle. */
    suspend fun obtenerAvance(metaId: String): Result<AvanceMeta> = llamada {
        supabase.from("v_avance_meta").select {
            filter { eq("meta_id", metaId) }
        }.decodeSingle<AvanceMeta>()
    }

    /** Ahorros y retiros de una meta, del más reciente al más antiguo. */
    suspend fun listarAportes(metaId: String): Result<List<AporteMeta>> = llamada {
        supabase.from("aporte_meta").select {
            filter { eq("meta_id", metaId) }
            order("fecha", Order.DESCENDING)
        }.decodeList<AporteMeta>()
    }

    /** Una meta por su id, para poder editarla. */
    suspend fun obtener(metaId: String): Result<Meta> = llamada {
        supabase.from("meta").select {
            filter { eq("id", metaId) }
        }.decodeSingle<Meta>()
    }

    /** Guarda los cambios de nombre, monto objetivo, fecha límite y descripción. */
    suspend fun actualizar(metaId: String, cambios: NuevaMeta): Result<Meta> = llamada {
        supabase.from("meta").update(cambios) {
            filter { eq("id", metaId) }
            select()
        }.decodeSingle<Meta>()
    }

    /** HU-17: crea una meta (nombre, monto objetivo y fecha límite opcional). */
    suspend fun crear(nueva: NuevaMeta): Result<Meta> = llamada {
        supabase.from("meta").insert(nueva) {
            select()
        }.decodeSingle<Meta>()
    }

    /**
     * HU-18: anota un ahorro o un retiro.
     * La base de datos rechaza un retiro mayor a lo ahorrado (trigger fn_validar_retiro).
     */
    suspend fun anotarAporte(nuevo: NuevoAporteMeta): Result<AporteMeta> = llamada {
        supabase.from("aporte_meta").insert(nuevo) {
            select()
        }.decodeSingle<AporteMeta>()
    }

    /** Quita la meta de la lista (queda CANCELADA); sus ahorros anotados no se borran. */
    suspend fun cancelar(metaId: String): Result<Unit> = llamada {
        supabase.from("meta").update(EstadoDeMeta(EstadoMeta.CANCELADA)) {
            filter { eq("id", metaId) }
        }
        Unit
    }
}