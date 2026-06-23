package com.iptvcinema.tv.core.epg

import com.iptvcinema.tv.core.database.entity.LocalProgramEntity
import com.iptvcinema.tv.core.database.entity.LocalChannelEntity
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

@Singleton
class XmltvParser @Inject constructor() {
    fun parse(
        sourceId: String,
        xml: String,
        channels: List<LocalChannelEntity>,
        nowMs: Long = System.currentTimeMillis(),
    ): List<LocalProgramEntity> {
        if (xml.isBlank()) return emptyList()
        val channelLookup = buildChannelLookup(channels)
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(xml.reader())

        val programs = mutableListOf<LocalProgramEntity>()
        var eventType = parser.eventType
        var currentChannelXmlId: String? = null
        var currentTitle: String? = null
        var currentDescription: String? = null
        var currentStartMs: Long? = null
        var currentEndMs: Long? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "programme" -> {
                        currentChannelXmlId = parser.getAttributeValue(null, "channel")
                        currentStartMs = parseXmltvDate(parser.getAttributeValue(null, "start"))
                        currentEndMs = parseXmltvDate(parser.getAttributeValue(null, "stop"))
                        currentTitle = null
                        currentDescription = null
                    }
                    "title" -> if (parser.depth > 1) {
                        currentTitle = parser.nextText().trim()
                    }
                    "desc" -> if (parser.depth > 1) {
                        currentDescription = parser.nextText().trim()
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name == "programme") {
                    val startMs = currentStartMs
                    val endMs = currentEndMs
                    val channelId = currentChannelXmlId?.let { channelLookup[it] }
                    if (channelId != null && startMs != null && endMs != null &&
                        isProgramInIngestWindow(startMs, endMs, nowMs)
                    ) {
                        val title = currentTitle?.takeIf { it.isNotBlank() } ?: "Unknown"
                        programs += LocalProgramEntity(
                            id = "${channelId}_${startMs}",
                            sourceId = sourceId,
                            channelId = channelId,
                            title = title,
                            description = currentDescription?.takeIf { it.isNotBlank() },
                            startEpochMs = startMs,
                            endEpochMs = endMs,
                        )
                    }
                }
            }
            eventType = parser.next()
        }
        return programs
    }

    private fun parseXmltvDate(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val cleaned = value.trim().take(14)
        return runCatching {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.US)
            LocalDateTime.parse(cleaned, formatter)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }

    companion object {
        private const val WINDOW_PAST_MS = 24L * 60 * 60 * 1000
        private const val WINDOW_FUTURE_MS = 48L * 60 * 60 * 1000

        internal fun isProgramInIngestWindow(startMs: Long, endMs: Long, nowMs: Long): Boolean {
            val windowStartMs = nowMs - WINDOW_PAST_MS
            val windowEndMs = nowMs + WINDOW_FUTURE_MS
            return endMs > windowStartMs && startMs < windowEndMs
        }

        internal fun buildChannelLookup(channels: List<LocalChannelEntity>): Map<String, String> {
            val lookup = mutableMapOf<String, String>()
            channels.forEach { channel ->
                channel.tvgId?.let { lookup[it] = channel.id }
                lookup[channel.id] = channel.id
                lookup[normalizeName(channel.name)] = channel.id
            }
            return lookup
        }

        internal fun normalizeName(value: String): String =
            value.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "")
    }
}
