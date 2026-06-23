package com.iptvcinema.tv.core.m3u

object M3uParser {
    private val attributePattern = Regex("""([\w-]+)=("([^"]*)"|([^\s,"]+))""")
    private val urlPattern = Regex("""^https?://.+""", RegexOption.IGNORE_CASE)

    fun parse(content: String): List<M3uEntry> {
        val normalized = content.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
        val lines = normalized.lineSequence().map { it.trim() }.toList()

        val entries = mutableListOf<M3uEntry>()
        var pendingExtInf: ExtInfLine? = null
        var pendingAttributes = mutableMapOf<String, String>()

        for (line in lines) {
            if (line.isBlank()) continue

            when {
                line.startsWith("#EXTM3U", ignoreCase = true) -> continue
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    pendingExtInf = parseExtInf(line)
                    pendingAttributes = pendingExtInf.attributes.toMutableMap()
                }
                line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                    parseExtVlcOpt(line)?.let { (key, value) ->
                        pendingAttributes[key] = value
                    }
                }
                line.startsWith("#") -> {
                    parseGenericTag(line)?.let { (key, value) ->
                        pendingAttributes[key] = value
                    }
                }
                else -> {
                    if (!isValidStreamUrl(line)) continue
                    val extInf = pendingExtInf ?: continue
                    val mergedAttributes = extInf.attributes + pendingAttributes
                    entries += M3uEntry(
                        name = extInf.displayName.ifBlank { extInf.tvgName.orEmpty() }.ifBlank { "Channel ${entries.size + 1}" },
                        url = line,
                        tvgId = mergedAttributes["tvg-id"]?.takeIf { it.isNotBlank() },
                        tvgName = mergedAttributes["tvg-name"]?.takeIf { it.isNotBlank() },
                        logo = mergedAttributes["tvg-logo"]?.takeIf { it.isNotBlank() },
                        group = mergedAttributes["group-title"]?.takeIf { it.isNotBlank() },
                        attributes = mergedAttributes.filterValues { it.isNotBlank() },
                    )
                    pendingExtInf = null
                    pendingAttributes = mutableMapOf()
                }
            }
        }

        return entries
    }

    private data class ExtInfLine(
        val displayName: String,
        val tvgId: String?,
        val tvgName: String?,
        val logo: String?,
        val group: String?,
        val attributes: Map<String, String>,
    )

    private fun parseExtInf(line: String): ExtInfLine {
        val payload = line.substringAfter("#EXTINF:", missingDelimiterValue = line)
            .trimStart()
            .removePrefix("-1")
            .trimStart()

        val commaIndex = findDisplayNameCommaIndex(payload)
        val attributeSection = if (commaIndex >= 0) payload.substring(0, commaIndex).trim() else payload
        val displayName = if (commaIndex >= 0) payload.substring(commaIndex + 1).trim() else ""

        val attributes = parseAttributes(attributeSection).toMutableMap()
        return ExtInfLine(
            displayName = displayName,
            tvgId = attributes["tvg-id"],
            tvgName = attributes["tvg-name"],
            logo = attributes["tvg-logo"],
            group = attributes["group-title"],
            attributes = attributes,
        )
    }

    private fun findDisplayNameCommaIndex(payload: String): Int {
        var inQuotes = false
        for (index in payload.indices) {
            when (payload[index]) {
                '"' -> inQuotes = !inQuotes
                ',' -> if (!inQuotes) return index
            }
        }
        return -1
    }

    private fun parseAttributes(section: String): Map<String, String> =
        attributePattern.findAll(section).associate { match ->
            val key = match.groupValues[1].lowercase()
            val value = match.groupValues[3].ifEmpty { match.groupValues[4] }
            key to value
        }

    private fun parseExtVlcOpt(line: String): Pair<String, String>? {
        val payload = line.substringAfter("#EXTVLCOPT:", missingDelimiterValue = "").trim()
        val separator = payload.indexOf('=')
        if (separator <= 0) return null
        return payload.substring(0, separator).trim().lowercase() to payload.substring(separator + 1).trim()
    }

    private fun parseGenericTag(line: String): Pair<String, String>? {
        val payload = line.removePrefix("#").trim()
        val separator = payload.indexOf(':')
        if (separator <= 0) return null
        val key = payload.substring(0, separator).trim().lowercase()
        val value = payload.substring(separator + 1).trim()
        return if (value.isBlank()) null else key to value
    }

    internal fun isValidStreamUrl(url: String): Boolean = urlPattern.matches(url)
}
