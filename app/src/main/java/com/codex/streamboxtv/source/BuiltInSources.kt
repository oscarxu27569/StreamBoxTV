package com.codex.streamboxtv.source

import com.codex.streamboxtv.data.SourceSubscription

object BuiltInSources {
    val defaultSubscriptions = listOf(
        SourceSubscription(
            name = "内置咪咕播放源",
            url = "asset:///playlist.txt",
        ),
    )

    val defaultSubscription = defaultSubscriptions.first()
}
