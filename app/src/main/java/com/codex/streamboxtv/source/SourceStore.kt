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

        return BuiltInSources.defaultSubscriptions + custom
    }

    fun add(url: String): Result<Unit> {
        val value = url.trim()
        if (!SafeStreamUrl.isHttpOrHttps(value)) {
            return Result.failure(IllegalArgumentException("订阅地址只支持 http 或 https"))
        }

        val current = list().drop(BuiltInSources.defaultSubscriptions.size)
        if (current.any { it.url == value } || BuiltInSources.defaultSubscriptions.any { it.url == value }) {
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

    fun addImportedFile(uri: String): Result<Unit> {
        val value = uri.trim()
        if (!value.startsWith("content://")) {
            return Result.failure(IllegalArgumentException("只能导入系统文件选择器返回的文件"))
        }

        val current = list().drop(BuiltInSources.defaultSubscriptions.size)
        if (current.any { it.url == value }) return Result.success(Unit)

        val next = current + SourceSubscription(
            name = "本地订阅文件 ${current.size + 1}",
            url = value,
        )
        prefs.edit()
            .putString(KEY_CUSTOM_SOURCES, next.joinToString("\n") { "${it.name}\t${it.url}" })
            .apply()
        return Result.success(Unit)
    }

    fun loadLastChannelId(): String? {
        return prefs.getString(KEY_LAST_CHANNEL_ID, null)
    }

    fun saveLastChannelId(channelId: String) {
        prefs.edit()
            .putString(KEY_LAST_CHANNEL_ID, channelId)
            .apply()
    }

    fun hiddenChannelIds(): Set<String> {
        return prefs.getStringSet(KEY_HIDDEN_CHANNEL_IDS, emptySet()).orEmpty()
    }

    fun hiddenGroups(): Set<String> {
        return prefs.getStringSet(KEY_HIDDEN_GROUPS, emptySet()).orEmpty()
    }

    fun hideChannel(channelId: String) {
        prefs.edit()
            .putStringSet(KEY_HIDDEN_CHANNEL_IDS, hiddenChannelIds() + channelId)
            .apply()
    }

    fun hideGroup(group: String) {
        prefs.edit()
            .putStringSet(KEY_HIDDEN_GROUPS, hiddenGroups() + group)
            .apply()
    }

    private companion object {
        const val KEY_CUSTOM_SOURCES = "custom_sources"
        const val KEY_LAST_CHANNEL_ID = "last_channel_id"
        const val KEY_HIDDEN_CHANNEL_IDS = "hidden_channel_ids"
        const val KEY_HIDDEN_GROUPS = "hidden_groups"
    }
}
