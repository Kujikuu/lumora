package com.iptvcinema.tv.core.data.repository.supabase

import com.iptvcinema.tv.BuildConfig
import com.iptvcinema.tv.core.data.repository.AuthRepository
import com.iptvcinema.tv.core.data.repository.DeviceActivationRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.model.ActivationSessionStatus
import com.iptvcinema.tv.core.model.DeviceActivationSession
import com.iptvcinema.tv.core.supabase.dto.ActivationSessionInsertDto
import com.iptvcinema.tv.core.supabase.dto.DeviceActivationSessionDto
import com.iptvcinema.tv.core.supabase.dto.SessionExchangeRequest
import com.iptvcinema.tv.core.supabase.dto.SessionExchangeResponse
import com.iptvcinema.tv.core.supabase.mapper.toDomain
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Singleton
class SupabaseDeviceActivationRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
    private val appSessionRepository: AppSessionRepository,
    private val httpClient: HttpClient,
    private val json: Json,
) : DeviceActivationRepository {
    override suspend fun createSession(deviceName: String): DeviceActivationSession {
        val code = generateActivationCode()
        val qrToken = generateQrToken()

        val dto = ActivationSessionInsertDto(
            code = code,
            qrToken = qrToken,
            status = ActivationSessionStatus.PENDING.name,
            deviceName = deviceName,
        )

        return supabaseClient.from(TABLE)
            .insert(dto) {
                select(Columns.ALL)
            }
            .decodeSingle<DeviceActivationSessionDto>()
            .toDomain()
    }

    override suspend fun getSession(sessionId: String): DeviceActivationSession? = runCatching {
        supabaseClient.from(TABLE)
            .select(Columns.ALL) {
                filter {
                    eq(COLUMN_ID, sessionId)
                }
            }
            .decodeSingleOrNull<DeviceActivationSessionDto>()
            ?.toDomain()
    }.getOrNull()

    override suspend fun exchangeForAuthSession(code: String): Result<Unit> = runCatching {
        val httpResponse = httpClient.post("${BuildConfig.SUPABASE_URL}/functions/v1/exchange-activation-session") {
            contentType(ContentType.Application.Json)
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            header("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            setBody(SessionExchangeRequest(code = code))
        }
        val bodyText = httpResponse.bodyAsText()
        if (!httpResponse.status.isSuccess()) {
            val message = runCatching {
                json.decodeFromString<SessionExchangeErrorResponse>(bodyText).error
            }.getOrDefault(bodyText.ifBlank { "Activation exchange failed (${httpResponse.status.value})" })
            error(message)
        }
        val response = json.decodeFromString<SessionExchangeResponse>(bodyText)

        authRepository.importSession(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
        )
        appSessionRepository.setAuthenticated(
            authenticated = true,
            userId = response.userId,
        )
    }

    override fun buildActivationUrl(code: String): String =
        "${BuildConfig.ACTIVATION_LINK_BASE}?activation=${code.uppercase()}"

    private fun generateActivationCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val part1 = (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        val part2 = (1..2).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "$part1-$part2"
    }

    private fun generateQrToken(): String =
        (1..32).map { TOKEN_CHARS[Random.nextInt(TOKEN_CHARS.length)] }.joinToString("")

    @Serializable
    private data class SessionExchangeErrorResponse(
        val error: String,
    )

    companion object {
        private const val TABLE = "device_activation_sessions"
        private const val COLUMN_ID = "id"
        private const val TOKEN_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789"
    }
}
