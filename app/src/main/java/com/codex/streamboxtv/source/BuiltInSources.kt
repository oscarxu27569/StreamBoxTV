package com.codex.streamboxtv.source

import com.codex.streamboxtv.data.SourceSubscription

object BuiltInSources {
    val defaultSubscriptions = listOf(
        SourceSubscription(
            name = "咪咕直播订阅",
            url = "https://78962588856486165751857.iepose.cn/",
        ),
    )

    val defaultSubscription = defaultSubscriptions.first()
}
