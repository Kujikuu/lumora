package com.iptvcinema.tv.core.tvhome

import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderRowDiffTest {
    @Test
    fun plan_updatesKeptRowsInsertsNewAndDeletesTheRest() {
        val existing = listOf(1L to "a", 2L to "b", 3L to "a", 4L to null)
        val plan = ProviderRowDiff.plan(existing, listOf("a", "c")) { it }
        assertEquals(listOf("c"), plan.inserts)
        assertEquals(listOf(1L to "a"), plan.updates)
        assertEquals(listOf(2L, 3L, 4L), plan.deletes)
    }
}
