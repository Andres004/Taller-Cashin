package org.ucb.cashin.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ucb.cashin.data.CategoriaRepository
import org.ucb.cashin.data.DesafioRepository
import org.ucb.cashin.data.MetaRepository
import org.ucb.cashin.data.PerfilRepository
import org.ucb.cashin.data.RegistroRepository
import org.ucb.cashin.data.RenovadorDesafios
import org.ucb.cashin.data.ResumenRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.Desafio
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.EstadoDesafio
import org.ucb.cashin.model.EstadoMeta
import org.ucb.cashin.model.ProgresoDesafio
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.prediccion.Confianza
import org.ucb.cashin.prediccion.MensajeAvatar
import org.ucb.cashin.prediccion.Pronostico
import org.ucb.cashin.prediccion.construirSerie
import org.ucb.cashin.prediccion.consejoDelDia
import org.ucb.cashin.prediccion.pronosticarProximoMes
import org.ucb.cashin.prediccion.pronosticarSiguienteMes
import org.ucb.cashin.prediccion.proyectarMesActual
import org.ucb.cashin.ui.registros.RegistroConCategoria
import org.ucb.cashin.ui.registros.unirConCategorias
import org.ucb.cashin.util.Formato
import java.math.BigDecimal
import java.math.RoundingMode

/** Todo lo que la pantalla de Inicio necesita para dibujarse. */
data class InicioUiState(
    val nombre: String = "",
    val totalGastos: BigDecimal = BigDecimal.ZERO,
    val totalIngresos: BigDecimal = BigDecimal.ZERO,
    val racha: Int = 0,
    val anotoHoy: Boolean = false,
    val nivel: Int = 1,
    val xp: Int = 0,
    val progresoNivel: Float = 0f,
    val ultimas: List<RegistroConCategoria> = emptyList(),
    /** Pronóstico del gasto de este mes, hecho solo con los meses completos anteriores. */
    val pronosticoMes: Pronostico = Pronostico.SIN_DATOS,
    /** Cómo terminará este mes al ritmo actual. */
    val proyeccionMes: BigDecimal = BigDecimal.ZERO,
    /** Pronóstico del próximo mes (rango mínimo–máximo). */
    val pronosticoProximo: Pronostico = Pronostico.SIN_DATOS,
    /** La proyección del mes supera el máximo pronosticado: se gasta más rápido que de costumbre. */
    val gastaMasRapido: Boolean = false,
    /** HU-21: consejo del día de Cashi (prediccion/Consejos.kt). */
    val mensajeIa: MensajeAvatar? = null,
    /** Desafíos en curso, con su progreso (tarjeta "Desafío activo"). */
    val desafiosActivos: List<Desafio> = emptyList(),
    val progresoDesafios: Map<String, ProgresoDesafio> = emptyMap(),
    /** HU-23: desafíos que la evaluación cerró (cumplidos o fallidos) y todavía no se mostraron (animación). */
    val resultadosDesafios: List<Desafio> = emptyList(),
    /** HU-22: acaban de llegar los desafíos de la semana o del mes (aviso "¡Nuevos desafíos disponibles!"). */
    val hayDesafiosNuevos: Boolean = false,
    /** HU-24: nivel al que se acaba de subir (celebración); null si no subió. */
    val subioANivel: Int? = null,
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
) {
    /** Ingresos menos gastos del mes. Es solo una diferencia de anotaciones, no un saldo. */
    val diferencia: BigDecimal get() = totalIngresos - totalGastos

    /** Hay al menos un mes completo de historia para pronosticar. */
    val hayPronostico: Boolean get() = pronosticoMes.confianza != Confianza.SIN_DATOS

    /** HU-24: hoy todavía no anotó y tiene una racha que puede perder. */
    val rachaEnRiesgo: Boolean get() = !anotoHoy && racha > 0
}

/**
 * HU-13: resumen del mes, racha, XP y últimas anotaciones.
 * IA predictiva: proyección del gasto de este mes y pronóstico del próximo (paquete prediccion/).
 * HU-21: consejo del día. HU-22/23: asigna los desafíos nuevos y evalúa los activos al abrir.
 * HU-24: racha en riesgo y subida de nivel.
 */
