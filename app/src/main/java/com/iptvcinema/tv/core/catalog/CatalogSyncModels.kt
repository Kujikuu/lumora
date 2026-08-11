package com.iptvcinema.tv.core.catalog

import androidx.annotation.StringRes
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.model.SourceType

enum class CatalogSyncTrigger(
    val force: Boolean,
    val awaitEpgCompletion: Boolean,
) {
    INITIAL_CONNECTION(force = true, awaitEpgCompletion = false),
    STARTUP(force = false, awaitEpgCompletion = true),
    PERIODIC(force = false, awaitEpgCompletion = true),
    MANUAL(force = true, awaitEpgCompletion = false),
}

data class CatalogChangeSummary(
    val added: Int = 0,
    val updated: Int = 0,
    val removed: Int = 0,
) {
    val totalChanges: Int get() = added + updated + removed
    val hasChanges: Boolean get() = totalChanges > 0

    operator fun plus(other: CatalogChangeSummary): CatalogChangeSummary = CatalogChangeSummary(
        added = added + other.added,
        updated = updated + other.updated,
        removed = removed + other.removed,
    )
}

enum class CatalogSyncFailureReason {
    NO_SOURCE,
    DEMO_UNAVAILABLE,
    CREDENTIALS_MISSING,
    AUTHENTICATION,
    NETWORK,
    PROVIDER,
    UNKNOWN,
}

@StringRes
internal fun failureMessageResource(reason: CatalogSyncFailureReason): Int = when (reason) {
    CatalogSyncFailureReason.NO_SOURCE -> R.string.refresh_source_missing
    CatalogSyncFailureReason.DEMO_UNAVAILABLE -> R.string.refresh_demo_unavailable
    CatalogSyncFailureReason.CREDENTIALS_MISSING -> R.string.refresh_credentials_missing
    CatalogSyncFailureReason.AUTHENTICATION -> R.string.refresh_authentication_failed
    CatalogSyncFailureReason.NETWORK -> R.string.refresh_network_failed
    CatalogSyncFailureReason.PROVIDER -> R.string.refresh_provider_failed
    CatalogSyncFailureReason.UNKNOWN -> R.string.refresh_failed
}

sealed interface CatalogSyncCoordinatorResult {
    data class Success(
        val sourceType: SourceType,
        val liveChannelCount: Int,
        val movieCount: Int,
        val seriesCount: Int,
        val changes: CatalogChangeSummary,
        val checkedProvider: Boolean,
        val epgAvailable: Boolean = false,
    ) : CatalogSyncCoordinatorResult

    data class Failed(
        val message: String,
        val retryable: Boolean,
        val reason: CatalogSyncFailureReason = CatalogSyncFailureReason.UNKNOWN,
    ) : CatalogSyncCoordinatorResult
}

internal fun requiresForcedFollowUp(
    requestedTrigger: CatalogSyncTrigger,
    activeTrigger: CatalogSyncTrigger,
    result: CatalogSyncCoordinatorResult,
): Boolean = result is CatalogSyncCoordinatorResult.Success && (
    (
        requestedTrigger.force &&
            !activeTrigger.force &&
            (!result.checkedProvider || !result.changes.hasChanges)
        ) ||
        (requestedTrigger == CatalogSyncTrigger.INITIAL_CONNECTION && activeTrigger != requestedTrigger)
    )

internal fun requiresEpgJoin(
    requestedTrigger: CatalogSyncTrigger,
    activeTrigger: CatalogSyncTrigger,
    result: CatalogSyncCoordinatorResult,
): Boolean = result is CatalogSyncCoordinatorResult.Success &&
    requestedTrigger.awaitEpgCompletion &&
    !activeTrigger.awaitEpgCompletion

object CatalogSyncPolicy {
    const val STARTUP_FRESHNESS_MS = 3L * 60 * 60 * 1000
    const val PERIODIC_INTERVAL_HOURS = 12L
    const val DATABASE_BATCH_SIZE = 500

    fun isStale(lastSyncedAtEpochMs: Long?, nowEpochMs: Long): Boolean =
        lastSyncedAtEpochMs == null || nowEpochMs - lastSyncedAtEpochMs >= STARTUP_FRESHNESS_MS

    fun <T> databaseBatches(items: Collection<T>): List<List<T>> =
        items.toList().chunked(DATABASE_BATCH_SIZE)
}

object CatalogSyncResource {
    const val XTREAM_LIVE_CATEGORIES = "xtream_live_categories"
    const val XTREAM_LIVE_STREAMS = "xtream_live_streams"
    const val XTREAM_VOD_CATEGORIES = "xtream_vod_categories"
    const val XTREAM_VOD_STREAMS = "xtream_vod_streams"
    const val XTREAM_SERIES_CATEGORIES = "xtream_series_categories"
    const val XTREAM_SERIES = "xtream_series"
    const val M3U_PLAYLIST = "m3u_playlist"
}
