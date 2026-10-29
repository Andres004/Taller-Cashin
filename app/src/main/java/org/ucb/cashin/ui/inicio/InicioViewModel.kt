package org.ucb.cashin.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ucb.cashin.data.CategoriaRepository
import org.ucb.cashin.data.PerfilRepository
import org.ucb.cashin.data.RegistroRepository
import org.ucb.cashin.data.ResumenRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.prediccion.Confianza
import org.ucb.cashin.prediccion.MensajeAvatar
import org.ucb.cashin.prediccion.Pronostico
import org.ucb.cashin.prediccion.TipoMensaje
import org.ucb.cashin.prediccion.construirSerie
import org.ucb.cashin.prediccion.mensajeParaProyeccion
import org.ucb.cashin.prediccion.pronosticarProximoMes
import org.ucb.cashin.prediccion.pronosticarSiguienteMes
import org.ucb.cashin.prediccion.proyectarMesActual
import org.ucb.cashin.ui.registros.RegistroConCategoria
import org.ucb.cashin.ui.registros.unirConCategorias
import org.ucb.cashin.util.Formato
import java.math.BigDecimal

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
    /** Mensaje del motor para Cashi (gasto normal o más rápido de lo común). */
    val mensajeIa: MensajeAvatar? = null,
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
) {
    /** Ingresos menos gastos del mes. Es solo una diferencia de anotaciones, no un saldo. */
    val diferencia: BigDecimal get() = totalIngresos - totalGastos

    /** Hay al menos un mes completo de historia para pronosticar. */
    val hayPronostico: Boolean get() = pronosticoMes.confianza != Confianza.SIN_DATOS

    /** La proyección del mes supera el máximo pronosticado: se gasta más rápido que de costumbre. */
    val gastaMasRapido: Boolean get() = mensajeIa?.tipo == TipoMensaje.GASTO_MAS_RAPIDO
}

/**
 * HU-13: resumen del mes, racha, XP y últimas anotaciones.
 * IA predictiva: proyección del gasto de este mes y pronóstico del próximo (paquete prediccion/).
 */
class InicioViewModel(
    private val perfilRepository: PerfilRepository,
    private val resumenRepository: ResumenRepository,
    private val registroRepository: RegistroRepository,
    private val categoriaRepository: CategoriaRepository,
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

            // Si la historia no se puede leer, Inicio igual se muestra: el pronóstico queda "sin datos".
            val historial = resumenRepository.historialMensual().getOrDefault(emptyList())

            val hoy = Formato.hoy()
            val gastoDelMes = totales?.totalGastos ?: BigDecimal.ZERO
            val serieGastos = construirSerie(historial, hoy) { it.totalGastos }
            val pronosticoMes = pronosticarSiguienteMes(serieGastos)
            val proyeccion = proyectarMesActual(gastoDelMes, hoy, pronosticoMes)
            val pronosticoProximo = pronosticarProximoMes(serieGastos, proyeccion, hoy)

            yaSeMostro = true
            _uiState.update {
                it.copy(
                    nombre = perfil.nombre,
                    totalGastos = totales?.totalGastos ?: BigDecimal.ZERO,
                    totalIngresos = totales?.totalIngresos ?: BigDecimal.ZERO,
                    racha = perfil.rachaVigente(hoy),
                    anotoHoy = perfil.anotoHoy(hoy),
                    nivel = perfil.nivel,
                    xp = perfil.xp,
                    progresoNivel = perfil.progresoNivel,
                    ultimas = unirConCategorias(ultimas, categorias),
                    pronosticoMes = pronosticoMes,
                    proyeccionMes = proyeccion,
                    pronosticoProximo = pronosticoProximo,
                    mensajeIa = mensajeParaProyeccion(proyeccion, pronosticoMes),
                    cargando = false,
                    error = null,
                )
            }
        }
    }

    private fun mostrarError(e: Throwable) =
        _uiState.update { it.copy(cargando = false, error = e.comoErrorApp()) }

    companion object {
        private const val CANTIDAD_ULTIMAS = 5L

        val Factory = fabricaDe {
            InicioViewModel(it.perfilRepository, it.resumenRepository, it.registroRepository, it.categoriaRepository)
        }
    }
}