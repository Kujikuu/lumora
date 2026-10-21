package com.iptvcinema.tv.core.util

/**
 * Turns provider labels into display text. IPTV providers pack release tags and two languages
 * into names ("مطلوب عائليا - CAM", "Mohamed HENEDI | محمد هنيدي"); shown raw they read as noise.
 * Only display text changes: ids and the raw names used for navigation and filters stay as stored.
 */
object CatalogLabels {
    private const val QUALITY_TAGS = "HD-?CAM|CAM-?RIP|CAM|HD-?TS|TELESYNC|TS|HD-?TC|TC"
    private val prefixTag = Regex("""^\s*[\[(]?\s*(?:$QUALITY_TAGS)\s*[\])]?\s*[-|:–]\s*""", RegexOption.IGNORE_CASE)
    private val suffixTag = Regex("""\s*[-|:–]\s*[\[(]?\s*(?:$QUALITY_TAGS)\s*[\])]?\s*$""", RegexOption.IGNORE_CASE)
    private val bracketTag = Regex("""\s*[\[(]\s*(?:$QUALITY_TAGS)\s*[\])]\s*""", RegexOption.IGNORE_CASE)

    // Scene-style recording names: "WWE.RAW.19.10.2025", "UFC.310.Main.Card".
    private val dottedName = Regex("""^[\p{L}\p{N}]+(?:\.[\p{L}\p{N}]+){2,}""")
    private val versus = Regex("""\svs\.?\s""", RegexOption.IGNORE_CASE)
    private val date = Regex("""\b\d{1,2}[./-]\d{1,2}[./-](?:\d{4}|\d{2})\b""")
    private val recordingCategoryWords = listOf(
        "sport", "football", "soccer", "match", "wwe", "ufc", "boxing", "league",
        "رياض", "مباريات", "مباراة", "كرة", "دوري",
    )
    private val upperWord = Regex("""\b[A-Z]{4,}\b""")
    // A short provider marker before the name: "[X] Infinity HD", "(AR) Kids".
    private val providerTag = Regex("""^\s*[\[(][^\])]{1,6}[\])]\s*""")

    /** [raw] without release-quality tags such as "CAM" or "HDTS". */
    fun cleanTitle(raw: String): String {
        // Brackets first: "[HD-TS]" would otherwise lose only its "-TS]" to the suffix rule.
        val cleaned = raw
            .replace(bracketTag, " ")
            .replace(prefixTag, "")
            .replace(suffixTag, "")
            .trim()
        return cleaned.ifEmpty { raw.trim() }
    }

    /** Whether [raw] is a cinema-camera copy, which should never lead the Home hero. */
    fun isCamCopy(raw: String): Boolean =
        prefixTag.containsMatchIn(raw) || suffixTag.containsMatchIn(raw) || bracketTag.containsMatchIn(raw)

    /** Whether [raw] reads like a match or show recording rather than a film. */
    fun looksLikeRecording(raw: String): Boolean {
        val title = raw.trim()
        return (dottedName.containsMatchIn(title) && title.any(Char::isDigit)) ||
            versus.containsMatchIn(title) ||
            date.containsMatchIn(title)
    }

    /** Whether a category holds sports or wrestling recordings rather than films. */
    fun isRecordingCategory(raw: String?): Boolean {
        val name = raw?.lowercase() ?: return false
        return recordingCategoryWords.any { it in name }
    }

    /**
     * One language of a bilingual category name ("Mohamed HENEDI | محمد هنيدي"), with shouted
     * words ("ARABIC") in title case. Short capitals such as "TV" or "4K" are kept.
     */
    fun displayCategory(raw: String, preferArabic: Boolean): String {
        val untagged = raw.replace(providerTag, "").trim().ifEmpty { raw.trim() }
        val parts = languageParts(untagged)
        val chosen = when {
            parts.size < 2 -> parts.firstOrNull() ?: untagged
            preferArabic -> parts.firstOrNull(::hasArabic) ?: parts.first()
            else -> parts.firstOrNull { !hasArabic(it) } ?: parts.first()
        }
        return chosen.replace(upperWord) { match ->
            match.value.lowercase().replaceFirstChar(Char::uppercaseChar)
        }
    }

    /**
     * The name's parts when it holds two languages: split on "|", or on " - " when one side is
     * Arabic and the other is not ("Series Nonstop - 24 مسلسلات"). A plain "Action - Adventure"
     * stays whole.
     */
    private fun languageParts(name: String): List<String> {
        val byBar = name.split('|').map(String::trim).filter(String::isNotEmpty)
        if (byBar.size >= 2) return byBar
        val byDash = name.split(" - ").map(String::trim).filter(String::isNotEmpty)
        val mixed = byDash.size >= 2 && byDash.any(::hasArabic) && byDash.any { !hasArabic(it) }
        return if (mixed) byDash else byBar
    }

    private fun hasArabic(text: String): Boolean = text.any { it in '؀'..'ۿ' }
}
