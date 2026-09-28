package com.iptvcinema.tv.core.util

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.iptvcinema.tv.core.platform.AppLocaleHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Strings for code outside Compose (view models, workers). The application context keeps the
 * system language, so lookups go through a context in the language chosen in Settings.
 */
@Singleton
class AppStrings @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @Volatile
    private var localized: Pair<String, Context>? = null

    fun get(@StringRes id: Int, vararg formatArgs: Any): String =
        localizedContext().getString(id, *formatArgs)

    fun getQuantity(@PluralsRes id: Int, quantity: Int, vararg formatArgs: Any): String =
        localizedContext().resources.getQuantityString(id, quantity, *formatArgs)

    private fun localizedContext(): Context {
        val language = AppLocaleHelper.currentLanguageTag(context)
        localized?.let { (cachedLanguage, cachedContext) -> if (cachedLanguage == language) return cachedContext }
        return AppLocaleHelper.wrap(context).also { localized = language to it }
    }
}
