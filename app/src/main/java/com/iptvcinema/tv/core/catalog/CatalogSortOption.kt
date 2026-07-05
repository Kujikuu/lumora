package com.iptvcinema.tv.core.catalog

enum class CatalogSortOption {
    TITLE_AZ,
    YEAR,
    NEWEST,
}

fun <T> List<T>.sortedByCatalogOption(
    option: CatalogSortOption,
    titleSelector: (T) -> String,
    yearSelector: (T) -> Int,
    sortOrderSelector: (T) -> Int = { 0 },
    addedAtSelector: (T) -> Long? = { null },
): List<T> = when (option) {
    CatalogSortOption.TITLE_AZ -> sortedBy { titleSelector(it).lowercase() }
    CatalogSortOption.YEAR -> sortedByDescending { yearSelector(it) }
    CatalogSortOption.NEWEST -> sortedWith(
        compareByDescending<T> { addedAtSelector(it) ?: Long.MIN_VALUE }
            .thenBy { sortOrderSelector(it) },
    )
}
