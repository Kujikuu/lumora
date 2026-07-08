package com.iptvcinema.tv.core.data.repository.supabase

import com.iptvcinema.tv.core.data.local.CloudUserDataCache
import com.iptvcinema.tv.core.data.local.LocalCredentialsStore
import com.iptvcinema.tv.core.data.repository.PlaylistSourcesRepository
import com.iptvcinema.tv.core.model.M3uCredentials
import com.iptvcinema.tv.core.model.PlaylistSourceRecord
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.model.XtreamCredentials
import com.iptvcinema.tv.core.xtream.XtreamUrlNormalizer
import com.iptvcinema.tv.core.supabase.dto.EncryptedCredentialsDto
import com.iptvcinema.tv.core.supabase.dto.PlaylistSourceDto
import com.iptvcinema.tv.core.supabase.mapper.toDomain
import com.iptvcinema.tv.core.supabase.security.CloudCredentialsCipher
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Singleton
class SupabasePlaylistSourcesRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val localCredentialsStore: LocalCredentialsStore,
    private val cloudCredentialsCipher: CloudCredentialsCipher,
    private val cloudUserDataCache: CloudUserDataCache,
) : PlaylistSourcesRepository {
    private var cachedSources: List<PlaylistSourceRecord>? = null
    private var cachedSourcesAtMs: Long = 0L

    override suspend fun getSources(): List<PlaylistSourceRecord> {
        val userId = requireUserId()
        val now = System.currentTimeMillis()
        cachedSources?.takeIf { now - cachedSourcesAtMs <= SOURCE_CACHE_TTL_MS }?.let { return it }

        val remote = supabaseClient.from(TABLE)
            .select(
                Columns.list(
                    COLUMN_ID,
                    COLUMN_USER_ID,
                    "name",
                    "type",
                    "server_url",
                    "playlist_url",
                    "epg_url",
                    "is_active",
                    "status",
                    "last_synced_at",
                ),
            ) {
                filter {
                    eq(COLUMN_USER_ID, userId)
                }
            }
            .decodeList<PlaylistSourceDto>()
            .map { it.toDomain() }

        cachedSources = remote
        cachedSourcesAtMs = now
        cloudUserDataCache.savePlaylistSources(userId, remote)
        return remote
    }

    suspend fun getSourcesCached(userId: String): List<PlaylistSourceRecord> =
        runCatching { getSources() }.getOrElse {
            cloudUserDataCache.getPlaylistSources(userId).orEmpty()
        }

    override suspend fun saveXtreamSource(credentials: XtreamCredentials): PlaylistSourceRecord {
        val userId = requireUserId()
        deactivateAllSources(userId)
        val insert = PlaylistSourceInsertDto(
            userId = userId,
            name = credentials.accountName.ifBlank { "Xtream Codes" },
            type = SourceType.XTREAM_CODES.name,
            serverUrl = credentials.serverUrl,
            isActive = true,
            status = SourceStatus.ACTIVE.name,
        )
        val saved = supabaseClient.from(TABLE)
            .insert(insert) {
                select(Columns.ALL)
            }
            .decodeSingle<PlaylistSourceDto>()
        localCredentialsStore.saveXtreamCredentials(saved.id, credentials)
        uploadEncryptedCredentials(saved.id, userId, credentials)
        invalidateSourceCache()
        return saved.toDomain()
    }

    override suspend fun findMatchingXtreamSource(credentials: XtreamCredentials): PlaylistSourceRecord? {
        val normalizedInput = XtreamUrlNormalizer.normalize(credentials.serverUrl).getOrNull() ?: return null
        return getSources()
            .asSequence()
            .filter { it.type == SourceType.XTREAM_CODES }
            .firstOrNull { source ->
                matchesXtreamSource(
                    serverUrl = source.serverUrl,
                    storedCredentials = localCredentialsStore.getXtreamCredentials(source.id),
                    credentials = credentials,
                    normalizedInput = normalizedInput,
                )
            }
    }

    override suspend fun activateXtreamSource(
        sourceId: String,
        credentials: XtreamCredentials,
    ): PlaylistSourceRecord {
        val userId = requireUserId()
        deactivateAllSources(userId)
        supabaseClient.from(TABLE)
            .update(
                PlaylistSourceActivationUpdateDto(
                    isActive = true,
                    status = SourceStatus.ACTIVE.name,
                    encryptedCredentials = cloudCredentialsCipher.encryptXtream(userId, credentials),
                ),
            ) {
                filter {
                    eq(COLUMN_ID, sourceId)
                    eq(COLUMN_USER_ID, userId)
                }
            }
        localCredentialsStore.saveXtreamCredentials(sourceId, credentials)
        invalidateSourceCache()
        return getSources().first { it.id == sourceId }
    }

    override suspend fun saveM3uSource(credentials: M3uCredentials): PlaylistSourceRecord {
        val userId = requireUserId()
        deactivateAllSources(userId)
        val insert = PlaylistSourceInsertDto(
            userId = userId,
            name = credentials.playlistName.ifBlank { "M3U Playlist" },
            type = SourceType.M3U.name,
            playlistUrl = credentials.playlistUrl,
            epgUrl = credentials.epgUrl,
            isActive = true,
            status = SourceStatus.ACTIVE.name,
        )
        val saved = supabaseClient.from(TABLE)
            .insert(insert) {
                select(Columns.ALL)
            }
            .decodeSingle<PlaylistSourceDto>()
        localCredentialsStore.saveM3uCredentials(saved.id, credentials)
        uploadEncryptedM3uCredentials(saved.id, userId, credentials)
        invalidateSourceCache()
        return saved.toDomain()
    }

    override suspend fun saveDemoSource(): PlaylistSourceRecord {
        val userId = requireUserId()
        deactivateAllSources(userId)
        val insert = PlaylistSourceInsertDto(
            userId = userId,
            name = "Demo Mode",
            type = SourceType.DEMO.name,
            isActive = true,
            status = SourceStatus.ACTIVE.name,
        )
        return supabaseClient.from(TABLE)
            .insert(insert) {
                select(Columns.ALL)
            }
            .decodeSingle<PlaylistSourceDto>()
            .toDomain()
            .also { invalidateSourceCache() }
    }

    override suspend fun setActiveSource(sourceId: String) {
        val userId = requireUserId()
        deactivateAllSources(userId)
        supabaseClient.from(TABLE)
            .update(PlaylistSourceActiveUpdateDto(isActive = true)) {
                filter {
                    eq(COLUMN_ID, sourceId)
                    eq(COLUMN_USER_ID, userId)
                }
            }
        invalidateSourceCache()
    }

    override suspend fun deleteSource(sourceId: String) {
        val userId = requireUserId()
        supabaseClient.from(TABLE)
            .delete {
                filter {
                    eq(COLUMN_ID, sourceId)
                    eq(COLUMN_USER_ID, userId)
                }
            }
        localCredentialsStore.removeCredentials(sourceId)
        invalidateSourceCache()
    }

    override suspend fun updateSyncStatus(
        sourceId: String,
        status: SourceStatus,
        lastSyncedAt: Instant?,
    ) {
        val userId = requireUserId()
        supabaseClient.from(TABLE)
            .update(
                PlaylistSourceSyncStatusUpdateDto(
                    status = status.name,
                    lastSyncedAt = lastSyncedAt?.toString(),
                ),
            ) {
                filter {
                    eq(COLUMN_ID, sourceId)
                    eq(COLUMN_USER_ID, userId)
                }
            }
        invalidateSourceCache()
    }

    override suspend fun ensureLocalCredentials(source: PlaylistSourceRecord) {
        when (source.type) {
            SourceType.DEMO -> Unit
            SourceType.XTREAM_CODES -> {
                if (localCredentialsStore.getXtreamCredentials(source.id) != null) return
                val dto = fetchSourceDto(source.id) ?: return
                dto.encryptedCredentials
                    ?.let { cloudCredentialsCipher.decryptXtream(source.userId, it) }
                    ?.let { localCredentialsStore.saveXtreamCredentials(source.id, it) }
            }
            SourceType.M3U -> {
                if (localCredentialsStore.getM3uCredentials(source.id) != null) return
                val dto = fetchSourceDto(source.id) ?: return
                val restored = dto.encryptedCredentials
                    ?.let { cloudCredentialsCipher.decryptM3u(source.userId, it) }
                if (restored != null) {
                    localCredentialsStore.saveM3uCredentials(source.id, restored)
                    return
                }
                hydrateM3uFromMetadata(source)
            }
        }
    }

    private suspend fun fetchSourceDto(sourceId: String): PlaylistSourceDto? {
        val userId = requireUserId()
        return runCatching {
            supabaseClient.from(TABLE)
                .select(Columns.ALL) {
                    filter {
                        eq(COLUMN_ID, sourceId)
                        eq(COLUMN_USER_ID, userId)
                    }
                }
                .decodeSingleOrNull<PlaylistSourceDto>()
        }.getOrNull()
    }

    private suspend fun uploadEncryptedCredentials(
        sourceId: String,
        userId: String,
        credentials: XtreamCredentials,
    ) {
        runCatching {
            supabaseClient.from(TABLE)
                .update(
                    PlaylistSourceCredentialsUpdateDto(
                        encryptedCredentials = cloudCredentialsCipher.encryptXtream(userId, credentials),
                    ),
                ) {
                    filter {
                        eq(COLUMN_ID, sourceId)
                        eq(COLUMN_USER_ID, userId)
                    }
                }
        }
    }

    private suspend fun uploadEncryptedM3uCredentials(
        sourceId: String,
        userId: String,
        credentials: M3uCredentials,
    ) {
        runCatching {
            supabaseClient.from(TABLE)
                .update(
                    PlaylistSourceCredentialsUpdateDto(
                        encryptedCredentials = cloudCredentialsCipher.encryptM3u(userId, credentials),
                    ),
                ) {
                    filter {
                        eq(COLUMN_ID, sourceId)
                        eq(COLUMN_USER_ID, userId)
                    }
                }
        }
    }

    private fun hydrateM3uFromMetadata(source: PlaylistSourceRecord) {
        val playlistUrl = source.playlistUrl?.trim().orEmpty()
        if (playlistUrl.isBlank()) return
        localCredentialsStore.saveM3uCredentials(
            source.id,
            M3uCredentials(
                playlistUrl = playlistUrl,
                epgUrl = source.epgUrl,
                playlistName = source.name,
                userAgent = null,
            ),
        )
    }

    private fun invalidateSourceCache() {
        cachedSources = null
        cachedSourcesAtMs = 0L
    }

    private suspend fun deactivateAllSources(userId: String) {
        supabaseClient.from(TABLE)
            .update(PlaylistSourceActiveUpdateDto(isActive = false)) {
                filter {
                    eq(COLUMN_USER_ID, userId)
                }
            }
    }

    private fun requireUserId(): String =
        supabaseClient.auth.currentUserOrNull()?.id
            ?: error("User must be authenticated")

    @Serializable
    private data class PlaylistSourceInsertDto(
        @SerialName("user_id") val userId: String,
        val name: String,
        val type: String,
        @SerialName("server_url") val serverUrl: String? = null,
        @SerialName("playlist_url") val playlistUrl: String? = null,
        @SerialName("epg_url") val epgUrl: String? = null,
        @SerialName("is_active") val isActive: Boolean = true,
        val status: String = SourceStatus.ACTIVE.name,
    )

    @Serializable
    private data class PlaylistSourceActiveUpdateDto(
        @SerialName("is_active") val isActive: Boolean,
    )

    @Serializable
    private data class PlaylistSourceActivationUpdateDto(
        @SerialName("is_active") val isActive: Boolean,
        val status: String,
        @SerialName("encrypted_credentials") val encryptedCredentials: EncryptedCredentialsDto? = null,
    )

    @Serializable
    private data class PlaylistSourceCredentialsUpdateDto(
        @SerialName("encrypted_credentials") val encryptedCredentials: EncryptedCredentialsDto,
    )

    @Serializable
    private data class PlaylistSourceSyncStatusUpdateDto(
        val status: String,
        @SerialName("last_synced_at") val lastSyncedAt: String? = null,
    )

    companion object {
        private const val TABLE = "playlist_sources"
        private const val COLUMN_ID = "id"
        private const val COLUMN_USER_ID = "user_id"
        private const val SOURCE_CACHE_TTL_MS = 30_000L

        internal fun matchesXtreamSource(
            serverUrl: String?,
            storedCredentials: XtreamCredentials?,
            credentials: XtreamCredentials,
            normalizedInput: String = XtreamUrlNormalizer.normalize(credentials.serverUrl).getOrNull().orEmpty(),
        ): Boolean {
            if (storedCredentials == null || normalizedInput.isBlank()) return false
            val normalizedStored = serverUrl?.let { XtreamUrlNormalizer.normalize(it).getOrNull() } ?: return false
            return normalizedStored == normalizedInput && storedCredentials.username == credentials.username
        }
    }
}
