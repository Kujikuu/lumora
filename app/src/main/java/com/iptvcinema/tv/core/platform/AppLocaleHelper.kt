package com.iptvcinema.tv.core.platform

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/**
 * App language chosen in Settings.
 *
 * Android TV builds often ship without the system per-app language service, so
 * AppCompatDelegate.setApplicationLocales silently does nothing there. The choice is kept
 * here instead and applied to each activity's context in attachBaseContext.
 */
object AppLocaleHelper {
    const val LANGUAGE_EN = "en"
    const val LANGUAGE_AR = "ar"

    private val SUPPORTED = setOf(LANGUAGE_EN, LANGUAGE_AR)
    private const val PREFS = "app_locale"
    private const val KEY_LANGUAGE = "language"

    fun resolveLanguage(saved: String?, systemLanguage: String): String = when {
        saved != null && saved in SUPPORTED -> saved
        systemLanguage in SUPPORTED -> systemLanguage
        else -> LANGUAGE_EN
    }

    fun currentLanguageTag(context: Context): String =
        resolveLanguage(savedLanguage(context), systemLanguage())

    /** Wraps [base] so resources, layout direction and formatting use the chosen language. */
    fun wrap(base: Context): Context {
        val locale = localeFor(currentLanguageTag(base))
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(config)
    }

    /**
     * Arabic uses Western digits (1 2 3), like most Arabic apps; the "nu-latn" extension keeps
     * the Arabic strings and right-to-left layout while numbers format as Latin digits.
     */
    fun localeFor(languageTag: String): Locale =
        if (languageTag == LANGUAGE_AR) Locale.forLanguageTag("ar-u-nu-latn") else Locale.forLanguageTag(languageTag)

    /** Saves the choice and restarts the activity so every screen picks it up. */
    fun applyLanguage(activity: Activity, languageTag: String) {
        if (languageTag == currentLanguageTag(activity)) return
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageTag)
            .commit()
        activity.recreate()
    }

    private fun savedLanguage(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null)

    private fun systemLanguage(): String =
        Resources.getSystem().configuration.locales[0]?.language ?: LANGUAGE_EN
}
