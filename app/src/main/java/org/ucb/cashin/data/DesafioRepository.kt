package org.ucb.cashin.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.ucb.cashin.model.Desafio
import org.ucb.cashin.model.EstadoDesafio
import org.ucb.cashin.model.ProgresoDesafio
import org.ucb.cashin.prediccion.PropuestaDesafio

/**
 * Módulo M8 · Desafíos (HU-22, HU-23). La app solo LEE la tabla `desafio`;
 * crearlos, descartarlos y evaluarlos lo hacen funciones del servidor (003_gamificacion.sql),
 * que también ponen las fechas y el XP.
 */
class DesafioRepository(private val supabase: SupabaseClient) {

    /** Desafíos en curso (como máximo uno semanal y uno mensual). */
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

    /** Cuánto se gastó y cuántos días se anotó en cada desafío activo (para las barras). */
    suspend fun progreso(): Result<List<ProgresoDesafio>> = llamada {
        supabase.postgrest.rpc("progreso_desafios").decodeList<ProgresoDesafio>()
    }

    /**
     * HU-22: acepta una propuesta del motor. [titulo] y [descripcion] los arma la pantalla
     * con plantillas de strings.xml. Los parámetros que no aplican a la regla van como null.
     * El servidor rechaza si ya hay un desafío activo del mismo periodo.
     */
    suspend fun aceptar(propuesta: PropuestaDesafio, titulo: String, descripcion: String): Result<Desafio> = llamada {
        val parametros = buildJsonObject {
            put("p_titulo", titulo)
            put("p_descripcion", descripcion)
            put("p_periodo", propuesta.periodo.name)
            put("p_regla", propuesta.regla.name)
            // put(clave, null) envía un null explícito en el JSON
            put("p_categoria_id", propuesta.categoriaId)
            put("p_monto_limite", propuesta.montoLimite)
            put("p_dias_objetivo", propuesta.diasObjetivo)
        }
        supabase.postgrest.rpc("aceptar_desafio", parametros).decodeAs<Desafio>()
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
