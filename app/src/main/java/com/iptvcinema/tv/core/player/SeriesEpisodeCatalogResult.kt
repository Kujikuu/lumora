package com.iptvcinema.tv.core.player

import com.iptvcinema.tv.core.model.CastMember
import com.iptvcinema.tv.core.model.catalog.CatalogEpisode

data class SeriesEpisodeCatalogResult(
    val episodes: List<CatalogEpisode>,
    val seasonNumbers: List<Int> = emptyList(),
    val plot: String? = null,
    val rating: String? = null,
    val cast: List<CastMember> = emptyList(),
    val genres: List<String> = emptyList(),
)
