package com.iptvcinema.tv.features.details

import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.model.catalog.CatalogEpisode
import com.iptvcinema.tv.core.model.catalog.CatalogSeries
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.player.EpisodeSequenceHelper
import com.iptvcinema.tv.core.player.EpisodeWatchProgress
import com.iptvcinema.tv.core.player.EpisodeWatchVisualState
import com.iptvcinema.tv.core.player.WatchHistoryResumePolicy
import com.iptvcinema.tv.features.home.HomeRailTitle
import com.iptvcinema.tv.features.home.HomeSection
import com.iptvcinema.tv.features.home.HomeUiMapper

/** Texts the episode rails need, resolved by the caller in the viewer's language. */
data class EpisodeLabels(
    val seasonTitle: (seasonNumber: Int, episodeCount: Int) -> String,
    val episodeTitle: (episodeNumber: Int) -> String,
    val watched: String,
)

/** The series page's episode list: one rail per season and where focus should start. */
data class SeriesEpisodesContent(
    val sections: List<HomeSection>,
    /** The season rail id and episode index focus starts on: the episode to resume or play next. */
    val resumeFocus: Pair<String, Int>?,
    val resumeEpisodeId: String?,
)

object SeriesEpisodesBuilder {
    fun seasonId(seasonNumber: Int) = "season:$seasonNumber"

    fun build(
        series: CatalogSeries?,
        episodes: List<CatalogEpisode>,
        history: List<WatchHistoryItem>,
        labels: EpisodeLabels,
    ): SeriesEpisodesContent {
        val ordered = episodes.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))
        val progress = progressByEpisode(ordered, history)
        val resumeId = resumeEpisodeId(ordered, history)
        val seriesBackdrop = series?.backdropUrl?.takeIf { it.isNotBlank() } ?: series?.posterUrl?.takeIf { it.isNotBlank() }
        val sections = ordered.groupBy { it.seasonNumber }.map { (season, seasonEpisodes) ->
            HomeSection.Episodes(
                id = seasonId(season),
                title = HomeRailTitle(0, text = labels.seasonTitle(season, seasonEpisodes.size)),
                items = seasonEpisodes.map { episode -> episode.toCard(series, seriesBackdrop, progress[episode.id], labels) },
            )
        }
        val resumeFocus = resumeId?.let { id ->
            ordered.firstOrNull { it.id == id }?.let { episode ->
                val index = sections.first { it.id == seasonId(episode.seasonNumber) }.items.indexOfFirst { it.contentId == id }
                seasonId(episode.seasonNumber) to index
            }
        }
        return SeriesEpisodesContent(sections, resumeFocus, resumeId)
    }

    /**
     * The episode to resume: the one watched last while still in progress, or the one after it
     * when it was finished. Null when nothing of the series was watched (start at the beginning).
     */
    fun resumeEpisodeId(episodes: List<CatalogEpisode>, history: List<WatchHistoryItem>): String? {
        val ids = episodes.mapTo(mutableSetOf()) { it.id }
        val last = history
            .filter { it.contentType == WatchHistoryContentType.EPISODE && it.contentId in ids }
            .maxByOrNull { it.lastWatchedAt }
            ?: return null
        if (!WatchHistoryResumePolicy.isNearEnd(last.positionMs, last.durationMs)) return last.contentId
        return EpisodeSequenceHelper.nextEpisode(episodes, last.contentId)?.id ?: last.contentId
    }

    private fun progressByEpisode(episodes: List<CatalogEpisode>, history: List<WatchHistoryItem>): Map<String, Float> {
        val ids = episodes.mapTo(mutableSetOf()) { it.id }
        return history
            .filter { it.contentType == WatchHistoryContentType.EPISODE && it.contentId in ids }
            .groupBy { it.contentId }
            .mapNotNull { (id, rows) ->
                val latest = rows.maxBy { it.lastWatchedAt }
                val fraction = EpisodeWatchProgress.normalized(latest.positionMs, latest.durationMs)
                    ?: return@mapNotNull null
                // Finished episodes show a full bar, even if the credits were skipped.
                val shown = if (WatchHistoryResumePolicy.isNearEnd(latest.positionMs, latest.durationMs)) 1f else fraction
                id to shown
            }
            .toMap()
    }

    private fun CatalogEpisode.toCard(
        series: CatalogSeries?,
        seriesBackdrop: String?,
        progress: Float?,
        labels: EpisodeLabels,
    ): HomeContentCard {
        val code = HomeUiMapper.episodeSubtitle(seasonNumber, episodeNumber, "")
        val watched = EpisodeWatchProgress.visualState(progress) == EpisodeWatchVisualState.WATCHED
        val details = listOfNotNull(
            code.takeIf { it.isNotBlank() },
            durationMinutes?.takeIf { it > 0 }?.let { "${it}m" },
            labels.watched.takeIf { watched },
        )
        val thumbnail = thumbnailUrl?.takeIf { it.isNotBlank() }
        return HomeContentCard(
            contentId = id,
            contentType = "episode",
            seriesId = seriesId,
            title = title.takeIf { it.isNotBlank() } ?: labels.episodeTitle(episodeNumber),
            subtitle = details.joinToString(" · "),
            imageUrl = thumbnail ?: seriesBackdrop,
            // The series backdrop stays put while moving between episodes; stills are too small.
            backdropUrl = seriesBackdrop ?: thumbnail,
            plot = plot?.takeIf { it.isNotBlank() } ?: series?.plot?.takeIf { it.isNotBlank() },
            rating = series?.rating,
            progress = progress,
        )
    }
}
