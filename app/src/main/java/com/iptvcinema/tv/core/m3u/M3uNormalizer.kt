package com.iptvcinema.tv.core.m3u

import com.iptvcinema.tv.core.database.entity.LocalCategoryEntity
import com.iptvcinema.tv.core.database.entity.LocalChannelEntity
import com.iptvcinema.tv.core.model.catalog.CatalogContentType
import java.security.MessageDigest

object M3uNormalizer {
    private val adultKeywords = setOf("adult", "xxx", "18+", "porn")

    fun normalizeLiveCatalog(
        sourceId: String,
        entries: List<M3uEntry>,
    ): Pair<List<LocalCategoryEntity>, List<LocalChannelEntity>> {
        val groupOrder = linkedSetOf<String>()
        entries.forEach { entry ->
            groupOrder += entry.group?.trim().orEmpty().ifBlank { "Uncategorized" }
        }

        val categories = groupOrder.mapIndexed { index, groupName ->
            LocalCategoryEntity(
                id = categoryId(sourceId, groupName),
                sourceId = sourceId,
                name = groupName,
                contentType = CatalogContentType.LIVE.name,
                sortOrder = index,
            )
        }
        val categoryIds = categories.associate { it.name to it.id }

        val channels = entries.mapIndexedNotNull { index, entry ->
            val streamUrl = entry.url.trim()
            if (streamUrl.isBlank()) return@mapIndexedNotNull null
            val groupName = entry.group?.trim().orEmpty().ifBlank { "Uncategorized" }
            val categoryId = categoryIds[groupName] ?: categoryId(sourceId, groupName)
            val name = entry.name.trim().ifBlank { entry.tvgName.orEmpty() }.ifBlank { "Channel ${index + 1}" }
            LocalChannelEntity(
                id = channelId(entry),
                sourceId = sourceId,
                name = name,
                streamUrl = streamUrl,
                logoUrl = entry.logo?.trim()?.takeIf { it.isNotBlank() },
                categoryId = categoryId,
                categoryName = groupName,
                tvgId = entry.tvgId?.trim()?.takeIf { it.isNotBlank() },
                channelNumber = index + 1,
                isAdult = isAdultContent(name, groupName),
                sortOrder = index,
            )
        }

        return categories to channels
    }

    internal fun channelId(entry: M3uEntry): String {
        val tvgId = entry.tvgId?.trim().orEmpty()
        val seed = if (tvgId.isNotBlank()) "$tvgId|${entry.url.trim()}" else entry.url.trim()
        return stableHash(seed)
    }

    internal fun categoryId(sourceId: String, groupName: String): String =
        "m3u-cat-${stableHash("$sourceId|$groupName")}"

    private fun stableHash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.take(12).joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun isAdultContent(name: String, group: String): Boolean {
        val normalized = "${name.lowercase()} ${group.lowercase()}"
        return adultKeywords.any { keyword -> normalized.contains(keyword) }
    }
}
