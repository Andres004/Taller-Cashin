package org.ucb.cashin.di

import android.content.res.Resources
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import org.ucb.cashin.BuildConfig
import org.ucb.cashin.data.AuthRepository
import org.ucb.cashin.data.CategoriaRepository
import org.ucb.cashin.data.DesafioRepository
import org.ucb.cashin.data.MetaRepository
import org.ucb.cashin.data.PerfilRepository
import org.ucb.cashin.data.PlanificadoRepository
import org.ucb.cashin.data.RegistroRepository
import org.ucb.cashin.data.RenovadorDesafios
import org.ucb.cashin.data.ReporteRepository
import org.ucb.cashin.data.ResumenRepository

/**
 * Crea una sola vez la conexión con Supabase y los repositorios que usa toda la app.
 * Cada repositorio se crea recién cuando alguien lo necesita por primera vez.
 * [recursos] (de la Application) solo lo usa el renovador de desafíos para escribir sus títulos.
 */
class AppContainer(private val recursos: Resources) {

    val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY,
    ) {
        defaultSerializer = KotlinXSerializer(
            Json {
                ignoreUnknownKeys = true   // si la BD agrega columnas, la app no se rompe
                encodeDefaults = true      // al guardar, se envían todos los campos
            },
        )
        install(Auth)
        install(Postgrest)
        install(Functions)
        // Sprint 3 (HU-02): install(ComposeAuth) { googleNativeLogin(serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID) }
    }

    val authRepository: AuthRepository by lazy { AuthRepository(supabase) }

    // Sprint 2
    val categoriaRepository: CategoriaRepository by lazy { CategoriaRepository(supabase) }
    val registroRepository: RegistroRepository by lazy { RegistroRepository(supabase) }
    val perfilRepository: PerfilRepository by lazy { PerfilRepository(supabase) }
    val resumenRepository: ResumenRepository by lazy { ResumenRepository(supabase) }

    // Sprint 3
    val planificadoRepository: PlanificadoRepository by lazy { PlanificadoRepository(supabase) }
    val metaRepository: MetaRepository by lazy { MetaRepository(supabase) }

    // Sprint 4
    val desafioRepository: DesafioRepository by lazy { DesafioRepository(supabase) }

    // Sprint 5
    val reporteRepository: ReporteRepository by lazy { ReporteRepository(supabase) }

    /** HU-22: asigna solos los desafíos de la semana y del mes (una vez por periodo). */
    val renovadorDesafios: RenovadorDesafios by lazy {
        RenovadorDesafios(
            authRepository, desafioRepository, resumenRepository, registroRepository, metaRepository,
            planificadoRepository, categoriaRepository, recursos,
        )
    }

    /**
     * HU-24: último nivel que Inicio mostró en esta sesión de la app (solo en memoria), por usuario
     * para que cerrar sesión y entrar con otra cuenta no celebre de más.
     * Si al volver a Inicio el nivel es mayor, se celebra "¡Subiste al nivel N!".
     */
    val ultimoNivelVisto: MutableMap<String, Int> = mutableMapOf()
    val ultimoXpVisto: MutableMap<String, Int> = mutableMapOf()

    companion object {
        /** Indica si las claves de Supabase están configuradas en local.properties. */
        val estaConfigurado: Boolean
            get() = BuildConfig.SUPABASE_URL.startsWith("https://") && BuildConfig.SUPABASE_KEY.isNotBlank()
    }
}