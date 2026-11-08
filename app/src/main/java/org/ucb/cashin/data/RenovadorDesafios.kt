package org.ucb.cashin.data

import android.content.res.Resources
import org.ucb.cashin.R
import org.ucb.cashin.model.AporteMeta
import org.ucb.cashin.model.Desafio
import org.ucb.cashin.model.DesafioAsignado
import org.ucb.cashin.model.EstadoMeta
import org.ucb.cashin.model.PeriodoDesafio
import org.ucb.cashin.model.Registro
import org.ucb.cashin.model.ReglaDesafio
import org.ucb.cashin.model.TipoRegistro
import org.ucb.cashin.prediccion.IMPORTANCIA_SIN_RETOS
import org.ucb.cashin.prediccion.cuentaParaLimites
import org.ucb.cashin.prediccion.desglosarGastos
import org.ucb.cashin.prediccion.HistoriaDesafios
import org.ucb.cashin.prediccion.MedicionPeriodo
import org.ucb.cashin.prediccion.PlantillaDesafio
import org.ucb.cashin.prediccion.PropuestaDesafio
import org.ucb.cashin.prediccion.finDelPeriodo
import org.ucb.cashin.prediccion.inicioDelPeriodo
import org.ucb.cashin.prediccion.semillaDe
import org.ucb.cashin.prediccion.sortearDesafios
import org.ucb.cashin.util.Formato
import java.math.BigDecimal

/**
 * HU-22: los desafíos llegan solos. Al abrir la app revisa si ya llegaron los de esta semana
 * (3 semanales) y los de este mes (1 mensual); si no, los sortea con el motor
 * (prediccion/GeneradorDesafios.kt) y los guarda con `asignar_desafios`, que solo los acepta
 * una vez por periodo. Así cada cuenta tiene los suyos y no cambian al salir y volver a entrar.
 *
 * Usa [recursos] solo para escribir el título y la descripción con las plantillas de strings.xml
 * (quedan guardados en el servidor). Lo usan InicioViewModel y DesafiosViewModel.
 */
