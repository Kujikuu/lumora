package com.iptvcinema.tv.core.catalog

/**
 * Continue-watching cards should show series artwork. Watch history often stores an
 * episode thumbnail that is missing or fails to load even when the series poster exists.
 */
internal fun resolveEpisodeContinueWatchingPosterUrl(
    seriesPosterUrl: String?,
    episodeThumbnailUrl: String?,
    storedPosterUrl: String?,
): String? =
    seriesPosterUrl?.takeIf { it.isNotBlank() }
        ?: episodeThumbnailUrl?.takeIf { it.isNotBlank() }
        ?: storedPosterUrl?.takeIf { it.isNotBlank() }

internal fun resolveEpisodeContinueWatchingBackdropUrl(
    seriesBackdropUrl: String?,
): String? = seriesBackdropUrl?.takeIf { it.isNotBlank() }
