package org.ucb.cashin.ui.metas

import org.ucb.cashin.data.CategoriaRepository
import org.ucb.cashin.data.PlanificadoRepository
import org.ucb.cashin.data.ResumenRepository
import org.ucb.cashin.model.AporteMeta
import org.ucb.cashin.model.AvanceMeta
import org.ucb.cashin.model.Categoria
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.prediccion.DesgloseGastos
import org.ucb.cashin.prediccion.PlanAhorro
import org.ucb.cashin.prediccion.Pronostico
import org.ucb.cashin.prediccion.construirSerie
import org.ucb.cashin.prediccion.desglosarGastos
import org.ucb.cashin.prediccion.planificarAhorro
import org.ucb.cashin.prediccion.pronosticarSiguienteMes
import org.ucb.cashin.util.Formato
import java.time.LocalDate

/** Lo que el motor necesita saber de las finanzas de la persona para armar el camino de una meta. */
internal class DatosPlan(
    val ingresos: Pronostico,
    val gastos: Pronostico,
    val desglose: DesgloseGastos,
    val categorias: List<Categoria>,
    val hoy: LocalDate,
) {
    fun planPara(avance: AvanceMeta, aportes: List<AporteMeta>): PlanAhorro =
        planificarAhorro(avance, aportes, ingresos, gastos, desglose, hoy)
}

/** Si algo no se puede leer, el plan se calcula con lo que haya: en el peor caso queda "sin datos". */
internal suspend fun cargarDatosPlan(
    resumenRepository: ResumenRepository,
    planificadoRepository: PlanificadoRepository,
    categoriaRepository: CategoriaRepository,
): DatosPlan {
    val historial = resumenRepository.historialMensual().getOrDefault(emptyList())
    val hoy = Formato.hoy()
    val categorias = categoriaRepository.listarConArchivadas(TipoRegistro.GASTO).getOrDefault(emptyList())
    return DatosPlan(
        ingresos = pronosticarSiguienteMes(construirSerie(historial, hoy) { it.totalIngresos }),
        gastos = pronosticarSiguienteMes(construirSerie(historial, hoy) { it.totalGastos }),
        desglose = desglosarGastos(
            planificados = planificadoRepository.listar().getOrDefault(emptyList()),
            gastoPorCategoria = resumenRepository.gastoPorCategoria().getOrDefault(emptyList()),
            importancias = categorias.associate { it.id to it.importancia },
            hoy = hoy,
        ),
        categorias = categorias,
        hoy = hoy,
    )
}
