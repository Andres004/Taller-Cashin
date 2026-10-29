package org.ucb.cashin.di

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
import org.ucb.cashin.data.MetaRepository
import org.ucb.cashin.data.PerfilRepository
import org.ucb.cashin.data.PlanificadoRepository
import org.ucb.cashin.data.RegistroRepository
import org.ucb.cashin.data.ResumenRepository

/**
 * Crea una sola vez la conexión con Supabase y los repositorios que usa toda la app.
 * Cada repositorio se crea recién cuando alguien lo necesita por primera vez.
 */
class AppContainer {

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

    companion object {
        /** Indica si las claves de Supabase están configuradas en local.properties. */
        val estaConfigurado: Boolean
            get() = BuildConfig.SUPABASE_URL.startsWith("https://") && BuildConfig.SUPABASE_KEY.isNotBlank()
    }
}