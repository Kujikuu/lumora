package com.iptvcinema.tv.core.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLocaleHelperTest {
    @Test
    fun savedChoice_winsOverSystem() {
        assertEquals("ar", AppLocaleHelper.resolveLanguage(saved = "ar", systemLanguage = "en"))
        assertEquals("en", AppLocaleHelper.resolveLanguage(saved = "en", systemLanguage = "ar"))
    }

    @Test
    fun noChoice_followsSupportedSystemLanguage() {
        assertEquals("ar", AppLocaleHelper.resolveLanguage(saved = null, systemLanguage = "ar"))
        assertEquals("en", AppLocaleHelper.resolveLanguage(saved = null, systemLanguage = "en"))
    }

    @Test
    fun unsupportedLanguages_fallBackToEnglish() {
        assertEquals("en", AppLocaleHelper.resolveLanguage(saved = null, systemLanguage = "fr"))
        assertEquals("en", AppLocaleHelper.resolveLanguage(saved = "xx", systemLanguage = "fr"))
    }

    @Test
    fun arabic_formatsNumbersWithWesternDigits() {
        val locale = AppLocaleHelper.localeFor("ar")

        assertEquals("ar", locale.language)
        assertEquals("S1 · 25", String.format(locale, "S%d · %d", 1, 25))
    }
}
