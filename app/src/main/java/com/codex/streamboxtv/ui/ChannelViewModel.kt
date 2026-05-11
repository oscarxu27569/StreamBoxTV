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
                onSuccess = { channels ->
                    val groups = listOf("全部") + channels.map { it.group }.distinct()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            channels = channels,
                            groups = groups,
                            selectedGroup = groups.firstOrNull().orEmpty(),
                            selectedChannelId = channels.firstOrNull()?.id,
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

    fun selectGroup(group: String) {
        _state.update { current ->
            val first = current.channelsFor(group).firstOrNull()
            current.copy(
                selectedGroup = group,
                selectedChannelId = first?.id ?: current.selectedChannelId,
            )
        }
    }

    fun selectChannel(channel: Channel) {
        _state.update { it.copy(selectedChannelId = channel.id) }
    }

    fun play(channel: Channel) {
        _state.update {
            it.copy(selectedChannelId = channel.id, playingChannel = channel)
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
                playingChannel = channels[nextIndex],
            )
        }
    }

    private fun Int.floorMod(mod: Int): Int = ((this % mod) + mod) % mod
}

data class TvUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val channels: List<Channel> = emptyList(),
    val groups: List<String> = listOf("全部"),
    val selectedGroup: String = "全部",
    val selectedChannelId: String? = null,
    val playingChannel: Channel? = null,
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
