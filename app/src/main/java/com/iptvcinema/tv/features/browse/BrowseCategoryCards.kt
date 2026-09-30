package com.iptvcinema.tv.features.browse

import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.model.home.BrowseCategory
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.features.home.HomeRailTitle
import com.iptvcinema.tv.features.home.HomeSection

/**
 * The "Browse by category" rail: one tile per category that is not blocked, largest first.
 * [countLabel] formats the title count shown under the name in the spotlight.
 */
fun categoriesSection(
    categories: List<BrowseCategory>,
    cardType: String,
    isBlocked: (String) -> Boolean,
    countLabel: (Int) -> String,
): HomeSection.Categories? =
    categories
        .filter { it.name.isNotBlank() && it.itemCount > 0 && !isBlocked(it.name) }
        .distinctBy { it.name }
        .map { category ->
            HomeContentCard(
                contentId = category.name,
                contentType = cardType,
                title = category.name,
                subtitle = countLabel(category.itemCount),
                imageUrl = category.posterUrl,
                backdropUrl = category.backdropUrl ?: category.posterUrl,
            )
        }
        .takeIf { it.isNotEmpty() }
        ?.let { HomeSection.Categories(HomeRailTitle(R.string.browse_by_category), it) }
