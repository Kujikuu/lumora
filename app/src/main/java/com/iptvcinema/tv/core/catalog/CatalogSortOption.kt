package com.iptvcinema.tv.core.catalog

enum class CatalogSortOption {
    TITLE_AZ,
    YEAR,
}

fun <T> List<T>.sortedByCatalogOption(
    option: CatalogSortOption,
    titleSelector: (T) -> String,
    yearSelector: (T) -> Int,
): List<T> = when (option) {
    CatalogSortOption.TITLE_AZ -> sortedBy { titleSelector(it).lowercase() }
    CatalogSortOption.YEAR -> sortedByDescending { yearSelector(it) }
}
