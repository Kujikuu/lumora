package com.iptvcinema.tv.features.series

import com.iptvcinema.tv.core.catalog.CatalogSortOption
import com.iptvcinema.tv.core.catalog.sortedByCatalogOption
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toPosterCardData
import com.iptvcinema.tv.core.data.repository.CatalogBrowseState
import com.iptvcinema.tv.core.design.components.PosterCardData
import com.iptvcinema.tv.core.model.FavoriteContentType
import com.iptvcinema.tv.core.model.SeriesItem
import com.iptvcinema.tv.features.catalog.CatalogGridDependencies
import com.iptvcinema.tv.features.catalog.CatalogGridViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** The full series grid of one category (reached from the Series tab's category tiles). */
@HiltViewModel
class SeriesViewModel @Inject constructor(
    deps: CatalogGridDependencies,
) : CatalogGridViewModel<SeriesItem>(deps) {
    override val favoriteType = FavoriteContentType.SERIES

    init {
        start()
    }

    override fun observeItems(categoryName: String?): Flow<CatalogBrowseState<SeriesItem>> =
        deps.catalogRepository.observeSeries(categoryName)

    override fun SeriesItem.itemId() = id
    override fun SeriesItem.itemTitle() = title
    override fun SeriesItem.itemPosterUrl() = imageUrl
    override fun SeriesItem.itemCategory() = categoryName
    override fun SeriesItem.itemRating() = rating

    override fun List<SeriesItem>.sortedBy(option: CatalogSortOption): List<SeriesItem> = sortedByCatalogOption(
        option = option,
        titleSelector = { it.title },
        yearSelector = { it.year },
        sortOrderSelector = { it.sortOrder },
        addedAtSelector = { it.addedAt },
    )

    override fun SeriesItem.toPoster(isFavorite: Boolean): PosterCardData =
        copy(isFavorite = isFavorite).toPosterCardData()
}
