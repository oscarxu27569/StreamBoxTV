package com.codex.streamboxtv.source

import java.net.URI
import java.util.Locale

object SafeStreamUrl {
    private val allowedSchemes = setOf("http", "https", "rtsp", "rtmp", "udp")
    private val blockedSchemes = setOf("content", "data", "file", "intent", "javascript")

    fun isAllowed(rawUrl: String): Boolean {
        val value = rawUrl.trim()
        if (value.isBlank() || value.length > 4096) return false
        if (value.any { it.code < 32 }) return false

        val scheme = runCatching { URI(value).scheme.orEmpty().lowercase(Locale.US) }
            .getOrDefault("")

        return scheme in allowedSchemes && scheme !in blockedSchemes
    }

    fun isHttpOrHttps(rawUrl: String): Boolean {
        val scheme = runCatching { URI(rawUrl.trim()).scheme.orEmpty().lowercase(Locale.US) }
            .getOrDefault("")
        return scheme == "http" || scheme == "https"
    }
}
