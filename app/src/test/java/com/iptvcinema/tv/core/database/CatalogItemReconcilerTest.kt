package com.iptvcinema.tv.core.database

import com.iptvcinema.tv.core.catalog.CatalogChangeSummary
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogItemReconcilerTest {
    private data class Item(val id: String, val title: String)

    /** In-memory table that records whether each write ran inside a transaction. */
    private class FakeTable(initial: List<Item>) {
        val rows = initial.associateBy { it.id }.toMutableMap()
        var transactions = 0
        var writesOutsideTransaction = 0
        private var inTransaction = false

        suspend fun transaction(block: suspend () -> Unit) {
            check(!inTransaction) { "transactions must not nest" }
            transactions++
            inTransaction = true
            try {
                block()
            } finally {
                inTransaction = false
            }
        }

        fun upsert(items: List<Item>) {
            if (!inTransaction) writesOutsideTransaction++
            items.forEach { rows[it.id] = it }
        }

        fun delete(ids: List<String>) {
            if (!inTransaction) writesOutsideTransaction++
            ids.forEach { rows.remove(it) }
        }
    }

    private suspend fun FakeTable.reconcile(incoming: List<Item>): CatalogChangeSummary =
        CatalogItemReconciler.reconcile(
            incoming = incoming,
            idOf = Item::id,
            existingIds = { rows.keys.toList() },
            existingByIds = { ids -> ids.mapNotNull { rows[it] } },
            upsert = ::upsert,
            delete = ::delete,
            inTransaction = ::transaction,
        )

    @Test
    fun smallSync_addsUpdatesAndRemovesInOneTransactionPerStep() = runTest {
        val table = FakeTable(listOf(Item("1", "Same"), Item("2", "Old"), Item("3", "Gone")))

        val changes = table.reconcile(listOf(Item("1", "Same"), Item("2", "New"), Item("4", "Added")))

        assertEquals(CatalogChangeSummary(added = 1, updated = 1, removed = 1), changes)
        assertEquals(
            mapOf("1" to Item("1", "Same"), "2" to Item("2", "New"), "4" to Item("4", "Added")),
            table.rows,
        )
        // One transaction for the upserts, one for the removals.
        assertEquals(2, table.transactions)
        assertEquals(0, table.writesOutsideTransaction)
    }

    @Test
    fun unchangedSync_writesNothing() = runTest {
        val items = listOf(Item("1", "A"), Item("2", "B"))
        val table = FakeTable(items)

        val changes = table.reconcile(items)

        assertFalse(changes.hasChanges)
        assertEquals(items.associateBy { it.id }, table.rows)
        assertEquals(1, table.transactions)
    }

    @Test
    fun hugeSync_commitsInGroupsAndEndsInTheSameState() = runTest {
        val size = CatalogItemReconciler.ITEMS_PER_TRANSACTION * 2 + 7
        val stale = (0 until size).map { Item("old-$it", "Old") }
        val table = FakeTable(stale)
        val incoming = (0 until size).map { Item("new-$it", "New") }

        val changes = table.reconcile(incoming)

        assertEquals(CatalogChangeSummary(added = size, updated = 0, removed = size), changes)
        assertEquals(incoming.associateBy { it.id }, table.rows)
        // Three upsert groups and three removal groups instead of one long transaction.
        assertEquals(6, table.transactions)
        assertEquals(0, table.writesOutsideTransaction)
    }

    @Test
    fun duplicateProviderIds_keepTheLastPayload() = runTest {
        val table = FakeTable(emptyList())

        val changes = table.reconcile(listOf(Item("1", "First"), Item("1", "Last")))

        assertEquals(CatalogChangeSummary(added = 1), changes)
        assertEquals(mapOf("1" to Item("1", "Last")), table.rows)
    }

    @Test
    fun emptyProviderList_removesEverything() = runTest {
        val table = FakeTable(listOf(Item("1", "A"), Item("2", "B")))

        val changes = table.reconcile(emptyList())

        assertEquals(CatalogChangeSummary(removed = 2), changes)
        assertTrue(table.rows.isEmpty())
    }
}
