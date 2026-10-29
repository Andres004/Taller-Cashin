package org.ucb.cashin.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import org.ucb.cashin.model.TotalesMes
import org.ucb.cashin.util.Formato

/**
 * Totales calculados por la base de datos. La app nunca guarda saldos:
 * siempre los lee de las vistas.
 */
class ResumenRepository(private val supabase: SupabaseClient) {

    /** Gastos e ingresos del mes actual. Si todavía no hay anotaciones este mes, devuelve null. */
    suspend fun totalesDelMes(): Result<TotalesMes?> = llamada {
        val primerDiaDelMes = Formato.hoy().withDayOfMonth(1)
        supabase.from("v_totales_mes").select {
            filter { eq("mes", primerDiaDelMes.toString()) }
        }.decodeSingleOrNull<TotalesMes>()
    }

    /**
     * Todos los meses con anotaciones, del más antiguo al más reciente.
     * Es la historia que usa el motor de predicción (paquete prediccion/).
     */
    suspend fun historialMensual(): Result<List<TotalesMes>> = llamada {
        supabase.from("v_totales_mes").select {
            order("mes", Order.ASCENDING)
        }.decodeList<TotalesMes>()
    }
}