class InicioViewModel(
    private val perfilRepository: PerfilRepository,
    private val resumenRepository: ResumenRepository,
    private val registroRepository: RegistroRepository,
    private val categoriaRepository: CategoriaRepository,
    private val desafioRepository: DesafioRepository,
    private val renovadorDesafios: RenovadorDesafios,
    private val metaRepository: MetaRepository,
    /** Último nivel mostrado por usuario en esta sesión de la app (vive en AppContainer). */
    private val ultimoNivelVisto: MutableMap<String, Int>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InicioUiState())
    val uiState: StateFlow<InicioUiState> = _uiState.asStateFlow()

    private var yaSeMostro = false

    /**
     * Carga todo el resumen. Se llama cada vez que se abre Inicio, así refleja
     * lo último que se anotó, editó o eliminó. Si ya hay datos, se actualizan sin parpadear.
     */
    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = !yaSeMostro, error = null) }

            // Antes del perfil: un desafío cumplido suma XP y el perfil ya debe traerlo.
            // Si la evaluación falla, Inicio igual se muestra (se reintenta en la próxima carga).
            val evaluados = desafioRepository.evaluar().getOrDefault(emptyList())
            // Si empezó la semana o el mes, llegan los desafíos nuevos (una sola vez por periodo).
            val llegaron = renovadorDesafios.renovar()

            val perfil = perfilRepository.obtener().getOrElse { e ->
                mostrarError(e)
                return@launch
            }
            val totales = resumenRepository.totalesDelMes().getOrElse { e ->
                mostrarError(e)
                return@launch
            }
            val categorias = categoriaRepository.listarTodas().getOrElse { e ->
                mostrarError(e)
                return@launch
            }
            val ultimas = registroRepository.listar(limite = CANTIDAD_ULTIMAS).getOrElse { e ->
                mostrarError(e)
                return@launch
            }

            // Lo que alimenta al motor y a las tarjetas extra: si falla, Inicio se muestra igual.
            val historial = resumenRepository.historialMensual().getOrDefault(emptyList())
            val gastoPorCategoria = resumenRepository.gastoPorCategoria().getOrDefault(emptyList())
            val activos = desafioRepository.listarActivos().getOrDefault(emptyList())
                .sortedBy { it.periodo.ordinal }
            val progreso = desafioRepository.progreso().getOrDefault(emptyList())
                .associateBy { it.desafioId }
            val metas = metaRepository.listarAvances().getOrDefault(emptyList())

            val hoy = Formato.hoy()
            val gastoDelMes = totales?.totalGastos ?: BigDecimal.ZERO
            val serieGastos = construirSerie(historial, hoy) { it.totalGastos }
            val pronosticoMes = pronosticarSiguienteMes(serieGastos)
            val proyeccion = proyectarMesActual(gastoDelMes, hoy, pronosticoMes)
            val pronosticoProximo = pronosticarProximoMes(serieGastos, proyeccion, hoy)

            // Para el consejo: el desafío con límite que va más cerca de pasarse, si hay;
            // y la meta activa más avanzada que todavía no llega al objetivo.
            val desafioDelConsejo = activos
                .filter { it.montoLimite != null }
                .maxByOrNull { d -> progreso[d.id]?.gastado?.divide(d.montoLimite, 4, RoundingMode.HALF_UP) ?: BigDecimal.ZERO }
            val metaMasCercana = metas
                .filter { it.estado == EstadoMeta.ACTIVA && it.totalAhorrado < it.montoObjetivo }
                .maxByOrNull { it.progreso }

            val racha = perfil.rachaVigente(hoy)
            val anotoHoy = perfil.anotoHoy(hoy)
            val consejo = consejoDelDia(
                hoy = hoy,
                anotoHoy = anotoHoy,
                racha = racha,
                gastoPorCategoria = gastoPorCategoria,
                proyeccionDelMes = proyeccion,
                pronosticoDelMes = pronosticoMes,
                desafioActivo = desafioDelConsejo,
                progreso = desafioDelConsejo?.let { progreso[it.id] },
                metaMasCercana = metaMasCercana,
            )

            // HU-24: la primera carga solo guarda el nivel; las siguientes celebran si subió.
            val nivelAnterior = ultimoNivelVisto[perfil.id]
            val subioANivel = perfil.nivel.takeIf { nivelAnterior != null && it > nivelAnterior }
            ultimoNivelVisto[perfil.id] = perfil.nivel

            yaSeMostro = true
            _uiState.update {
                it.copy(
                    nombre = perfil.nombre,
                    totalGastos = totales?.totalGastos ?: BigDecimal.ZERO,
                    totalIngresos = totales?.totalIngresos ?: BigDecimal.ZERO,
                    racha = racha,
                    anotoHoy = anotoHoy,
                    nivel = perfil.nivel,
                    xp = perfil.xp,
                    progresoNivel = perfil.progresoNivel,
                    ultimas = unirConCategorias(ultimas, categorias),
                    pronosticoMes = pronosticoMes,
                    proyeccionMes = proyeccion,
                    pronosticoProximo = pronosticoProximo,
                    gastaMasRapido = pronosticoMes.confianza != Confianza.SIN_DATOS && proyeccion > pronosticoMes.maximo,
                    mensajeIa = consejo,
                    desafiosActivos = activos,
                    progresoDesafios = progreso,
                    hayDesafiosNuevos = it.hayDesafiosNuevos || llegaron.isNotEmpty(),
                    resultadosDesafios = it.resultadosDesafios + evaluados.filter { d ->
                        d.estado == EstadoDesafio.CUMPLIDO || d.estado == EstadoDesafio.FALLIDO
                    },
                    subioANivel = subioANivel,
                    cargando = false,
                    error = null,
                )
            }
        }
    }

    /** La animación del primer desafío cerrado ya se vio: pasa al siguiente (si hay). */
    fun resultadoVisto() = _uiState.update { it.copy(resultadosDesafios = it.resultadosDesafios.drop(1)) }

    /** El aviso de desafíos nuevos ya se vio (se cerró o se abrió Desafíos). */
    fun avisoNuevosVisto() = _uiState.update { it.copy(hayDesafiosNuevos = false) }

    private fun mostrarError(e: Throwable) =
        _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) }

    companion object {
        private const val CANTIDAD_ULTIMAS = 5L

        val Factory = fabricaDe {
            InicioViewModel(
                it.perfilRepository, it.resumenRepository, it.registroRepository, it.categoriaRepository,
                it.desafioRepository, it.renovadorDesafios, it.metaRepository, it.ultimoNivelVisto,
            )
        }
    }
}
