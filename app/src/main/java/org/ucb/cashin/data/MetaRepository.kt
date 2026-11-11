package org.ucb.cashin.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.ucb.cashin.model.AporteMeta
import org.ucb.cashin.model.AvanceMeta
import org.ucb.cashin.model.EstadoMeta
import org.ucb.cashin.model.Meta
import org.ucb.cashin.model.NuevaMeta
import org.ucb.cashin.model.NuevoAporteMeta
import org.ucb.cashin.model.TipoAporte
import org.ucb.cashin.prediccion.CadenciaPlan
import org.ucb.cashin.prediccion.CaminoGuardado
import org.ucb.cashin.prediccion.EstadoPlan
import org.ucb.cashin.prediccion.PasoGuardado
import java.math.BigDecimal
import java.time.LocalDate

/** Para completar o cancelar una meta sin borrar sus ahorros anotados. */
@Serializable
private data class EstadoDeMeta(val estado: EstadoMeta)

/**
 * Módulo M6 · Metas (HU-17, HU-18, HU-19). Una meta es solo información:
 * Cashin no guarda ni mueve dinero, la persona anota cuánto fue apartando.
 */
class MetaRepository(private val supabase: SupabaseClient) {

    /** Respaldo mientras no se ejecute la migración 007: el camino se mantiene fijo durante la sesión. */
    private val caminosEnMemoria = mutableMapOf<String, CaminoGuardado>()

    /** El camino guardado de cada meta, por id. Las que todavía no tienen camino no aparecen. */
    suspend fun caminos(): Result<Map<String, CaminoGuardado>> = llamada {
        val guardados = try {
            supabase.from("meta").select(Columns.list("id", "camino")).decodeList<FilaCamino>()
                .mapNotNull { fila -> fila.camino?.aCamino()?.let { fila.id to it } }
                .toMap()
        } catch (e: PostgrestRestException) {
            // La migración 007 aún no se ejecutó: no existe la columna camino.
            if (e.code == CODIGO_COLUMNA_INEXISTENTE) emptyMap() else throw e
        }
        caminosEnMemoria + guardados
    }

    /** Guarda el camino de una meta. Si la columna todavía no existe, queda solo en memoria. */
    suspend fun guardarCamino(metaId: String, camino: CaminoGuardado): Result<Unit> {
        caminosEnMemoria[metaId] = camino
        return llamada {
            supabase.from("meta").update(CambioCamino(CaminoJson.de(camino))) {
                filter { eq("id", metaId) }
            }
            Unit
        }
    }

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

    /** Ahorros (no retiros) anotados en cualquier meta desde [desde]. Los usan los desafíos de ahorro. */
    suspend fun listarAhorrosDesde(desde: LocalDate): Result<List<AporteMeta>> = llamada {
        supabase.from("aporte_meta").select {
            filter {
                eq("tipo", TipoAporte.AHORRO.name)
                gte("fecha", desde.toString())
            }
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

    /**
     * Cambia el estado de la meta:
     * COMPLETADA cuando la persona la marca como lograda; CANCELADA para quitarla de la lista.
     * En ningún caso se borran sus ahorros anotados.
     */
    suspend fun cambiarEstado(metaId: String, estado: EstadoMeta): Result<Unit> = llamada {
        supabase.from("meta").update(EstadoDeMeta(estado)) {
            filter { eq("id", metaId) }
        }
        Unit
    }

    /**
     * Marca la meta como COMPLETADA y suma +100 XP (función completar_meta del servidor).
     * Devuelve false si todavía no llegó al objetivo o ya no estaba activa (no da XP dos veces).
     */
    suspend fun completar(metaId: String): Result<Boolean> = llamada {
        supabase.postgrest.rpc("completar_meta", buildJsonObject { put("p_meta_id", metaId) })
            .decodeAs<Boolean>()
    }
}

private const val CODIGO_COLUMNA_INEXISTENTE = "42703"

@Serializable
private data class FilaCamino(val id: String, val camino: CaminoJson? = null)

@Serializable
private data class CambioCamino(val camino: CaminoJson)

/** Un paso dentro de la columna `meta.camino`. */
@Serializable
private data class PasoJson(val fecha: String, val monto: String)

/** Columna `meta.camino` (jsonb). Los montos van como texto para no perder decimales. */
@Serializable
private data class CaminoJson(
    val pasos: List<PasoJson>,
    @SerialName("ahorrado_inicial") val ahorradoInicial: String,
    @SerialName("ahorrado_al_estimar") val ahorradoAlEstimar: String,
    val objetivo: String,
    @SerialName("fecha_limite") val fechaLimite: String? = null,
    val estado: String,
    val cadencia: String,
    @SerialName("meses_por_paso") val mesesPorPaso: Int,
    @SerialName("ahorro_por_paso") val ahorroPorPaso: String,
) {
    /** Null si el contenido no se puede leer (por ejemplo, un camino con el formato anterior): se arma uno nuevo. */
    fun aCamino(): CaminoGuardado? = runCatching {
        CaminoGuardado(
            pasos = pasos.map { PasoGuardado(LocalDate.parse(it.fecha), BigDecimal(it.monto)) },
            ahorradoInicial = BigDecimal(ahorradoInicial),
            ahorradoAlEstimar = BigDecimal(ahorradoAlEstimar),
            objetivo = BigDecimal(objetivo),
            fechaLimite = fechaLimite?.let(LocalDate::parse),
            estado = EstadoPlan.valueOf(estado),
            cadencia = CadenciaPlan.valueOf(cadencia),
            mesesPorPaso = mesesPorPaso,
            ahorroPorPaso = BigDecimal(ahorroPorPaso),
        )
    }.getOrNull()

    companion object {
        fun de(c: CaminoGuardado) = CaminoJson(
            pasos = c.pasos.map { PasoJson(it.fecha.toString(), it.monto.toPlainString()) },
            ahorradoInicial = c.ahorradoInicial.toPlainString(),
            ahorradoAlEstimar = c.ahorradoAlEstimar.toPlainString(),
            objetivo = c.objetivo.toPlainString(),
            fechaLimite = c.fechaLimite?.toString(),
            estado = c.estado.name,
            cadencia = c.cadencia.name,
            mesesPorPaso = c.mesesPorPaso,
            ahorroPorPaso = c.ahorroPorPaso.toPlainString(),
        )
    }
}
