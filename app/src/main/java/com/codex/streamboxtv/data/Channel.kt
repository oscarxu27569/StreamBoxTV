package com.codex.streamboxtv.data

data class Channel(
    val id: String,
    val name: String,
    val group: String,
    val logoUrl: String?,
    val streamUrl: String,
    val sourceName: String,
)
