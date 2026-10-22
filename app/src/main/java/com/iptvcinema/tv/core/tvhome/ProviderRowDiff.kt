package com.iptvcinema.tv.core.tvhome

/** What to change in a TV provider table so it holds exactly the desired rows. */
data class ProviderRowPlan<T>(
    val inserts: List<T>,
    val updates: List<Pair<Long, T>>,
    val deletes: List<Long>,
)

object ProviderRowDiff {
    /**
     * [existing] maps each of the app's provider rows (row id) to its key. Rows whose key is
     * wanted are updated in place; duplicates and rows no longer wanted are deleted.
     */
    fun <T> plan(existing: List<Pair<Long, String?>>, desired: List<T>, keyOf: (T) -> String): ProviderRowPlan<T> {
        val desiredByKey = desired.associateBy(keyOf)
        val kept = mutableMapOf<String, Long>()
        val deletes = mutableListOf<Long>()
        existing.forEach { (rowId, key) ->
            if (key != null && key in desiredByKey && key !in kept) {
                kept[key] = rowId
            } else {
                deletes += rowId
            }
        }
        return ProviderRowPlan(
            inserts = desiredByKey.values.filter { keyOf(it) !in kept },
            updates = kept.map { (key, rowId) -> rowId to desiredByKey.getValue(key) },
            deletes = deletes,
        )
    }
}
