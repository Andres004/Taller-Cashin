package org.ucb.cashin.prediccion

import org.ucb.cashin.model.AvanceMeta
import org.ucb.cashin.model.EstadoMeta
import java.time.LocalDate

/**
 * La meta que se muestra en Inicio: primero la que ya llegó y falta marcar (tiene +100 XP pendientes);
 * si no hay, la más avanzada y, si empatan, la de fecha límite más cercana.
 */
fun elegirMetaPrincipal(metas: List<AvanceMeta>): AvanceMeta? {
    val activas = metas.filter { it.estado == EstadoMeta.ACTIVA }
    return activas.firstOrNull { it.totalAhorrado >= it.montoObjetivo }
        ?: activas.sortedWith(
            compareByDescending<AvanceMeta> { it.progreso }.thenBy { it.fechaLimite ?: LocalDate.MAX },
        ).firstOrNull()
}

/**
 * Hasta [maximo] pasos para el camino compacto de Inicio, sin contar el trofeo: dos hechos, el actual
 * y lo que siga. Si no quedan pendientes, se completa con más pasos hechos.
 */
fun PlanAhorro.pasosResumidos(maximo: Int = 4): List<PasoAhorro> {
    if (estado == EstadoPlan.SIN_DATOS) return emptyList()
    val filas = if (estado == EstadoPlan.LOGRADA) pasos else pasos.filterNot { it.esMeta }
    val actual = filas.indexOfFirst { it.estado == EstadoPaso.ACTUAL }
    val ancla = if (actual >= 0) actual else filas.lastIndex
    val desde = (ancla - 2).coerceIn(0, (filas.size - maximo).coerceAtLeast(0))
    return filas.drop(desde).take(maximo)
}
