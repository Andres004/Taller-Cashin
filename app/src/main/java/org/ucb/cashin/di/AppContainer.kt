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

/**
 * Inyección de dependencias manual (sin Hilt). Un solo cliente de Supabase para toda la app
 * y un repositorio por módulo. Para agregar un repositorio nuevo:
 *
 *     val registroRepository by lazy { RegistroRepository(supabase) }
 *
 * y en el ViewModel: `(this[APPLICATION_KEY] as CashinApp).container.registroRepository`.
 */
class AppContainer {

    val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY,
    ) {
        defaultSerializer = KotlinXSerializer(
            Json {
                ignoreUnknownKeys = true   // columnas nuevas en la BD no rompen la app
                encodeDefaults = true      // los Nuevo* envían todos sus campos
            },
        )
        install(Auth)
        install(Postgrest)
        install(Functions)
        // Sprint 2 (HU-02): install(ComposeAuth) { googleNativeLogin(serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID) }
    }

    val authRepository: AuthRepository by lazy { AuthRepository(supabase) }

    companion object {
        /** false si falta local.properties: la app muestra instrucciones en vez de fallar. */
        val estaConfigurado: Boolean
            get() = BuildConfig.SUPABASE_URL.startsWith("https://") && BuildConfig.SUPABASE_KEY.isNotBlank()
    }
}
