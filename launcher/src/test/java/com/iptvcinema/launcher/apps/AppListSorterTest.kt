package com.iptvcinema.launcher.apps

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class AppListSorterTest {
    private fun app(pkg: String, label: String, leanback: Boolean) = AppEntry(pkg, label, leanback, isSystem = false, lastUpdateTimeMs = 0)

    @Test
    fun sort_pinnedThenTvAppsThenByName() {
        val sorted = AppListSorter.sort(
            listOf(app("z", "Zebra", true), app("t", "touch", false), app("a", "apple", true), app("main", "Lumora Play", true)),
            Locale.ENGLISH,
            pinnedFirst = "main",
        )
        assertEquals(listOf("main", "a", "z", "t"), sorted.map { it.packageName })
    }

    @Test
    fun sort_usesTheLocaleCollatorForArabicNames() {
        val sorted = AppListSorter.sort(listOf(app("b", "يوتيوب", true), app("a", "أفلام", true)), Locale("ar"))
        assertEquals(listOf("a", "b"), sorted.map { it.packageName })
    }

    @Test
    fun hidden_dropsTheLauncherAndOtherHomeApps() {
        val visible = AppListSorter.hidden(
            listOf(app("me", "Me", true), app("stock", "Stock", true), app("yt", "YouTube", true)),
            selfPackage = "me",
            homePackages = setOf("stock", "me"),
        )
        assertEquals(listOf("yt"), visible.map { it.packageName })
    }
}
