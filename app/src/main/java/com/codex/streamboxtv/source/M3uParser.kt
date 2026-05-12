package com.codex.streamboxtv.source

import com.codex.streamboxtv.data.Channel
import com.codex.streamboxtv.data.SourceSubscription
import java.security.MessageDigest

class M3uParser {
    fun parse(content: String, source: SourceSubscription): List<Channel> {
        val channels = mutableListOf<Channel>()
        var pendingInfo: ExtInfo? = null

        content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                when {
                    line.startsWith("#EXTINF", ignoreCase = true) -> {
                        pendingInfo = parseExtInfo(line)
                    }
                    line.startsWith("#") -> Unit
                    SafeStreamUrl.isAllowed(line) -> {
                        val info = pendingInfo
                        val name = info?.name?.takeIf { it.isNotBlank() } ?: line
                        channels += Channel(
                            id = stableId("${source.url}|$line"),
                            name = name,
                            group = info?.groupTitle?.takeIf { it.isNotBlank() } ?: "未分组",
                            logoUrl = info?.logoUrl?.takeIf { SafeStreamUrl.isAllowed(it) },
                            streamUrls = listOf(line),
                            sourceNames = listOf(source.name),
                        )
                        pendingInfo = null
                    }
                    else -> {
                        parsePlainTextChannel(line)?.let { (name, streamUrl) ->
                            channels += Channel(
                                id = stableId("${source.url}|$streamUrl"),
                                name = name,
                                group = "未分组",
                                logoUrl = null,
                                streamUrls = listOf(streamUrl),
                                sourceNames = listOf(source.name),
                            )
                            pendingInfo = null
                        }
                    }
                }
            }

        return channels.distinctBy { it.streamUrl }
    }

    private fun parseExtInfo(line: String): ExtInfo {
        val name = line.substringAfter(",", missingDelimiterValue = "")
            .trim()
        val attributes = attributeRegex.findAll(line)
            .associate { it.groupValues[1] to it.groupValues[2] }

        return ExtInfo(
            name = name.ifBlank { attributes["tvg-name"].orEmpty() },
            groupTitle = attributes["group-title"].orEmpty(),
            logoUrl = attributes["tvg-logo"],
        )
    }

    private fun parsePlainTextChannel(line: String): Pair<String, String>? {
        val urlMatch = streamUrlRegex.find(line) ?: return null
        val streamUrl = urlMatch.value.trim().trimEnd(',', '，', ';')
        if (!SafeStreamUrl.isAllowed(streamUrl)) return null

        val name = line
            .substring(0, urlMatch.range.first)
            .trim()
            .trimEnd(',', '，', '|', '$', ';')
            .trim()
            .ifBlank { streamUrl }

        return name to streamUrl
    }

    private fun stableId(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
        return bytes.take(12).joinToString("") { "%02x".format(it) }
    }

    private data class ExtInfo(
        val name: String,
        val groupTitle: String,
        val logoUrl: String?,
    )

    private companion object {
        val attributeRegex = Regex("""([A-Za-z0-9_-]+)="([^"]*)"""")
        val streamUrlRegex = Regex("""(?i)(https?://\S+|rtsp://\S+|rtmp://\S+|udp://\S+)""")
    }
}
