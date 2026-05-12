package com.codex.streamboxtv.data

data class Channel(
    val id: String,
    val name: String,
    val group: String,
    val logoUrl: String?,
    val streamUrls: List<String>,
    val sourceNames: List<String>,
) {
    val streamUrl: String
        get() = streamUrls.firstOrNull().orEmpty()

    val sourceName: String
        get() = sourceNames.distinct().joinToString(" / ")
}
