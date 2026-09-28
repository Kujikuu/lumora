package com.iptvcinema.tv.core.data.repository

import com.iptvcinema.tv.core.model.ActivationSessionStatus
import com.iptvcinema.tv.core.model.DeviceActivationSession
import java.time.Instant

data class ActivationStatusSnapshot(
    val status: ActivationSessionStatus,
    val expiresAt: Instant,
)

interface DeviceActivationRepository {
    suspend fun createSession(deviceName: String): DeviceActivationSession

    /** Throws on network or server errors so callers can tell "offline" from "still pending". */
    suspend fun checkStatus(session: DeviceActivationSession): ActivationStatusSnapshot

    /** Exchanges an approved session for a signed-in Supabase session. */
    suspend fun exchangeForAuthSession(session: DeviceActivationSession): Result<Unit>

    fun buildActivationUrl(code: String): String
}
