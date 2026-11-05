package org.ucb.cashin.prediccion

import org.ucb.cashin.model.PeriodoDesafio
import org.ucb.cashin.model.ReglaDesafio
import java.math.BigDecimal

/**
 * HU-22: un desafío que el motor propone y la persona puede aceptar.
 * Título y descripción no van aquí: la pantalla los arma con plantillas de strings.xml.
 */
data class PropuestaDesafio(
    val periodo: PeriodoDesafio,
    val regla: ReglaDesafio,
    val categoriaId: String? = null,
    val categoriaNombre: String? = null,
    val montoLimite: BigDecimal? = null,
    val diasObjetivo: Int? = null,
    /** Promedio o pronóstico que se usó (para explicarlo en pantalla). */
    val basadoEn: BigDecimal? = null,
)
