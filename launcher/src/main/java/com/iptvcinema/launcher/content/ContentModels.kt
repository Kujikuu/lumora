package com.iptvcinema.launcher.content

enum class ContentKind { Movie, Series, Channel }

/** One card from Lumora Play, as the provider sends it. */
data class ContentCard(
    val key: String,
    val kind: ContentKind,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val isWide: Boolean,
    val intentUri: String,
    val progress: Float? = null,
    /** When [subtitle] (the current programme) stops being true; null means never. */
    val validUntilMs: Long? = null,
)

data class ContentRow(val key: String, val cards: List<ContentCard>)

/** Everything the app published, plus when it built it. */
data class ContentSnapshot(val generatedAtMs: Long, val rows: List<ContentRow>) {
    fun row(key: String): ContentRow? = rows.firstOrNull { it.key == key }
}

/** What the launcher knows about the app's rows right now. */
data class ContentState(
    /** False once the package manager says Lumora Play is not installed. */
    val appInstalled: Boolean = true,
    /** Last rows seen, from the provider or the launcher's own cache; null before either. */
    val snapshot: ContentSnapshot? = null,
)
