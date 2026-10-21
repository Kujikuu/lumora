package com.iptvcinema.tv.core.supabase.di

import android.content.Context
import com.iptvcinema.tv.BuildConfig
import com.iptvcinema.tv.core.supabase.security.EncryptedSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    /**
     * One OkHttp-backed engine for every Ktor client. Realtime needs WebSockets; the Android
     * (HttpURLConnection) engine has none, so live updates failed and retried every few seconds.
     * Built from a fresh OkHttp client: the playlist client's User-Agent and long timeouts are
     * not meant for Supabase.
     */
    @Provides
    @Singleton
    fun provideHttpClientEngine(): HttpClientEngine = OkHttp.create {
        preconfigured = OkHttpClient.Builder()
            .pingInterval(WEBSOCKET_PING_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideHttpClient(engine: HttpClientEngine, json: Json): HttpClient = HttpClient(engine) {
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    @Provides
    @Singleton
    fun provideSupabaseClient(
        engine: HttpClientEngine,
        json: Json,
        @ApplicationContext context: Context,
    ): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        httpEngine = engine
        defaultSerializer = KotlinXSerializer(json)
        install(Auth) {
            sessionManager = EncryptedSessionManager(
                context = context,
                legacy = EncryptedSessionManager.legacyFor(BuildConfig.SUPABASE_URL),
            )
        }
        install(Postgrest)
        install(Realtime)
    }

    private const val CONNECT_TIMEOUT_MS = 10_000L
    private const val REQUEST_TIMEOUT_MS = 20_000L
    private const val WEBSOCKET_PING_SECONDS = 20L
}
