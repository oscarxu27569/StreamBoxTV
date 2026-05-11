package com.codex.streamboxtv.source

import android.content.Context
import com.codex.streamboxtv.data.SourceSubscription

class SourceStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("sources", Context.MODE_PRIVATE)

    fun list(): List<SourceSubscription> {
        val custom = prefs.getString(KEY_CUSTOM_SOURCES, "").orEmpty()
            .lineSequence()
            .mapNotNull { line ->
                val parts = line.split('\t', limit = 2)
                if (parts.size != 2) return@mapNotNull null
                SourceSubscription(name = parts[0], url = parts[1])
            }
            .toList()

        return listOf(BuiltInSources.defaultSubscription) + custom
    }

    fun add(url: String): Result<Unit> {
        val value = url.trim()
        if (!SafeStreamUrl.isHttpOrHttps(value)) {
            return Result.failure(IllegalArgumentException("订阅地址只支持 http 或 https"))
        }

        val current = list().drop(1)
        if (current.any { it.url == value } || BuiltInSources.defaultSubscription.url == value) {
            return Result.success(Unit)
        }

        val next = current + SourceSubscription(
            name = "自定义订阅 ${current.size + 1}",
            url = value,
        )
        prefs.edit()
            .putString(KEY_CUSTOM_SOURCES, next.joinToString("\n") { "${it.name}\t${it.url}" })
            .apply()
        return Result.success(Unit)
    }

    private companion object {
        const val KEY_CUSTOM_SOURCES = "custom_sources"
    }
}
