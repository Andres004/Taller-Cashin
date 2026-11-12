package org.ucb.cashin.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.ucb.cashin.model.AbonoDeuda
import org.ucb.cashin.model.AumentoDeuda
import org.ucb.cashin.model.CambiosDeuda
import org.ucb.cashin.model.Deuda
import org.ucb.cashin.model.NuevaDeuda
import org.ucb.cashin.model.NuevoAbonoDeuda
import org.ucb.cashin.model.NuevoAumentoDeuda
import org.ucb.cashin.model.ResumenDeuda
import org.ucb.cashin.util.LocalDateSerializer
import java.time.LocalDate

/** Los aumentos y abonos de una deuda (el inicio está en la deuda misma). */
data class MovimientosGuardados(
    val aumentos: List<AumentoDeuda>,
    val abonos: List<AbonoDeuda>,
)

/** Para poner, cambiar o quitar la fecha límite al prestar más. */
@Serializable
private data class CambioFechaLimite(
    @Serializable(with = LocalDateSerializer::class)
    @SerialName("fecha_limite") val fechaLimite: LocalDate?,
)

/**
 * Módulo Deudas (HU-25, HU-26). Cashin no mueve dinero: solo anota lo que se debe.
 * El total, lo pendiente y el estado (PAGADA / PENDIENTE) los calcula la base de datos
 * (migración 009); la app solo escribe deudas, aumentos y abonos.
 */
class DeudaRepository(private val supabase: SupabaseClient) {

    /** Todas las deudas con su resumen (vista v_deuda). El orden lo decide la pantalla. */
    suspend fun listar(): Result<List<ResumenDeuda>> = llamada {
        supabase.from("v_deuda").select().decodeList<ResumenDeuda>()
    }

    /** Una deuda con su resumen. */
    suspend fun obtener(deudaId: String): Result<ResumenDeuda> = llamada {
        supabase.from("v_deuda").select {
            filter { eq("deuda_id", deudaId) }
        }.decodeSingle<ResumenDeuda>()
    }

    /** Crea la deuda. Lo que se envía como monto_total queda como monto inicial. */
    suspend fun crear(nueva: NuevaDeuda): Result<Deuda> = llamada {
        supabase.from("deuda").insert(nueva) {
            select()
        }.decodeSingle<Deuda>()
    }

    suspend fun editar(deudaId: String, cambios: CambiosDeuda): Result<Unit> = llamada {
        supabase.from("deuda").update(cambios) {
            filter { eq("id", deudaId) }
        }
        Unit
    }

    /** Pone, cambia o quita (null) la fecha límite. */
    suspend fun cambiarFechaLimite(deudaId: String, fecha: LocalDate?): Result<Unit> = llamada {
        supabase.from("deuda").update(CambioFechaLimite(fecha)) {
            filter { eq("id", deudaId) }
        }
        Unit
    }

    /** Borra la deuda y, en cascada, todos sus movimientos. */
    suspend fun eliminar(deudaId: String): Result<Unit> = llamada {
        supabase.from("deuda").delete {
            filter { eq("id", deudaId) }
        }
        Unit
    }

    suspend fun movimientos(deudaId: String): Result<MovimientosGuardados> = llamada {
        val aumentos = supabase.from("aumento_deuda").select {
            filter { eq("deuda_id", deudaId) }
            order("fecha", Order.ASCENDING)
        }.decodeList<AumentoDeuda>()
        val abonos = supabase.from("abono_deuda").select {
            filter { eq("deuda_id", deudaId) }
            order("fecha", Order.ASCENDING)
        }.decodeList<AbonoDeuda>()
        MovimientosGuardados(aumentos, abonos)
    }

    suspend fun anotarAbono(nuevo: NuevoAbonoDeuda): Result<AbonoDeuda> = llamada {
        supabase.from("abono_deuda").insert(nuevo) {
            select()
        }.decodeSingle<AbonoDeuda>()
    }

    suspend fun anotarAumento(nuevo: NuevoAumentoDeuda): Result<AumentoDeuda> = llamada {
        supabase.from("aumento_deuda").insert(nuevo) {
            select()
        }.decodeSingle<AumentoDeuda>()
    }

    /** Solo el último movimiento se puede borrar: la base de datos rechaza los demás. */
    suspend fun borrarAbono(abonoId: String): Result<Unit> = llamada {
        supabase.from("abono_deuda").delete {
            filter { eq("id", abonoId) }
        }
        Unit
    }

    suspend fun borrarAumento(aumentoId: String): Result<Unit> = llamada {
        supabase.from("aumento_deuda").delete {
            filter { eq("id", aumentoId) }
        }
        Unit
    }
}
