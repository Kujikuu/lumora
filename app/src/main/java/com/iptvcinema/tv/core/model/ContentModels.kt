package com.iptvcinema.tv.core.model

enum class ProfileType { MAIN, FAMILY, KIDS, GUEST }

enum class SourceType { XTREAM_CODES, M3U, DEMO }

enum class SourceStatus { ACTIVE, SYNCING, NEEDS_ATTENTION, EXPIRED, FAILED }

data class UserProfile(
    val id: String,
    val name: String,
    val type: ProfileType,
    val avatarInitial: String,
)

data class MovieItem(
    val id: String,
    val title: String,
    val year: Int,
    val runtimeMinutes: Int,
    val rating: String,
    val plot: String,
    val genres: List<String>,
    val is4K: Boolean = false,
    val progress: Float? = null,
    val isFavorite: Boolean = false,
    val imageUrl: String? = null,
    val backdropUrl: String? = null,
    val sortOrder: Int = 0,
    val addedAt: Long? = null,
)

data class EpisodeItem(
    val id: String,
    val episodeNumber: Int,
    val title: String,
    val durationMinutes: Int,
    val progress: Float? = null,
    val thumbnailUrl: String? = null,
)

data class SeasonItem(
    val id: String,
    val seasonNumber: Int,
    val episodes: List<EpisodeItem>,
)

data class SeriesItem(
    val id: String,
    val title: String,
    val year: Int,
    val rating: String,
    val plot: String,
    val genres: List<String>,
    val seasonCount: Int,
    val is4K: Boolean = false,
    val hasNewEpisode: Boolean = false,
    val progress: Float? = null,
    val isFavorite: Boolean = false,
    val imageUrl: String? = null,
    val backdropUrl: String? = null,
    val seasons: List<SeasonItem> = emptyList(),
    val sortOrder: Int = 0,
    val addedAt: Long? = null,
)

data class ChannelItem(
    val id: String,
    val name: String,
    val logoUrl: String? = null,
    val channelNumber: Int = 0,
    val category: String,
    val currentProgram: String,
    val programDescription: String = "",
    val programStart: String,
    val programEnd: String,
    val programProgress: Float,
    val qualityBadge: String? = null,
)

data class EpgProgram(
    val id: String,
    val channelId: String,
    val title: String,
    val startHour: Int,
    val startMinute: Int,
    val durationMinutes: Int,
    val startEpochMs: Long = 0L,
    val endEpochMs: Long = 0L,
    val description: String = "",
)

data class PlaylistSourceItem(
    val id: String,
    val name: String,
    val type: SourceType,
    val status: SourceStatus,
    val channelCount: Int?,
    val movieCount: Int? = null,
    val seriesCount: Int? = null,
    val lastSynced: String,
    val epgAvailable: Boolean = true,
)

data class CastMember(
    val name: String,
    val role: String,
)

data class AccountSummary(
    val name: String,
    val email: String,
    val plan: String,
    val renewalDate: String,
)

data class SearchResults(
    val movies: List<MovieItem>,
    val series: List<SeriesItem>,
    val channels: List<ChannelItem>,
)

data class ValidationStatus(
    val label: String,
    val isSuccess: Boolean,
)
