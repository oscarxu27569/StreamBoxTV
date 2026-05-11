package com.codex.streamboxtv.source

import com.codex.streamboxtv.data.SourceSubscription

object BuiltInSources {
    val defaultSubscription = SourceSubscription(
        name = "big-mouth-cn/tv 可用订阅",
        url = "https://raw.githubusercontent.com/big-mouth-cn/tv/main/iptv-ok.m3u",
    )
}
