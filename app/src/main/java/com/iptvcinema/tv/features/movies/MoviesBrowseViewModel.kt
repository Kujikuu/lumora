package com.iptvcinema.tv.features.movies

import com.iptvcinema.tv.core.data.repository.CatalogBrowseState
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.model.WatchHistoryItem
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.core.model.home.MoviesCatalogSnapshot
import com.iptvcinema.tv.core.model.home.MoviesPersonalSnapshot
import com.iptvcinema.tv.features.browse.BrowseSessionKey
import com.iptvcinema.tv.features.browse.BrowseTabDependencies
import com.iptvcinema.tv.features.browse.BrowseTabViewModel
import com.iptvcinema.tv.features.home.HomeSection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** The Movies tab: Home-style rails of movies (see [MoviesSectionsBuilder]). */
@HiltViewModel
class MoviesBrowseViewModel @Inject constructor(
    deps: BrowseTabDependencies,
) : BrowseTabViewModel<MoviesCatalogSnapshot, MoviesPersonalSnapshot>(deps) {
    override val continueWatchingType = WatchHistoryContentType.MOVIE
    override val emptyCatalog = MoviesCatalogSnapshot()
    override val emptyPersonal = MoviesPersonalSnapshot()

    init {
        start()
    }

    override fun observeAvailability(): Flow<CatalogBrowseState<Unit>> =
        deps.catalogRepository.observeMoviesAvailability()

    override suspend fun loadCatalog(session: BrowseSessionKey): MoviesCatalogSnapshot =
        deps.browseContentRepository.loadMoviesCatalog(session.sourceId, session.isDemoMode)

    override suspend fun loadPersonal(
        session: BrowseSessionKey,
        history: List<WatchHistoryItem>,
        isCategoryBlocked: (String) -> Boolean,
    ): MoviesPersonalSnapshot =
        deps.browseContentRepository.loadMoviesPersonal(session.sourceId, session.isDemoMode, history, isCategoryBlocked)

    override fun buildSections(
        catalog: MoviesCatalogSnapshot,
        personal: MoviesPersonalSnapshot,
        continueWatching: List<HomeContentCard>,
        isBlocked: (categoryName: String?, rating: String?) -> Boolean,
        isFavorite: (contentId: String, type: FavoriteContentType) -> Boolean,
    ): List<HomeSection> = MoviesSectionsBuilder.build(
        catalog = catalog,
        personal = personal,
        continueWatching = continueWatching,
        isBlocked = isBlocked,
        isFavorite = isFavorite,
        countLabel = ::titleCountLabel,
    )
}
