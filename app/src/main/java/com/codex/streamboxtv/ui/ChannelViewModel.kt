package com.codex.streamboxtv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codex.streamboxtv.data.Channel
import com.codex.streamboxtv.source.ChannelRepository
import com.codex.streamboxtv.source.SourceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChannelViewModel(
    private val sourceStore: SourceStore,
    private val repository: ChannelRepository = ChannelRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(TvUiState())
    val state: StateFlow<TvUiState> = _state

    init {
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val sources = sourceStore.list()
            val result = repository.loadAll(sources)
            result.fold(
                onSuccess = { loadedChannels ->
                    val channels = applyHiddenItems(loadedChannels)
                    val groups = listOf("全部") + channels.map { it.group }.distinct()
                    val lastChannel = sourceStore.loadLastChannelId()
                        ?.let { id -> channels.firstOrNull { it.id == id } }
                    val initialChannel = lastChannel ?: channels.firstOrNull()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            channels = channels,
                            groups = groups,
                            selectedGroup = groups.firstOrNull().orEmpty(),
                            selectedChannelId = initialChannel?.id,
                            playingChannel = initialChannel,
                            playingStreamIndex = 0,
                            playbackMessage = null,
                            sourceCount = sources.size,
                            errorMessage = if (channels.isEmpty()) "订阅里没有找到可用频道" else null,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "订阅加载失败",
                        )
                    }
                },
            )
        }
    }

    fun addSource(url: String) {
        sourceStore.add(url).fold(
            onSuccess = { refresh() },
            onFailure = { error ->
                _state.update { it.copy(errorMessage = error.message ?: "添加订阅失败") }
            },
        )
    }

    fun addImportedSource(uri: String) {
        sourceStore.addImportedFile(uri).fold(
            onSuccess = { refresh() },
            onFailure = { error ->
                _state.update { it.copy(errorMessage = error.message ?: "导入订阅文件失败") }
            },
        )
    }

    fun selectGroup(group: String) {
        _state.update { current ->
            val first = current.channelsFor(group).firstOrNull()
            current.copy(
                selectedGroup = group,
                selectedChannelId = first?.id ?: current.selectedChannelId,
                selectedSourceIndex = 0,
            )
        }
    }

    fun selectChannel(channel: Channel) {
        _state.update { it.copy(selectedChannelId = channel.id, selectedSourceIndex = 0) }
    }

    fun selectChannelOffset(delta: Int) {
        _state.update { current ->
            val channels = current.visibleChannels
            if (channels.isEmpty()) return@update current
            val activeIndex = channels.indexOfFirst { it.id == current.selectedChannelId }
                .takeIf { it >= 0 } ?: 0
            val nextIndex = (activeIndex + delta).floorMod(channels.size)
            current.copy(selectedChannelId = channels[nextIndex].id, selectedSourceIndex = 0)
        }
    }

    fun selectSourceOffset(delta: Int) {
        _state.update { current ->
            val channel = current.playingChannel ?: current.selectedChannel ?: return@update current
            if (channel.streamUrls.isEmpty()) return@update current
            val nextIndex = (current.selectedSourceIndex + delta).floorMod(channel.streamUrls.size)
            current.copy(selectedSourceIndex = nextIndex)
        }
    }

    fun playSelectedSource() {
        val channel = _state.value.playingChannel ?: _state.value.selectedChannel ?: return
        play(channel, _state.value.selectedSourceIndex)
    }

    fun selectGroupOffset(delta: Int) {
        _state.update { current ->
            if (current.groups.isEmpty()) return@update current
            val activeIndex = current.groups.indexOf(current.selectedGroup)
                .takeIf { it >= 0 } ?: 0
            val nextGroup = current.groups[(activeIndex + delta).floorMod(current.groups.size)]
            val firstChannel = current.channelsFor(nextGroup).firstOrNull()
            current.copy(
                selectedGroup = nextGroup,
                selectedChannelId = firstChannel?.id ?: current.selectedChannelId,
                selectedSourceIndex = 0,
            )
        }
    }

    fun play(channel: Channel, sourceIndex: Int = 0) {
        sourceStore.saveLastChannelId(channel.id)
        val boundedSourceIndex = sourceIndex.coerceIn(0, (channel.streamUrls.size - 1).coerceAtLeast(0))
        _state.update {
            it.copy(
                selectedChannelId = channel.id,
                selectedSourceIndex = boundedSourceIndex,
                playingChannel = channel,
                playingStreamIndex = boundedSourceIndex,
                playbackMessage = null,
            )
        }
    }

    fun closePlayer() {
        _state.update { it.copy(playingChannel = null) }
    }

    fun playOffset(delta: Int) {
        _state.update { current ->
            val channels = current.visibleChannels
            if (channels.isEmpty()) return@update current
            val active = current.playingChannel ?: channels.first()
            val activeIndex = channels.indexOfFirst { it.id == active.id }.takeIf { it >= 0 } ?: 0
            val nextIndex = (activeIndex + delta).floorMod(channels.size)
            current.copy(
                selectedChannelId = channels[nextIndex].id,
                selectedSourceIndex = 0,
                playingChannel = channels[nextIndex],
                playingStreamIndex = 0,
                playbackMessage = null,
            )
                .also { sourceStore.saveLastChannelId(channels[nextIndex].id) }
        }
    }

    fun handlePlaybackFailure(errorCodeName: String, allowFallback: Boolean) {
        _state.update { current ->
            val channel = current.playingChannel ?: return@update current
            val nextSourceIndex = current.playingStreamIndex + 1
            if (allowFallback && nextSourceIndex < channel.streamUrls.size) {
                current.copy(
                    selectedSourceIndex = nextSourceIndex,
                    playingStreamIndex = nextSourceIndex,
                    playbackMessage = "当前源不可用，正在尝试备用源 ${nextSourceIndex + 1}/${channel.streamUrls.size}",
                )
            } else {
                current.copy(
                    playbackMessage = "无法播放：${playbackErrorText(errorCodeName)}",
                )
            }
        }
    }

    fun handlePlaybackReady() {
        _state.update { it.copy(playbackMessage = null) }
    }

    fun deleteSelectedGroup(): Boolean {
        val group = _state.value.selectedGroup
        if (group == "全部") return false
        sourceStore.hideGroup(group)
        removeHiddenItemsFromState()
        return true
    }

    fun deleteSelectedChannel(): Boolean {
        val channelId = _state.value.selectedChannelId ?: return false
        sourceStore.hideChannel(channelId)
        removeHiddenItemsFromState()
        return true
    }

    private fun removeHiddenItemsFromState() {
        _state.update { current ->
            val channels = applyHiddenItems(current.channels)
            val groups = listOf("全部") + channels.map { it.group }.distinct()
            val selectedGroup = current.selectedGroup.takeIf { it in groups } ?: groups.firstOrNull().orEmpty()
            val selectedChannel = channels.firstOrNull { it.id == current.selectedChannelId }
                ?: (if (selectedGroup == "全部") channels.firstOrNull() else channels.firstOrNull { it.group == selectedGroup })
                ?: channels.firstOrNull()
            val playingChannel = current.playingChannel?.takeIf { playing -> channels.any { it.id == playing.id } }
                ?: selectedChannel

            current.copy(
                channels = channels,
                groups = groups,
                selectedGroup = selectedGroup,
                selectedChannelId = selectedChannel?.id,
                selectedSourceIndex = 0,
                playingChannel = playingChannel,
                playingStreamIndex = 0,
                playbackMessage = null,
                errorMessage = if (channels.isEmpty()) "没有可显示频道，可以添加源或导入 M3U 文件" else current.errorMessage,
            )
        }
    }

    private fun applyHiddenItems(channels: List<Channel>): List<Channel> {
        val hiddenChannelIds = sourceStore.hiddenChannelIds()
        val hiddenGroups = sourceStore.hiddenGroups()
        return channels.filter { it.id !in hiddenChannelIds && it.group !in hiddenGroups }
    }

    private fun Int.floorMod(mod: Int): Int = ((this % mod) + mod) % mod

    private fun playbackErrorText(errorCodeName: String): String {
        return when (errorCodeName) {
            "ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED" -> "当前直播源格式不支持"
            "ERROR_CODE_IO_NETWORK_CONNECTION_FAILED" -> "网络连接失败"
            "ERROR_CODE_IO_BAD_HTTP_STATUS" -> "直播源无响应"
            "BUFFER_TIMEOUT" -> "缓冲超时"
            "ENDED" -> "直播源已断开"
            else -> errorCodeName
        }
    }
}

data class TvUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val channels: List<Channel> = emptyList(),
    val groups: List<String> = listOf("全部"),
    val selectedGroup: String = "全部",
    val selectedChannelId: String? = null,
    val selectedSourceIndex: Int = 0,
    val playingChannel: Channel? = null,
    val playingStreamIndex: Int = 0,
    val playbackMessage: String? = null,
    val sourceCount: Int = 1,
) {
    val visibleChannels: List<Channel>
        get() = channelsFor(selectedGroup)

    val selectedChannel: Channel?
        get() = channels.firstOrNull { it.id == selectedChannelId }

    fun channelsFor(group: String): List<Channel> {
        return if (group == "全部") channels else channels.filter { it.group == group }
    }
}
