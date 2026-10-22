package com.iptvcinema.tv.core.tvhome.launcher

import com.iptvcinema.tv.core.tvhome.HomeChannelSpec
import com.iptvcinema.tv.core.tvhome.HomeProgramEntry
import com.iptvcinema.tv.core.tvhome.HomeProgramKind
import org.json.JSONArray
import org.json.JSONObject

/** The rows snapshot the app writes for the launcher: rows plus the time they were built. */
data class LauncherRowsSnapshot(
    val generatedAtMs: Long,
    val rows: List<HomeChannelSpec>,
)

/** JSON form of [LauncherRowsSnapshot]; org.json so no serialization plugin is needed anywhere. */
object LauncherRowsCodec {
    fun encode(snapshot: LauncherRowsSnapshot): String {
        val rows = JSONArray()
        snapshot.rows.forEach { row ->
            val cards = JSONArray()
            row.programs.forEach { cards.put(it.toJson()) }
            rows.put(
                JSONObject()
                    .put("key", row.key)
                    .put("title", row.displayName)
                    .put("cards", cards),
            )
        }
        return JSONObject()
            .put("version", VERSION)
            .put("generatedAt", snapshot.generatedAtMs)
            .put("rows", rows)
            .toString()
    }

    /** Null for anything that is not a snapshot this version understands. */
    fun decode(json: String): LauncherRowsSnapshot? = runCatching {
        val root = JSONObject(json)
        if (root.optInt("version") != VERSION) return null
        val rows = root.getJSONArray("rows")
        LauncherRowsSnapshot(
            generatedAtMs = root.getLong("generatedAt"),
            rows = (0 until rows.length()).map { index ->
                val row = rows.getJSONObject(index)
                val cards = row.getJSONArray("cards")
                HomeChannelSpec(
                    key = row.getString("key"),
                    displayName = row.getString("title"),
                    programs = (0 until cards.length()).map { cards.getJSONObject(it).toEntry() },
                )
            },
        )
    }.getOrNull()

    private fun HomeProgramEntry.toJson(): JSONObject = JSONObject()
        .put("key", key)
        .put("kind", kind.wireName())
        .put("title", title)
        .putOrSkip("subtitle", description)
        .putOrSkip("imageUrl", posterUrl)
        .put("wide", isWidePoster)
        .putOrSkip("progress", progress?.toDouble())
        .putOrSkip("validUntil", validUntilMs)
        .put("intentUri", intentUri)

    private fun JSONObject.toEntry() = HomeProgramEntry(
        key = getString("key"),
        kind = kindOf(getString("kind")),
        title = getString("title"),
        description = optStringOrNull("subtitle"),
        posterUrl = optStringOrNull("imageUrl"),
        isWidePoster = optBoolean("wide", false),
        intentUri = getString("intentUri"),
        progress = if (has("progress")) getDouble("progress").toFloat() else null,
        validUntilMs = if (has("validUntil")) getLong("validUntil") else null,
    )

    private fun JSONObject.putOrSkip(name: String, value: Any?): JSONObject =
        if (value == null) this else put(name, value)

    private fun JSONObject.optStringOrNull(name: String): String? =
        if (has(name) && !isNull(name)) getString(name) else null

    fun HomeProgramKind.wireName(): String = when (this) {
        HomeProgramKind.Movie -> LauncherRowsContract.KIND_MOVIE
        HomeProgramKind.Series -> LauncherRowsContract.KIND_SERIES
        HomeProgramKind.Channel -> LauncherRowsContract.KIND_CHANNEL
    }

    private fun kindOf(name: String): HomeProgramKind = when (name) {
        LauncherRowsContract.KIND_SERIES -> HomeProgramKind.Series
        LauncherRowsContract.KIND_CHANNEL -> HomeProgramKind.Channel
        else -> HomeProgramKind.Movie
    }

    private const val VERSION = 1
}
