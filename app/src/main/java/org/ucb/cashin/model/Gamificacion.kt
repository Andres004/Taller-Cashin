@file:UseSerializers(BigDecimalSerializer::class)

package org.ucb.cashin.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.ucb.cashin.util.BigDecimalSerializer
import java.math.BigDecimal

/* Resultados de las funciones de gamificación (003_gamificacion.sql). */

/** Una fila de `progreso_desafios()`: cuánto va de un desafío ACTIVO (para su barra). */
@Serializable
data class ProgresoDesafio(
    @SerialName("desafio_id") val desafioId: String,
    val gastado: BigDecimal,
    @SerialName("dias_con_registro") val diasConRegistro: Int,
)
