package com.iptvcinema.launcher.content

import org.json.JSONArray
import org.json.JSONObject

/** JSON form of the launcher's own copy of the rows, drawn before the provider is asked. */
object ContentRowsCodec {
    fun encode(snapshot: ContentSnapshot): String {
        val rows = JSONArray()
        snapshot.rows.forEach { row ->
            val cards = JSONArray()
            row.cards.forEach { card ->
                cards.put(
                    JSONObject()
                        .put("key", card.key)
                        .put("kind", card.kind.name)
                        .put("title", card.title)
                        .putOrSkip("subtitle", card.subtitle)
                        .putOrSkip("imageUrl", card.imageUrl)
                        .put("wide", card.isWide)
                        .put("intentUri", card.intentUri)
                        .putOrSkip("progress", card.progress?.toDouble())
                        .putOrSkip("validUntil", card.validUntilMs),
                )
            }
            rows.put(JSONObject().put("key", row.key).put("cards", cards))
        }
        return JSONObject().put("version", VERSION).put("generatedAt", snapshot.generatedAtMs).put("rows", rows).toString()
    }

    fun decode(json: String): ContentSnapshot? = runCatching {
        val root = JSONObject(json)
        if (root.optInt("version") != VERSION) return null
        val rows = root.getJSONArray("rows")
        ContentSnapshot(
            generatedAtMs = root.getLong("generatedAt"),
            rows = (0 until rows.length()).map { index ->
                val row = rows.getJSONObject(index)
                val cards = row.getJSONArray("cards")
                ContentRow(
                    key = row.getString("key"),
                    cards = (0 until cards.length()).map { cardIndex ->
                        val card = cards.getJSONObject(cardIndex)
                        ContentCard(
                            key = card.getString("key"),
                            kind = runCatching { ContentKind.valueOf(card.getString("kind")) }.getOrDefault(ContentKind.Movie),
                            title = card.getString("title"),
                            subtitle = card.optStringOrNull("subtitle"),
                            imageUrl = card.optStringOrNull("imageUrl"),
                            isWide = card.optBoolean("wide", false),
                            intentUri = card.getString("intentUri"),
                            progress = if (card.has("progress")) card.getDouble("progress").toFloat() else null,
                            validUntilMs = if (card.has("validUntil")) card.getLong("validUntil") else null,
                        )
                    },
                )
            },
        )
    }.getOrNull()

    private fun JSONObject.putOrSkip(name: String, value: Any?): JSONObject = if (value == null) this else put(name, value)

    private fun JSONObject.optStringOrNull(name: String): String? = if (has(name) && !isNull(name)) getString(name) else null

    private const val VERSION = 1
}