class RenovadorDesafios(
    private val authRepository: AuthRepository,
    private val desafioRepository: DesafioRepository,
    private val resumenRepository: ResumenRepository,
    private val registroRepository: RegistroRepository,
    private val metaRepository: MetaRepository,
    private val planificadoRepository: PlanificadoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val recursos: Resources,
) {

    /**
     * Asigna lo que falte del periodo en curso: primero el mensual y después los semanales
     * (que no repiten el reto del mensual). Devuelve los desafíos nuevos; vacío si ya estaban
     * o si algo falló (se reintenta en la próxima carga, sin romper la pantalla).
     */
    suspend fun renovar(): List<Desafio> {
        val usuarioId = authRepository.usuarioIdActual() ?: return emptyList()
        val hoy = Formato.hoy()
        val desde = minOf(inicioDelPeriodo(PeriodoDesafio.SEMANAL, hoy), inicioDelPeriodo(PeriodoDesafio.MENSUAL, hoy))

        val asignados = desafioRepository.listarAutomaticosDesde(desde).getOrElse { return emptyList() }
        val pendientes = listOf(PeriodoDesafio.MENSUAL, PeriodoDesafio.SEMANAL).filter { periodo ->
            asignados.none { it.periodo == periodo && it.fechaInicio == inicioDelPeriodo(periodo, hoy) }
        }
        if (pendientes.isEmpty()) return emptyList()

        val gastoPorCategoria = resumenRepository.gastoPorCategoria().getOrDefault(emptyList())

        // Con la migración 006, los límites no consideran pagos fijos ni categorías de importancia alta.
        // Si algo de esto falla no se asigna con otra regla: se reintenta en la próxima carga.
        val sinFijos = desafioRepository.midenSinFijos().getOrElse { return emptyList() }
        val importancias = if (sinFijos) {
            categoriaRepository.listarConArchivadas(TipoRegistro.GASTO).getOrElse { return emptyList() }
                .associate { it.id to it.importancia }
        } else {
            null
        }
        val desglose = importancias?.let {
            desglosarGastos(planificadoRepository.listar().getOrElse { return emptyList() }, gastoPorCategoria, it, hoy)
        }
        val excluidas = importancias?.filterValues { it >= IMPORTANCIA_SIN_RETOS }?.keys

        val historia = HistoriaDesafios(
            gastoPorCategoria = gastoPorCategoria,
            totales = resumenRepository.historialMensual().getOrDefault(emptyList()),
            tieneMetasActivas = metaRepository.listarAvances().getOrDefault(emptyList())
                .any { it.estado == EstadoMeta.ACTIVA && it.totalAhorrado < it.montoObjetivo },
            desglose = desglose,
        )
        val registros = registroRepository.listarDesde(desde).getOrElse { return emptyList() }
        val ahorros = metaRepository.listarAhorrosDesde(desde).getOrDefault(emptyList())
        val activos = desafioRepository.listarActivos().getOrDefault(emptyList()).toMutableList()

        val nuevos = mutableListOf<Desafio>()
        for (periodo in pendientes) {
            val propuestas = sortearDesafios(
                periodo = periodo,
                semilla = semillaDe(usuarioId, periodo, inicioDelPeriodo(periodo, hoy)),
                hoy = hoy,
                historia = historia,
                medicion = medir(periodo, registros, ahorros, excluidas),
                evitar = activos,
            )
            if (propuestas.isEmpty()) continue
            desafioRepository.asignar(periodo, propuestas.map { it.conTextos() }).onSuccess {
                nuevos += it
                activos += it
            }
        }
        return nuevos
    }

    /** Lo que ya se hizo en el periodo en curso: igual que lo mide el servidor (fn_medir_desafio). */
    private fun medir(
        periodo: PeriodoDesafio,
        registros: List<Registro>,
        ahorros: List<AporteMeta>,
        excluidas: Set<String>?,
    ): MedicionPeriodo {
        val hoy = Formato.hoy()
        val inicio = inicioDelPeriodo(periodo, hoy)
        val fin = finDelPeriodo(periodo, hoy)
        val gastos = registros.filter { it.tipo == TipoRegistro.GASTO && Formato.fechaDe(it.fechaHora) in inicio..fin }
        val medibles = if (excluidas == null) gastos else gastos.filter { cuentaParaLimites(it.planificadoId, it.categoriaId, excluidas) }
        // Días y movimientos se cuentan por cuándo se anotaron (no se puede "anotar en el pasado").
        val anotados = registros.map { Formato.fechaDe(it.creadoEn ?: it.fechaHora) }.filter { it in inicio..fin }
        val diasConGasto = gastos.map { Formato.fechaDe(it.fechaHora) }.toSet()
        val diasTerminados = generateSequence(inicio) { it.plusDays(1) }.takeWhile { it < hoy && it <= fin }

        return MedicionPeriodo(
            gastoPorCategoria = medibles.groupBy { it.categoriaId }.mapValues { (_, r) -> r.sumOf { it.monto } },
            gastoTotal = medibles.sumOf { it.monto },
            diasConRegistro = anotados.distinct().size,
            cantidadRegistros = anotados.size,
            diasSinGasto = diasTerminados.count { it !in diasConGasto },
            ahorros = ahorros.count { it.fecha in inicio..fin },
        )
    }

    /** Título y descripción con las plantillas de strings.xml (no IA generativa). */
    private fun PropuestaDesafio.conTextos(): DesafioAsignado {
        val cuando = recursos.getString(
            if (periodo == PeriodoDesafio.SEMANAL) R.string.desafios_esta_semana else R.string.desafios_este_mes,
        )
        val limite = Formato.monto(montoLimite ?: BigDecimal.ZERO)
        val costumbre = Formato.montoAproximado(basadoEn ?: BigDecimal.ZERO)
        val objetivo = diasObjetivo ?: 0
        val nombre = categoriaNombre.orEmpty()
        val dificil = plantilla?.dificil == true

        val (titulo, descripcion) = when (regla) {
            ReglaDesafio.LIMITE_CATEGORIA ->
                recursos.getString(if (dificil) R.string.desafio_titulo_categoria_duro else R.string.desafio_titulo_categoria, nombre) to
                    recursos.getString(R.string.desafio_desc_categoria, limite, nombre, cuando, costumbre)
            ReglaDesafio.LIMITE_TOTAL ->
                recursos.getString(if (dificil) R.string.desafio_titulo_total_duro else R.string.desafio_titulo_total) to
                    recursos.getString(R.string.desafio_desc_total, limite, cuando, costumbre)
            ReglaDesafio.DIAS_REGISTRO ->
                (if (plantilla == PlantillaDesafio.S_DIAS_7) recursos.getString(R.string.desafio_titulo_semana_perfecta)
                else recursos.getQuantityString(R.plurals.desafio_titulo_dias, objetivo, objetivo)) to
                    recursos.getString(R.string.desafio_desc_dias, objetivo, cuando)
            ReglaDesafio.CANTIDAD_REGISTROS ->
                recursos.getString(R.string.desafio_titulo_movimientos, objetivo) to
                    recursos.getString(R.string.desafio_desc_movimientos, objetivo, cuando)
            ReglaDesafio.DIAS_SIN_GASTO ->
                recursos.getString(R.string.desafio_titulo_sin_gasto, objetivo) to
                    recursos.getString(R.string.desafio_desc_sin_gasto, objetivo, cuando)
            ReglaDesafio.AHORRO_META ->
                recursos.getQuantityString(R.plurals.desafio_titulo_ahorro, objetivo, objetivo) to
                    recursos.getQuantityString(R.plurals.desafio_desc_ahorro, objetivo, objetivo, cuando)
        }

        return DesafioAsignado(
            titulo = titulo,
            descripcion = descripcion,
            regla = regla,
            categoriaId = categoriaId,
            montoLimite = montoLimite,
            objetivo = diasObjetivo,
            plantilla = plantilla?.name,
        )
    }
}
