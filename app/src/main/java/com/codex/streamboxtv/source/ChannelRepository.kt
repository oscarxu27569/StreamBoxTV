package com.codex.streamboxtv.source

import android.content.Context
import android.net.Uri
import com.codex.streamboxtv.data.Channel
import com.codex.streamboxtv.data.SourceSubscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

class ChannelRepository(
    private val context: Context? = null,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build(),
    private val parser: M3uParser = M3uParser(),
) {
    suspend fun load(source: SourceSubscription = BuiltInSources.defaultSubscription): Result<List<Channel>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val body = when (Uri.parse(source.url).scheme) {
                    "content" -> readContentUri(source.url)
                    "asset" -> readAsset(source.url)
                    else -> downloadSubscription(source)
                }
                parser.parse(body, source)
            }
        }
    }

    suspend fun loadAll(sources: List<SourceSubscription>): Result<List<Channel>> {
        return withContext(Dispatchers.IO) {
            val channels = testChannels().toMutableList()
            val errors = mutableListOf<String>()
            val sourceResults = sources
                .map { source ->
                    async {
                        val result = withTimeoutOrNull(SOURCE_LOAD_TIMEOUT_MS) {
                            load(source)
                        }
                        source to result
                    }
                }
                .awaitAll()

            sourceResults.forEach { (source, result) ->
                when (result) {
                    null -> errors += "${source.name}: 加载超时"
                    else -> result.fold(
                        onSuccess = { channels += it },
                        onFailure = { errors += "${source.name}: ${it.message.orEmpty()}" },
                    )
                }
            }

            when {
                channels.isNotEmpty() -> Result.success(mergeAlternateStreams(channels))
                errors.isNotEmpty() -> Result.failure(
                    IllegalStateException("所有订阅源都无法访问，请检查网络，或导入本地 M3U 文件。")
                )
                else -> Result.success(emptyList())
            }
        }
    }

    private fun testChannels(): List<Channel> {
        return listOf(
            Channel(
                id = "test-mux-hls",
                name = "播放器测试 Mux HLS",
                group = "测试",
                logoUrl = null,
                tvgId = null,
                epgUrl = null,
                catchupMode = null,
                catchupSource = null,
                streamUrls = listOf("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
                sourceNames = listOf("内置测试源"),
            ),
            Channel(
                id = "test-apple-hls",
                name = "播放器测试 Apple HLS",
                group = "测试",
                logoUrl = null,
                tvgId = null,
                epgUrl = null,
                catchupMode = null,
                catchupSource = null,
                streamUrls = listOf("https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_ts/master.m3u8"),
                sourceNames = listOf("内置测试源"),
            ),
        )
    }

    private fun mergeAlternateStreams(channels: List<Channel>): List<Channel> {
        return channels
            .groupBy { it.name.normalizedKey() }
            .map { (key, variants) ->
                val first = variants.first()
                first.copy(
                    id = stableId(key),
                    logoUrl = variants.firstNotNullOfOrNull { it.logoUrl },
                    tvgId = variants.firstNotNullOfOrNull { it.tvgId },
                    epgUrl = variants.firstNotNullOfOrNull { it.epgUrl },
                    catchupMode = variants.firstNotNullOfOrNull { it.catchupMode },
                    catchupSource = variants.firstNotNullOfOrNull { it.catchupSource },
                    streamUrls = variants.flatMap { it.streamUrls }.distinct(),
                    sourceNames = variants.flatMap { it.sourceNames }.distinct(),
                )
            }
            .sortedWith(compareBy<Channel> { it.group }.thenBy { it.name })
    }

    private fun String.normalizedKey(): String {
        return trim()
            .lowercase(Locale.US)
            .replace(Regex("""\s+"""), " ")
    }

    private fun stableId(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.take(12).joinToString("") { "%02x".format(it) }
    }

    private fun downloadSubscription(source: SourceSubscription): String {
        val request = Request.Builder()
            .url(source.url)
            .header("User-Agent", "StreamBoxTV/0.1")
            .build()
        val response = client.newCall(request).execute()
        response.use {
            if (!it.isSuccessful) error("订阅下载失败：HTTP ${it.code}")
            return it.body?.string().orEmpty()
        }
    }

    private fun readContentUri(uri: String): String {
        val resolver = context?.contentResolver
            ?: error("当前环境无法读取本地订阅文件")
        return resolver.openInputStream(Uri.parse(uri))?.use { input ->
            input.bufferedReader().readText()
        }.orEmpty()
    }

    private fun readAsset(uri: String): String {
        val assetManager = context?.assets
            ?: error("当前环境无法读取内置订阅文件")
        val assetName = Uri.parse(uri).path
            ?.removePrefix("/")
            ?.takeIf { it.isNotBlank() }
            ?: error("内置订阅文件路径无效")

        return assetManager.open(assetName).use { input ->
            input.bufferedReader().readText()
        }
    }

    private companion object {
        const val SOURCE_LOAD_TIMEOUT_MS = 16_000L
    }
}
