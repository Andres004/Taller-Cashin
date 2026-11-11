package org.ucb.cashin.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import org.ucb.cashin.model.IngresoCategoriaMes

/**
 * Datos de los reportes de Historial (HU-14) que no están en [ResumenRepository].
 * Solo lee vistas: los cálculos (porcentajes, comparación con el mes anterior, frases de Cashi)
 * están en prediccion/Reportes.kt.
 */
class ReporteRepository(private val supabase: SupabaseClient) {

    /**
     * Ingreso por categoría de todos los meses (vista de la migración 008), del más antiguo al más
     * reciente. Son pocas filas (categorías × meses), así que se lee una vez y cambiar de mes es inmediato.
     */
    suspend fun ingresoPorCategoria(): Result<List<IngresoCategoriaMes>> = llamada {
        supabase.from("v_ingreso_categoria_mes").select {
            order("mes", Order.ASCENDING)
        }.decodeList<IngresoCategoriaMes>()
    }
}
