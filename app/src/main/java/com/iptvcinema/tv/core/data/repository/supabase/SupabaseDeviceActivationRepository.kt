package com.iptvcinema.tv.core.data.repository.supabase

import com.iptvcinema.tv.BuildConfig
import com.iptvcinema.tv.core.data.repository.ActivationStatusSnapshot
import com.iptvcinema.tv.core.data.repository.AuthRepository
import com.iptvcinema.tv.core.data.repository.DeviceActivationRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.model.DeviceActivationSession
import com.iptvcinema.tv.core.supabase.dto.ActivationStatusDto
import com.iptvcinema.tv.core.supabase.dto.DeviceActivationSessionDto
import com.iptvcinema.tv.core.supabase.dto.SessionExchangeRequest
import com.iptvcinema.tv.core.supabase.dto.SessionExchangeResponse
import com.iptvcinema.tv.core.supabase.mapper.toDomain
import com.iptvcinema.tv.core.supabase.mapper.toSnapshot
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Thrown when the exchange endpoint rejects the request; [httpStatus] drives the UI message. */
class ActivationExchangeException(val httpStatus: Int) : Exception("Activation exchange failed ($httpStatus)")

@Singleton
class SupabaseDeviceActivationRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
    private val appSessionRepository: AppSessionRepository,
    private val httpClient: HttpClient,
    private val json: Json,
) : DeviceActivationRepository {
    private val random = SecureRandom()

    override suspend fun createSession(deviceName: String): DeviceActivationSession {
        repeat(MAX_CODE_ATTEMPTS - 1) {
            try {
                return createSessionOnce(deviceName)
            } catch (error: PostgrestRestException) {
                if (error.code != UNIQUE_VIOLATION) throw error
            }
        }
        return createSessionOnce(deviceName)
    }

    // RPC returns a single composite row (JSON object), not a list: use decodeAs, not decodeSingle.
    private suspend fun createSessionOnce(deviceName: String): DeviceActivationSession =
        supabaseClient.postgrest.rpc(
            function = "create_device_activation_session",
            parameters = buildJsonObject {
                put("activation_code", generateActivationCode())
                put("activation_qr_token", generateQrToken())
                put("activation_device_name", deviceName)
            },
        )
            .decodeAs<DeviceActivationSessionDto>()
            .toDomain()

    override suspend fun checkStatus(session: DeviceActivationSession): ActivationStatusSnapshot =
        supabaseClient.postgrest.rpc(
            function = "get_device_activation_session",
            parameters = buildJsonObject {
                put("session_id", session.id)
                put("session_qr_token", session.qrToken)
            },
        )
            .decodeAs<ActivationStatusDto>()
            .toSnapshot()

    override suspend fun exchangeForAuthSession(session: DeviceActivationSession): Result<Unit> = runCatching {
        val httpResponse = httpClient.post("${BuildConfig.SUPABASE_URL}/functions/v1/exchange-activation-session") {
            contentType(ContentType.Application.Json)
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            header("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            setBody(SessionExchangeRequest(sessionId = session.id, qrToken = session.qrToken))
        }
        if (!httpResponse.status.isSuccess()) {
            throw ActivationExchangeException(httpResponse.status.value)
        }
        val response = json.decodeFromString<SessionExchangeResponse>(httpResponse.bodyAsText())

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
        val part1 = randomString(CODE_CHARS, 4)
        val part2 = randomString(CODE_CHARS, 2)
        return "$part1-$part2"
    }

    private fun generateQrToken(): String = randomString(TOKEN_CHARS, 32)

    private fun randomString(alphabet: String, length: Int): String =
        buildString(length) { repeat(length) { append(alphabet[random.nextInt(alphabet.length)]) } }

    private companion object {
        const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        const val TOKEN_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789"
        const val MAX_CODE_ATTEMPTS = 3
        const val UNIQUE_VIOLATION = "23505"
    }
}
