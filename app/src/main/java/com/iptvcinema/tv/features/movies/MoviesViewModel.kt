package com.iptvcinema.tv.features.movies

import com.iptvcinema.tv.core.catalog.CatalogSortOption
import com.iptvcinema.tv.core.catalog.sortedByCatalogOption
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toPosterCardData
import com.iptvcinema.tv.core.data.repository.CatalogBrowseState
import com.iptvcinema.tv.core.design.components.PosterCardData
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.features.catalog.CatalogGridDependencies
import com.iptvcinema.tv.features.catalog.CatalogGridViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** The full movie grid of one category (reached from the Movies tab's category tiles). */
@HiltViewModel
class MoviesViewModel @Inject constructor(
    deps: CatalogGridDependencies,
) : CatalogGridViewModel<MovieItem>(deps) {
    override val favoriteType = FavoriteContentType.MOVIE

    init {
        start()
    }

    override fun observeItems(categoryName: String?): Flow<CatalogBrowseState<MovieItem>> =
        deps.catalogRepository.observeMovies(categoryName)

    override fun MovieItem.itemId() = id
    override fun MovieItem.itemTitle() = title
    override fun MovieItem.itemPosterUrl() = imageUrl
    override fun MovieItem.itemCategory() = categoryName
    override fun MovieItem.itemRating() = rating

    override fun List<MovieItem>.sortedBy(option: CatalogSortOption): List<MovieItem> = sortedByCatalogOption(
        option = option,
        titleSelector = { it.title },
        yearSelector = { it.year },
        sortOrderSelector = { it.sortOrder },
        addedAtSelector = { it.addedAt },
    )

    override fun MovieItem.toPoster(isFavorite: Boolean): PosterCardData =
        copy(isFavorite = isFavorite).toPosterCardData()
}
