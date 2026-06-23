package com.iptvcinema.tv.features.details

import com.iptvcinema.tv.core.catalog.SeasonGrouping
import com.iptvcinema.tv.core.model.SeasonItem
import com.iptvcinema.tv.core.model.catalog.CatalogEpisode

internal object DetailsSeasonGrouping {
    fun toSeasonItems(
        episodes: List<CatalogEpisode>,
        seriesId: String,
        additionalSeasonNumbers: List<Int> = emptyList(),
    ): List<SeasonItem> = SeasonGrouping.toSeasonItems(episodes, seriesId, additionalSeasonNumbers)
}
