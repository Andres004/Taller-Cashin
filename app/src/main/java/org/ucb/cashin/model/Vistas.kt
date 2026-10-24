@file:UseSerializers(BigDecimalSerializer::class, LocalDateSerializer::class)

package org.ucb.cashin.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.ucb.cashin.util.BigDecimalSerializer
import org.ucb.cashin.util.LocalDateSerializer
import java.math.BigDecimal
import java.time.LocalDate

/* Una data class por vista. Los totales SIEMPRE se leen de estas vistas (no se guardan saldos). */

/** Vista `v_totales_mes`. `mes` es el primer día del mes (hora Bolivia). */
@Serializable
data class TotalesMes(
    @SerialName("usuario_id") val usuarioId: String,
    val mes: LocalDate,
    @SerialName("total_gastos") val totalGastos: BigDecimal,
    @SerialName("total_ingresos") val totalIngresos: BigDecimal,
    @SerialName("cantidad_registros") val cantidadRegistros: Int,
) {
    /** Ingresos − gastos del mes. Es solo una diferencia de anotaciones, no un saldo. */
    val diferencia: BigDecimal get() = totalIngresos - totalGastos
}

/** Vista `v_gasto_categoria_mes`. */
@Serializable
data class GastoCategoriaMes(
    @SerialName("usuario_id") val usuarioId: String,
    val mes: LocalDate,
    @SerialName("categoria_id") val categoriaId: String,
    val nombre: String,
    val icono: String? = null,
    val color: String? = null,
    val total: BigDecimal,
)

/** Vista `v_avance_meta`. */
@Serializable
data class AvanceMeta(
    @SerialName("meta_id") val metaId: String,
    @SerialName("usuario_id") val usuarioId: String,
    val nombre: String,
    @SerialName("monto_objetivo") val montoObjetivo: BigDecimal,
    @SerialName("fecha_limite") val fechaLimite: LocalDate? = null,
    val estado: EstadoMeta,
    @SerialName("total_ahorrado") val totalAhorrado: BigDecimal,
) {
    /** Avance entre 0 y 1 para la barra de progreso. */
    val progreso: Float
        get() = if (montoObjetivo.signum() <= 0) 0f
        else (totalAhorrado.toFloat() / montoObjetivo.toFloat()).coerceIn(0f, 1f)
}

/** Vista `v_deuda`. */
@Serializable
data class ResumenDeuda(
    @SerialName("deuda_id") val deudaId: String,
    @SerialName("usuario_id") val usuarioId: String,
    val tipo: TipoDeuda,
    val contraparte: String,
    @SerialName("monto_total") val montoTotal: BigDecimal,
    @SerialName("fecha_limite") val fechaLimite: LocalDate? = null,
    val estado: EstadoDeuda,
    val pendiente: BigDecimal,
)
