package com.iptvcinema.tv.core.tvhome

import com.iptvcinema.tv.core.data.repository.WatchHistoryCardDisplay
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem

enum class WatchNextKind {
    /** Stopped partway through. */
    Continue,

    /** The next episode after a finished one. */
    Next,
}

enum class WatchNextProgramKind { Movie, Episode }

/** One card in the TV home screen's Watch Next row, independent of the TV provider API. */
data class WatchNextEntry(
    /** Stable key, the same as the in-app Continue Watching key: "series:<id>" or "movie:<id>". */
    val key: String,
    val kind: WatchNextKind,
    val programKind: WatchNextProgramKind,
    val title: String,
    val episodeTitle: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val posterUrl: String?,
    val positionMs: Long,
    val durationMs: Long?,
    val lastEngagementMs: Long,
    val intentUri: String,
)

data class EpisodeNumbers(val season: Int, val episode: Int, val title: String?)

object WatchNextEntryMapper {
    /** Mirrors [com.iptvcinema.tv.core.player.ContinueWatchingResolver]'s synthesized next-up ids. */
    private const val NEXT_UP_PREFIX = "next-up:"

    fun from(
        item: WatchHistoryItem,
        display: WatchHistoryCardDisplay,
        episode: EpisodeNumbers?,
        profileId: String,
    ): WatchNextEntry? {
        val programKind = when (item.contentType) {
            WatchHistoryContentType.MOVIE -> WatchNextProgramKind.Movie
            WatchHistoryContentType.EPISODE -> WatchNextProgramKind.Episode
            WatchHistoryContentType.CHANNEL -> return null
        }
        val contentType = if (programKind == WatchNextProgramKind.Movie) "movie" else "episode"
        val seriesId = item.seriesId?.takeIf { it.isNotBlank() }
        val key = if (programKind == WatchNextProgramKind.Episode && seriesId != null) {
            "series:$seriesId"
        } else {
            "$contentType:${item.contentId}"
        }
        val isNextUp = item.id.startsWith(NEXT_UP_PREFIX) || item.positionMs <= 0L
        val resumeMs = item.positionMs.takeIf { !isNextUp && it > 0L }
        return WatchNextEntry(
            key = key,
            kind = if (isNextUp) WatchNextKind.Next else WatchNextKind.Continue,
            programKind = programKind,
            title = display.title,
            episodeTitle = episode?.title?.takeIf { programKind == WatchNextProgramKind.Episode && it.isNotBlank() },
            seasonNumber = episode?.season,
            episodeNumber = episode?.episode,
            posterUrl = display.backdropUrl?.takeIf { it.isNotBlank() } ?: display.posterUrl,
            positionMs = resumeMs ?: 0L,
            durationMs = item.durationMs?.takeIf { it > 0L },
            lastEngagementMs = item.lastWatchedAt.toEpochMilli(),
            intentUri = PlaybackDeepLink(
                contentType = contentType,
                contentId = item.contentId,
                seriesId = seriesId,
                profileId = profileId,
            ).toUri(),
        )
    }
}

/** What to change in the provider so it holds exactly the desired entries. */
data class WatchNextPlan(
    val inserts: List<WatchNextEntry>,
    val updates: List<Pair<Long, WatchNextEntry>>,
    val deletes: List<Long>,
)

object WatchNextDiff {
    /**
     * [existing] maps each of the app's provider rows (row id) to its key. Rows whose key is
     * wanted are updated in place; duplicates and rows no longer wanted are deleted.
     */
    fun plan(existing: List<Pair<Long, String?>>, desired: List<WatchNextEntry>): WatchNextPlan {
        val desiredByKey = desired.associateBy { it.key }
        val kept = mutableMapOf<String, Long>()
        val deletes = mutableListOf<Long>()
        existing.forEach { (rowId, key) ->
            if (key != null && key in desiredByKey && key !in kept) {
                kept[key] = rowId
            } else {
                deletes += rowId
            }
        }
        return WatchNextPlan(
            inserts = desiredByKey.values.filter { it.key !in kept },
            updates = kept.map { (key, rowId) -> rowId to desiredByKey.getValue(key) },
            deletes = deletes,
        )
    }
}
