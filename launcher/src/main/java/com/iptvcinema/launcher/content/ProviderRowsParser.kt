package com.iptvcinema.launcher.content

/**
 * Builds a [ContentSnapshot] from provider rows given as column maps, so the cursor walking stays
 * a few lines in the repository and the grouping and ordering rules are unit tested.
 */
object ProviderRowsParser {
    fun parse(rows: List<Map<String, Any?>>): ContentSnapshot {
        val generatedAt = rows.firstOrNull()?.get(LauncherRowsContract.COLUMN_GENERATED_AT) as? Long ?: 0L
        val byRow = rows.groupBy { it[LauncherRowsContract.COLUMN_ROW_KEY] as? String ?: "" }
        val ordered = LauncherRowsContract.ROW_ORDER.map { key ->
            ContentRow(key, byRow[key].orEmpty().mapNotNull(::toCard))
        }
        return ContentSnapshot(generatedAt, ordered)
    }

    private fun toCard(values: Map<String, Any?>): ContentCard? {
        val key = values[LauncherRowsContract.COLUMN_KEY] as? String ?: return null
        val title = values[LauncherRowsContract.COLUMN_TITLE] as? String ?: return null
        val intentUri = values[LauncherRowsContract.COLUMN_INTENT_URI] as? String ?: return null
        return ContentCard(
            key = key,
            kind = when (values[LauncherRowsContract.COLUMN_KIND]) {
                LauncherRowsContract.KIND_SERIES -> ContentKind.Series
                LauncherRowsContract.KIND_CHANNEL -> ContentKind.Channel
                else -> ContentKind.Movie
            },
            title = title,
            subtitle = values[LauncherRowsContract.COLUMN_SUBTITLE] as? String,
            imageUrl = values[LauncherRowsContract.COLUMN_IMAGE_URL] as? String,
            isWide = (values[LauncherRowsContract.COLUMN_WIDE] as? Number)?.toInt() == 1,
            intentUri = intentUri,
            progress = (values[LauncherRowsContract.COLUMN_PROGRESS] as? Number)?.toFloat(),
            validUntilMs = (values[LauncherRowsContract.COLUMN_VALID_UNTIL] as? Number)?.toLong(),
        )
    }
}
