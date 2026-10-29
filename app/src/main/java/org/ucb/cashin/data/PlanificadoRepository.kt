package org.ucb.cashin.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.ucb.cashin.model.AvancePlanificado
import org.ucb.cashin.model.EstadoDePlanificado
import org.ucb.cashin.model.EstadoPlanificado
import org.ucb.cashin.model.NuevoPlanificado
import org.ucb.cashin.model.NuevoRegistro
import org.ucb.cashin.model.Registro
import org.ucb.cashin.model.RegistroPlanificado
import org.ucb.cashin.model.siguienteFecha

/**
 * Módulo M5 · Planificados (HU-15, HU-16): pagos e ingresos esperados, como el alquiler o el sueldo.
 * Son solo anotaciones: Cashin no paga ni cobra nada.
 */
class PlanificadoRepository(private val supabase: SupabaseClient) {

    /** Los que siguen vigentes, del más próximo al más lejano. Los cancelados no se muestran. */
    suspend fun listar(): Result<List<RegistroPlanificado>> = llamada {
        supabase.from("registro_planificado").select {
            filter { neq("estado", EstadoPlanificado.CANCELADO.name) }
            order("proxima_fecha", Order.ASCENDING)
        }.decodeList<RegistroPlanificado>()
    }

    /** HU-15: anota un pago o ingreso esperado. */
    suspend fun crear(nuevo: NuevoPlanificado): Result<RegistroPlanificado> = llamada {
        supabase.from("registro_planificado").insert(nuevo) {
            select()
        }.decodeSingle<RegistroPlanificado>()
    }

    /**
     * HU-16: "Ya lo pagué / cobré". Un periodo solo se puede marcar UNA vez:
     *
     * 1. Mueve la próxima fecha (o cierra si era UNICA), pero SOLO si en la base de datos
     *    la fecha sigue siendo la que el usuario vio. Si otro toque (u otro celular) ya la
     *    movió, no se cambia nada y se devuelve null: ese periodo ya estaba anotado.
     * 2. Crea la anotación en `registro` con `planificado_id`.
     * 3. Si la anotación falla, devuelve la fecha a como estaba, para no perder ese periodo.
     */
    suspend fun registrarPago(planificado: RegistroPlanificado): Result<Registro?> = llamada {
        val siguiente = planificado.frecuencia.siguienteFecha(planificado.proximaFecha)
        val avance = if (siguiente == null) {
            AvancePlanificado(proximaFecha = planificado.proximaFecha, estado = EstadoPlanificado.CANCELADO)
        } else {
            AvancePlanificado(proximaFecha = siguiente, estado = EstadoPlanificado.ACTIVO)
        }

        val movidos = supabase.from("registro_planificado").update(avance) {
            filter {
                eq("id", planificado.id)
                eq("proxima_fecha", planificado.proximaFecha.toString())
                eq("estado", EstadoPlanificado.ACTIVO.name)
            }
            select()
        }.decodeList<RegistroPlanificado>()

        if (movidos.isEmpty()) return@llamada null

        try {
            supabase.from("registro").insert(
                NuevoRegistro(
                    categoriaId = planificado.categoriaId,
                    tipo = planificado.tipo,
                    monto = planificado.monto,
                    medioPago = planificado.medioPago,
                    nota = planificado.nombre,
                    planificadoId = planificado.id,
                ),
            ) {
                select()
            }.decodeSingle<Registro>()
        } catch (e: Throwable) {
            withContext(NonCancellable) {
                runCatching {
                    supabase.from("registro_planificado").update(
                        AvancePlanificado(proximaFecha = planificado.proximaFecha, estado = EstadoPlanificado.ACTIVO),
                    ) {
                        filter { eq("id", planificado.id) }
                    }
                }
            }
            throw e
        }
    }

    /**
     * Quita un planificado de la lista. No se borra: queda CANCELADO para que
     * las anotaciones que ya generó sigan apuntando a él.
     */
    suspend fun quitar(id: String): Result<Unit> = llamada {
        supabase.from("registro_planificado").update(EstadoDePlanificado(EstadoPlanificado.CANCELADO)) {
            filter { eq("id", id) }
        }
        Unit
    }
}