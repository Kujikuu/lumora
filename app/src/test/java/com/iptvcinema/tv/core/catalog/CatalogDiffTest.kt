package com.iptvcinema.tv.core.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogDiffTest {
    private data class Item(val id: String, val title: String)

    @Test
    fun upserts_returnsOnlyNewAndChangedItems() {
        val existing = listOf(Item("1", "Same"), Item("2", "Old"))
        val incoming = listOf(Item("1", "Same"), Item("2", "New"), Item("3", "Added"))

        val diff = CatalogDiff.upserts(incoming, existing, Item::id)

        assertEquals(listOf("2", "3"), diff.items.map { it.id })
        assertEquals(1, diff.added)
        assertEquals(1, diff.updated)
    }

    @Test
    fun upserts_deduplicatesProviderIdsUsingLastValue() {
        val diff = CatalogDiff.upserts(
            incoming = listOf(Item("1", "Old payload"), Item("1", "Latest payload")),
            existing = emptyList(),
            idOf = Item::id,
        )

        assertEquals(listOf(Item("1", "Latest payload")), diff.items)
        assertEquals(1, diff.added)
    }

    @Test
    fun removedIds_returnsOnlyItemsMissingFromProvider() {
        assertEquals(setOf("1", "3"), CatalogDiff.removedIds(setOf("1", "2", "3"), setOf("2", "4")))
    }
}
