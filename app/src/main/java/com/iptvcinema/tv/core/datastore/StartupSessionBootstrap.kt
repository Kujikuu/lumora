package com.iptvcinema.tv.core.datastore

import com.iptvcinema.tv.core.datastore.AppSessionState
import com.iptvcinema.tv.core.datastore.SessionRequirement
import com.iptvcinema.tv.core.player.WatchedSeriesEpisodePrefetcher
import com.iptvcinema.tv.core.data.local.LocalCredentialsStore
import com.iptvcinema.tv.core.data.repository.AuthRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabasePlaylistSourcesRepository
import com.iptvcinema.tv.core.di.ApplicationScope
import com.iptvcinema.tv.core.model.PlaylistSourceRecord
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.sync.CatalogSyncScheduler
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun shouldScheduleStartupCatalogSync(state: AppSessionState): Boolean =
    state.meetsRequirement(SessionRequirement.HasSource) &&
        state.currentSourceId != null &&
        state.sourceType != null &&
        state.sourceType != SourceType.DEMO &&
        !state.isDemoMode

@Singleton
class StartupSessionBootstrap @Inject constructor(
    private val appSessionRepository: AppSessionRepository,
    private val authRepository: AuthRepository,
    private val playlistSourcesRepository: SupabasePlaylistSourcesRepository,
    private val localCredentialsStore: LocalCredentialsStore,
    private val watchedSeriesEpisodePrefetcher: WatchedSeriesEpisodePrefetcher,
    private val catalogSyncScheduler: CatalogSyncScheduler,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    suspend fun prepareSessionState(): AppSessionState {
        if (authRepository.isConfigured()) {
            authRepository.awaitAuthInitialization()
            authRepository.syncSessionToLocal()
        }
        restoreActiveSourceIfNeeded()
        val state = appSessionRepository.sessionState.first()
        prefetchWatchedSeriesEpisodesIfReady(state)
        if (shouldScheduleStartupCatalogSync(state)) {
            catalogSyncScheduler.enqueueStartupCheck()
        }
        return state
    }

    suspend fun authenticateLocalDev(): AppSessionState {
        appSessionRepository.setAuthenticated(authenticated = true, userId = "local-dev-user")
        restoreActiveSourceIfNeeded()
        return appSessionRepository.sessionState.first()
    }

    private suspend fun restoreActiveSourceIfNeeded() {
        val state = appSessionRepository.sessionState.first()
        if (!state.isAuthenticated) return

        if (state.hasSource && state.currentSourceId != null) {
            validateExistingSource(state.currentSourceId, state.sourceType)
            return
        }

        if (!authRepository.isConfigured()) return

        runCatching {
            val userId = state.userId ?: return@runCatching
            val sources = playlistSourcesRepository.getSourcesCached(userId)
            val activeSource = sources.firstOrNull { it.isActive } ?: sources.firstOrNull() ?: return@runCatching
            playlistSourcesRepository.ensureLocalCredentials(activeSource)
            if (canRestoreSource(activeSource)) {
                appSessionRepository.setSource(
                    sourceId = activeSource.id,
                    sourceType = activeSource.type,
                    isDemoMode = activeSource.type == SourceType.DEMO,
                )
            }
        }
    }

    private suspend fun validateExistingSource(sourceId: String, sourceType: SourceType?) {
        if (!authRepository.isConfigured()) return

        runCatching {
            val userId = appSessionRepository.sessionState.first().userId ?: return@runCatching
            val sources = playlistSourcesRepository.getSourcesCached(userId)
            val source = sources.firstOrNull { it.id == sourceId }
            if (source == null) {
                appSessionRepository.clearSource()
            } else {
                playlistSourcesRepository.ensureLocalCredentials(source)
                if (!canRestoreSource(source)) {
                    appSessionRepository.clearSource()
                } else if (sourceType != source.type) {
                    appSessionRepository.setSource(
                        sourceId = source.id,
                        sourceType = source.type,
                        isDemoMode = source.type == SourceType.DEMO,
                    )
                }
            }
        }.onFailure {
            // Keep local flags when offline; browsing still uses Room cache.
        }
    }

    private fun canRestoreSource(source: PlaylistSourceRecord): Boolean = when (source.type) {
        SourceType.XTREAM_CODES -> localCredentialsStore.getXtreamCredentials(source.id) != null
        SourceType.M3U -> localCredentialsStore.getM3uCredentials(source.id) != null
        SourceType.DEMO -> true
    }

    private fun prefetchWatchedSeriesEpisodesIfReady(state: AppSessionState) {
        if (!state.meetsRequirement(SessionRequirement.Ready)) return
        if (state.isDemoMode) return
        applicationScope.launch {
            runCatching {
                watchedSeriesEpisodePrefetcher.prefetchForCurrentSession()
            }
        }
    }
}
