package com.iptvcinema.tv.features.series

import com.iptvcinema.tv.core.data.repository.CatalogBrowseState
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.SeriesCatalogSnapshot
import com.iptvcinema.tv.core.model.home.SeriesPersonalSnapshot
import com.iptvcinema.tv.features.browse.BrowseSessionKey
import com.iptvcinema.tv.features.browse.BrowseTabDependencies
import com.iptvcinema.tv.features.browse.BrowseTabViewModel
import com.iptvcinema.tv.features.home.HomeSection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** The Series tab: Home-style rails of series (see [SeriesSectionsBuilder]). */
@HiltViewModel
class SeriesBrowseViewModel @Inject constructor(
    deps: BrowseTabDependencies,
) : BrowseTabViewModel<SeriesCatalogSnapshot, SeriesPersonalSnapshot>(deps) {
    override val continueWatchingType = WatchHistoryContentType.EPISODE
    override val emptyCatalog = SeriesCatalogSnapshot()
    override val emptyPersonal = SeriesPersonalSnapshot()

    init {
        start()
    }

    override fun observeAvailability(): Flow<CatalogBrowseState<Unit>> =
        deps.catalogRepository.observeSeriesAvailability()

    override suspend fun loadCatalog(session: BrowseSessionKey): SeriesCatalogSnapshot =
        deps.browseContentRepository.loadSeriesCatalog(session.sourceId, session.isDemoMode)

    override suspend fun loadPersonal(
        session: BrowseSessionKey,
        history: List<WatchHistoryItem>,
        isCategoryBlocked: (String) -> Boolean,
    ): SeriesPersonalSnapshot =
        deps.browseContentRepository.loadSeriesPersonal(session.sourceId, session.isDemoMode, history, isCategoryBlocked)

    override fun buildSections(
        catalog: SeriesCatalogSnapshot,
        personal: SeriesPersonalSnapshot,
        continueWatching: List<HomeContentCard>,
        isBlocked: (categoryName: String?, rating: String?) -> Boolean,
        isFavorite: (contentId: String, type: FavoriteContentType) -> Boolean,
    ): List<HomeSection> = SeriesSectionsBuilder.build(
        catalog = catalog,
        personal = personal,
        continueWatching = continueWatching,
        isBlocked = isBlocked,
        isFavorite = isFavorite,
        countLabel = ::titleCountLabel,
    )
}
