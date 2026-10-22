package com.iptvcinema.launcher.apps

import java.text.Collator
import java.util.Locale

/** Apps row order: the main app first, TV apps before touch-only apps, then by name. */
object AppListSorter {
    fun sort(apps: List<AppEntry>, locale: Locale, pinnedFirst: String? = null): List<AppEntry> {
        val collator = Collator.getInstance(locale)
        return apps.sortedWith(
            compareBy<AppEntry> { if (it.packageName == pinnedFirst) 0 else 1 }
                .thenBy { if (it.isLeanback) 0 else 1 }
                .thenComparator { a, b -> collator.compare(a.label, b.label) },
        )
    }

    /** Apps the row never shows: the launcher itself and the box's other home apps. */
    fun hidden(apps: List<AppEntry>, selfPackage: String, homePackages: Set<String>): List<AppEntry> =
        apps.filterNot { it.packageName == selfPackage || it.packageName in homePackages }
}
