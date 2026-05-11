package com.codex.streamboxtv.source

import com.codex.streamboxtv.data.SourceSubscription

object BuiltInSources {
    val defaultSubscriptions = listOf(
        SourceSubscription(
            name = "big-mouth-cn/tv 可用订阅",
            url = "https://raw.githubusercontent.com/big-mouth-cn/tv/main/iptv-ok.m3u",
        ),
        SourceSubscription(
            name = "big-mouth-cn/tv 国内备用订阅",
            url = "https://gh-proxy.com/https://raw.githubusercontent.com/big-mouth-cn/tv/main/iptv-ok.m3u",
        ),
    )

    val defaultSubscription = defaultSubscriptions.first()
}
