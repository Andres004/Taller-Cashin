package org.ucb.cashin.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.ucb.cashin.model.Desafio
import org.ucb.cashin.model.DesafioAsignado
import org.ucb.cashin.model.EstadoDesafio
import org.ucb.cashin.model.ParametrosAsignarDesafios
import org.ucb.cashin.model.PeriodoDesafio
import org.ucb.cashin.model.ProgresoDesafio
import java.time.LocalDate

private const val CODIGO_COLUMNA_INEXISTENTE = "42703"

/**
 * Módulo M8 · Desafíos (HU-22, HU-23). La app solo LEE la tabla `desafio`;
 * asignarlos, descartarlos y evaluarlos lo hacen funciones del servidor
 * (003_gamificacion.sql y 004_desafios_automaticos.sql), que también ponen las fechas y el XP.
 */
class DesafioRepository(private val supabase: SupabaseClient) {

    /** Desafíos en curso (hasta 3 semanales y 1 mensual). */
    suspend fun listarActivos(): Result<List<Desafio>> = llamada {
        supabase.from("desafio").select {
            filter { eq("estado", EstadoDesafio.ACTIVO.name) }
            order("fecha_fin", Order.ASCENDING)
        }.decodeList<Desafio>()
    }

    /** Últimos 20 desafíos terminados (cumplidos o fallidos), del más reciente al más antiguo. */
    suspend fun listarHistorial(): Result<List<Desafio>> = llamada {
        supabase.from("desafio").select {
            filter { isIn("estado", listOf(EstadoDesafio.CUMPLIDO.name, EstadoDesafio.FALLIDO.name)) }
            order("creado_en", Order.DESCENDING)
            limit(20)
        }.decodeList<Desafio>()
    }

    /** Cuánto se gastó, anotó, ahorró… en cada desafío activo (para las barras). */
    suspend fun progreso(): Result<List<ProgresoDesafio>> = llamada {
        supabase.postgrest.rpc("progreso_desafios").decodeList<ProgresoDesafio>()
    }

    /**
     * HU-22: guarda los desafíos sorteados para el periodo en curso (rpc asignar_desafios).
     * El servidor pone fechas y XP y solo los guarda si en este periodo todavía no hubo asignación,
     * así que llamarla dos veces no duplica nada. Devuelve los desafíos nuevos (vacío si ya estaban).
     */
    suspend fun asignar(periodo: PeriodoDesafio, desafios: List<DesafioAsignado>): Result<List<Desafio>> = llamada {
        supabase.postgrest.rpc("asignar_desafios", ParametrosAsignarDesafios(periodo, desafios))
            .decodeList<Desafio>()
    }

    /** Desafíos asignados por el sistema desde [desde] (cualquier estado): para saber si ya llegaron los del periodo. */
    suspend fun listarAutomaticosDesde(desde: LocalDate): Result<List<Desafio>> = llamada {
        supabase.from("desafio").select {
            filter {
                eq("automatico", true)
                gte("fecha_inicio", desde.toString())
            }
        }.decodeList<Desafio>()
    }

    /**
     * Si el servidor ya tiene la migración 006 (los retos de límite no cuentan los pagos fijos).
     * Hasta entonces se sigue generando con la regla anterior, para que límite y medición coincidan.
     */
    suspend fun midenSinFijos(): Result<Boolean> = llamada {
        try {
            supabase.from("desafio").select(Columns.list("categorias_excluidas")) { limit(1) }
            true
        } catch (e: PostgrestRestException) {
            if (e.code == CODIGO_COLUMNA_INEXISTENTE) false else throw e
        }
    }

    /** Descarta un desafío activo. Devuelve false si ya no estaba activo. */
    suspend fun descartar(id: String): Result<Boolean> = llamada {
        supabase.postgrest.rpc("descartar_desafio", buildJsonObject { put("p_id", id) })
            .decodeAs<Boolean>()
    }

    /**
     * HU-23: cierra los desafíos que corresponda (CUMPLIDO o FALLIDO) y suma el XP de los cumplidos.
     * Devuelve solo los que cambiaron de estado. Se llama al abrir Inicio y Desafíos.
     */
    suspend fun evaluar(): Result<List<Desafio>> = llamada {
        supabase.postgrest.rpc("evaluar_desafios").decodeList<Desafio>()
    }
}
