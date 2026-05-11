package com.codex.streamboxtv.source

import com.codex.streamboxtv.data.Channel
import com.codex.streamboxtv.data.SourceSubscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ChannelRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build(),
    private val parser: M3uParser = M3uParser(),
) {
    suspend fun load(source: SourceSubscription = BuiltInSources.defaultSubscription): Result<List<Channel>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(source.url)
                    .header("User-Agent", "StreamBoxTV/0.1")
                    .build()
                val response = client.newCall(request).execute()
                response.use {
                    if (!it.isSuccessful) error("订阅下载失败：HTTP ${it.code}")
                    val body = it.body?.string().orEmpty()
                    parser.parse(body, source)
                }
            }
        }
    }

    suspend fun loadAll(sources: List<SourceSubscription>): Result<List<Channel>> {
        return withContext(Dispatchers.IO) {
            val channels = mutableListOf<Channel>()
            val errors = mutableListOf<String>()
            sources.forEach { source ->
                load(source).fold(
                    onSuccess = { channels += it },
                    onFailure = { errors += "${source.name}: ${it.message.orEmpty()}" },
                )
            }

            when {
                channels.isNotEmpty() -> Result.success(channels.distinctBy { it.streamUrl })
                errors.isNotEmpty() -> Result.failure(IllegalStateException(errors.joinToString("\n")))
                else -> Result.success(emptyList())
            }
        }
    }
}
