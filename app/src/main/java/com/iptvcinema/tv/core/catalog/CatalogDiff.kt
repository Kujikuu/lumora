package com.iptvcinema.tv.core.catalog

data class CatalogUpsertDiff<T>(
    val items: List<T>,
    val added: Int,
    val updated: Int,
)

object CatalogDiff {
    fun <T> upserts(
        incoming: List<T>,
        existing: List<T>,
        idOf: (T) -> String,
    ): CatalogUpsertDiff<T> {
        val currentById = existing.associateBy(idOf)
        val changed = incoming.associateBy(idOf).values.filter { item -> currentById[idOf(item)] != item }
        val added = changed.count { idOf(it) !in currentById }
        return CatalogUpsertDiff(
            items = changed,
            added = added,
            updated = changed.size - added,
        )
    }

    fun removedIds(existingIds: Set<String>, incomingIds: Set<String>): Set<String> =
        existingIds - incomingIds
}
