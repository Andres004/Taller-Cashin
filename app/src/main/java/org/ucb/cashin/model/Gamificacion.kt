@file:UseSerializers(BigDecimalSerializer::class)

package org.ucb.cashin.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.ucb.cashin.util.BigDecimalSerializer
import java.math.BigDecimal

/* Datos de las funciones de gamificación (003_gamificacion.sql y 004_desafios_automaticos.sql). */

/** Una fila de `progreso_desafios()`: cuánto va de un desafío ACTIVO (para su barra). */
@Serializable
data class ProgresoDesafio(
    @SerialName("desafio_id") val desafioId: String,
    val gastado: BigDecimal,
    @SerialName("dias_con_registro") val diasConRegistro: Int,
    @SerialName("cantidad_registros") val cantidadRegistros: Int = 0,
    /** Días ya terminados del periodo sin ningún gasto. */
    @SerialName("dias_sin_gasto") val diasSinGasto: Int = 0,
    /** Ahorros anotados en metas durante el periodo. */
    val ahorros: Int = 0,
)

/**
 * Un desafío que la app manda a `asignar_desafios()` (004_desafios_automaticos.sql).
 * [objetivo]: días, movimientos o ahorros (reglas que cuentan algo); [plantilla]: PlantillaDesafio.name.
 */
@Serializable
data class DesafioAsignado(
    val titulo: String,
    val descripcion: String,
    val regla: ReglaDesafio,
    @SerialName("categoria_id") val categoriaId: String? = null,
    @SerialName("monto_limite") val montoLimite: BigDecimal? = null,
    val objetivo: Int? = null,
    val plantilla: String? = null,
)

/** Parámetros de la rpc `asignar_desafios(p_periodo, p_desafios)`. */
@Serializable
data class ParametrosAsignarDesafios(
    @SerialName("p_periodo") val periodo: PeriodoDesafio,
    @SerialName("p_desafios") val desafios: List<DesafioAsignado>,
)
