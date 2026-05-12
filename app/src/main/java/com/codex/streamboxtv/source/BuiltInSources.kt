package com.codex.streamboxtv.source

import com.codex.streamboxtv.data.SourceSubscription

object BuiltInSources {
    val defaultSubscriptions = listOf(
        SourceSubscription(
            name = "BurningC4 大陆优化订阅",
            url = "https://iptv.burningc4.com/TV-IPV4.m3u",
        ),
        SourceSubscription(
            name = "big-mouth-cn/tv 国内备用订阅",
            url = "https://gh-proxy.com/https://raw.githubusercontent.com/big-mouth-cn/tv/main/iptv-ok.m3u",
        ),
        SourceSubscription(
            name = "big-mouth-cn/tv 可用订阅",
            url = "https://raw.githubusercontent.com/big-mouth-cn/tv/main/iptv-ok.m3u",
        ),
        SourceSubscription(
            name = "BurningC4 GitHub 订阅",
            url = "https://raw.githubusercontent.com/BurningC4/Chinese-IPTV/master/TV-IPV4.m3u",
        ),
    )

    val defaultSubscription = defaultSubscriptions.first()
}
