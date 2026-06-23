package com.iptvcinema.tv.features.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.tv.core.data.mapper.CatalogUiMapper.toPosterCardData
import com.iptvcinema.tv.core.data.repository.CatalogLoadState
import com.iptvcinema.tv.core.data.repository.CatalogRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.parental.ParentalGate
import com.iptvcinema.tv.core.design.components.PosterCardData
import com.iptvcinema.tv.core.model.SeriesItem
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.model.catalog.CatalogSeries
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class SeriesUiState(
    val loadState: CatalogLoadState = CatalogLoadState.Loading,
    val categories: List<String> = emptyList(),
    val series: List<SeriesItem> = emptyList(),
    val featured: SeriesItem? = null,
    val posters: List<PosterCardData> = emptyList(),
    val message: String? = null,
    val sourceStatus: SourceStatus? = null,
    val sourceType: SourceType? = null,
)

@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val appSessionRepository: AppSessionRepository,
    private val parentalControlsRepository: ParentalControlsRepository,
    private val parentalGate: ParentalGate,
) : ViewModel() {
    private val selectedCategory = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(SeriesUiState())
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                appSessionRepository.sessionState,
                selectedCategory,
            ) { session, categoryName ->
                session to categoryName
            }.flatMapLatest { (session, categoryName) ->
                val controlsFlow = session.currentProfileId?.let { profileId ->
                    parentalControlsRepository.observeControls(profileId)
                } ?: flowOf(null)
                combine(
                    catalogRepository.observeSeries(categoryName),
                    controlsFlow,
                ) { browseState, controls ->
                    browseState to controls
                }
            }.collect { (state, controls) ->
                val filteredCategories = if (controls != null) {
                    parentalGate.filterCategoryNames(state.categories, controls)
                } else {
                    state.categories
                }
                val seriesItems = if (controls != null) {
                    state.items.filter { series ->
                        !parentalGate.isContentBlocked(
                            series.genres.firstOrNull(),
                            series.rating,
                            controls,
                        )
                    }
                } else {
                    state.items
                }
                _uiState.value = SeriesUiState(
                    loadState = state.loadState,
                    categories = filteredCategories,
                    series = seriesItems,
                    featured = seriesItems.firstOrNull(),
                    posters = seriesItems.map { series ->
                        CatalogSeries(
                            id = series.id,
                            sourceId = "",
                            title = series.title,
                            posterUrl = series.imageUrl,
                            backdropUrl = series.backdropUrl,
                            categoryId = null,
                            categoryName = series.genres.firstOrNull(),
                            plot = series.plot,
                            rating = series.rating,
                            year = series.year,
                        ).toPosterCardData()
                    },
                    message = state.message,
                    sourceStatus = state.sourceStatus,
                    sourceType = state.sourceType,
                )
            }
        }
    }

    fun selectCategory(categoryName: String?) {
        selectedCategory.value = categoryName
    }
}
