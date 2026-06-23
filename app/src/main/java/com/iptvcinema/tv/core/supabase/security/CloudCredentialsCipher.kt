package com.iptvcinema.tv.core.supabase.security

import com.iptvcinema.tv.core.model.M3uCredentials
import com.iptvcinema.tv.core.model.XtreamCredentials
import com.iptvcinema.tv.core.supabase.dto.EncryptedCredentialsDto
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class CloudCredentialsCipher @Inject constructor(
    private val json: Json,
) {
    fun encryptXtream(userId: String, credentials: XtreamCredentials): EncryptedCredentialsDto {
        val payload = XtreamPayload(
            serverUrl = credentials.serverUrl,
            username = credentials.username,
            password = credentials.password,
            accountName = credentials.accountName,
        )
        return encrypt(userId, TYPE_XTREAM, json.encodeToString(payload))
    }

    fun decryptXtream(userId: String, encrypted: EncryptedCredentialsDto): XtreamCredentials? {
        if (encrypted.type != TYPE_XTREAM) return null
        return runCatching {
            json.decodeFromString<XtreamPayload>(decrypt(userId, encrypted)).toDomain()
        }.getOrNull()
    }

    fun encryptM3u(userId: String, credentials: M3uCredentials): EncryptedCredentialsDto {
        val payload = M3uPayload(
            playlistUrl = credentials.playlistUrl,
            epgUrl = credentials.epgUrl,
            playlistName = credentials.playlistName,
            userAgent = credentials.userAgent,
            referer = credentials.referer,
            customHeaders = credentials.customHeaders,
        )
        return encrypt(userId, TYPE_M3U, json.encodeToString(payload))
    }

    fun decryptM3u(userId: String, encrypted: EncryptedCredentialsDto): M3uCredentials? {
        if (encrypted.type != TYPE_M3U) return null
        return runCatching {
            json.decodeFromString<M3uPayload>(decrypt(userId, encrypted)).toDomain()
        }.getOrNull()
    }

    private fun encrypt(userId: String, type: String, plaintext: String): EncryptedCredentialsDto {
        val iv = ByteArray(GCM_IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(userId), GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return EncryptedCredentialsDto(
            type = type,
            iv = Base64.getEncoder().encodeToString(iv),
            data = Base64.getEncoder().encodeToString(ciphertext),
        )
    }

    private fun decrypt(userId: String, encrypted: EncryptedCredentialsDto): String {
        val iv = Base64.getDecoder().decode(encrypted.iv)
        val ciphertext = Base64.getDecoder().decode(encrypted.data)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(userId), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun deriveKey(userId: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyMaterial = digest.digest("$userId:$KEY_SALT".toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyMaterial, "AES")
    }

    @Serializable
    private data class XtreamPayload(
        @SerialName("server_url") val serverUrl: String,
        val username: String,
        val password: String,
        @SerialName("account_name") val accountName: String,
    ) {
        fun toDomain() = XtreamCredentials(
            serverUrl = serverUrl,
            username = username,
            password = password,
            accountName = accountName,
        )
    }

    @Serializable
    private data class M3uPayload(
        @SerialName("playlist_url") val playlistUrl: String,
        @SerialName("epg_url") val epgUrl: String?,
        @SerialName("playlist_name") val playlistName: String,
        @SerialName("user_agent") val userAgent: String?,
        val referer: String? = null,
        @SerialName("custom_headers") val customHeaders: Map<String, String> = emptyMap(),
    ) {
        fun toDomain() = M3uCredentials(
            playlistUrl = playlistUrl,
            epgUrl = epgUrl,
            playlistName = playlistName,
            userAgent = userAgent,
            referer = referer,
            customHeaders = customHeaders,
        )
    }

    companion object {
        private const val TYPE_XTREAM = "XTREAM_CODES"
        private const val TYPE_M3U = "M3U"
        private const val KEY_SALT = "iptv-cinema-cloud-credentials-v1"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
    }
}
