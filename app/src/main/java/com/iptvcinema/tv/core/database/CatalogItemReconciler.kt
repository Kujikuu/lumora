package com.iptvcinema.tv.core.database

import com.iptvcinema.tv.core.catalog.CatalogChangeSummary
import com.iptvcinema.tv.core.catalog.CatalogDiff
import com.iptvcinema.tv.core.catalog.CatalogSyncPolicy

/** Runs [block] inside one database transaction. */
typealias TransactionRunner = suspend (block: suspend () -> Unit) -> Unit

/**
 * Brings one catalog table in line with the provider's list: upserts new or changed rows and
 * deletes rows the provider dropped.
 *
 * Work is committed in groups of [ITEMS_PER_TRANSACTION] rows so a first sync of a huge catalog
 * does not hold the write lock for its whole length; other writers (watch progress, favorites)
 * get a turn between groups. Groups are large so a normal sync still commits once per table and
 * observers re-query only a few times. The end state is the same as one big transaction; if the
 * sync stops half way, the caller has not saved the sync metadata yet, so the next sync redoes it.
 */
internal object CatalogItemReconciler {
    const val ITEMS_PER_TRANSACTION = CatalogSyncPolicy.DATABASE_BATCH_SIZE * 10

    suspend fun <T> reconcile(
        incoming: List<T>,
        idOf: (T) -> String,
        existingIds: suspend () -> List<String>,
        existingByIds: suspend (List<String>) -> List<T>,
        upsert: suspend (List<T>) -> Unit,
        delete: suspend (List<String>) -> Unit,
        inTransaction: TransactionRunner,
    ): CatalogChangeSummary {
        val uniqueIncoming = incoming.associateBy(idOf).values.toList()
        val localIds = existingIds().toHashSet()
        val incomingIds = uniqueIncoming.mapTo(hashSetOf(), idOf)
        var added = 0
        var updated = 0
        uniqueIncoming.chunked(ITEMS_PER_TRANSACTION).forEach { group ->
            inTransaction {
                CatalogSyncPolicy.databaseBatches(group).forEach { chunk ->
                    val diff = CatalogDiff.upserts(chunk, existingByIds(chunk.map(idOf)), idOf)
                    added += diff.added
                    updated += diff.updated
                    if (diff.items.isNotEmpty()) upsert(diff.items)
                }
            }
        }
        val removed = CatalogDiff.removedIds(localIds, incomingIds).toList()
        removed.chunked(ITEMS_PER_TRANSACTION).forEach { group ->
            inTransaction {
                CatalogSyncPolicy.databaseBatches(group).forEach { delete(it) }
            }
        }
        return CatalogChangeSummary(added = added, updated = updated, removed = removed.size)
    }
}
