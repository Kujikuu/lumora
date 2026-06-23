package com.iptvcinema.tv.core.data.repository

import com.iptvcinema.tv.core.model.DeviceActivationSession

interface DeviceActivationRepository {
    suspend fun createSession(deviceName: String): DeviceActivationSession
    suspend fun getSession(sessionId: String): DeviceActivationSession?
    suspend fun exchangeForAuthSession(code: String): Result<Unit>
    fun buildActivationUrl(code: String): String
}
