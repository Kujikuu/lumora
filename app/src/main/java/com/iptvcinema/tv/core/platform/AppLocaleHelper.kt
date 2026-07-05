package com.iptvcinema.tv.core.platform

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppLocaleHelper {
    const val LANGUAGE_EN = "en"
    const val LANGUAGE_AR = "ar"

    fun currentLanguageTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return when {
            locales.isEmpty -> LANGUAGE_EN
            else -> locales[0]?.language ?: LANGUAGE_EN
        }
    }

    fun applyLanguage(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }
}
