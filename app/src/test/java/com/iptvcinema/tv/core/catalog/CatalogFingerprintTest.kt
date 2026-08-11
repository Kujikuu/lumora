package com.iptvcinema.tv.core.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CatalogFingerprintTest {
    @Test
    fun sameNormalizedItems_produceSameFingerprint() {
        assertEquals(CatalogFingerprint.of(listOf("a", "b")), CatalogFingerprint.of(listOf("a", "b")))
    }

    @Test
    fun contentOrOrderChange_changesFingerprint() {
        val original = CatalogFingerprint.of(listOf("a", "b"))
        assertNotEquals(original, CatalogFingerprint.of(listOf("a", "c")))
        assertNotEquals(original, CatalogFingerprint.of(listOf("b", "a")))
    }
}
