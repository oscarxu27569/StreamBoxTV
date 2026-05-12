package com.codex.streamboxtv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.streamboxtv.data.Channel
import com.codex.streamboxtv.player.PlayerScreen

@Composable
fun StreamBoxApp(
    viewModel: ChannelViewModel,
    onImportSourceFile: () -> Unit,
    onExitApp: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var showAddSource by remember { mutableStateOf(false) }
    var sourceUrl by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showSourcePicker by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var exitConfirmed by remember { mutableStateOf(true) }
    var menuLayer by remember { mutableStateOf(MenuLayer.Groups) }
    var addSourceSelected by remember { mutableStateOf(false) }
    var addSourceActionIndex by remember { mutableStateOf(0) }
    var pendingDelete by remember { mutableStateOf<DeleteTarget?>(null) }
    var deleteConfirmed by remember { mutableStateOf(false) }
    var suppressNextDeleteOkUp by remember { mutableStateOf(false) }
    var okPressedAt by remember { mutableStateOf<Long?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(showMenu, showAddSource, state.playingChannel?.id) {
        if (!showAddSource) {
            focusRequester.requestFocus()
        }
    }

    BackHandler(enabled = showExitConfirm) {
        showExitConfirm = false
        exitConfirmed = true
    }

    BackHandler(enabled = showAddSource && !showExitConfirm) {
        showAddSource = false
    }

    BackHandler(enabled = showSourcePicker && !showAddSource && !showExitConfirm) {
        showSourcePicker = false
    }

    BackHandler(enabled = showMenu && !showAddSource && !showSourcePicker && !showExitConfirm) {
        if (menuLayer == MenuLayer.Sources) {
            menuLayer = MenuLayer.Channels
        } else if (menuLayer == MenuLayer.Channels) {
            menuLayer = MenuLayer.Groups
        } else {
            showMenu = false
        }
    }

    BackHandler(enabled = state.playingChannel != null && !showMenu && !showAddSource && !showSourcePicker && !showExitConfirm) {
        exitConfirmed = true
        showExitConfirm = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (showAddSource) {
                    return@onPreviewKeyEvent when (event.key) {
                        Key.DirectionLeft, Key.DirectionUp -> {
                            if (event.type == KeyEventType.KeyDown) {
                                addSourceActionIndex = (addSourceActionIndex + 2) % 3
                            }
                            true
                        }
                        Key.DirectionRight, Key.DirectionDown -> {
                            if (event.type == KeyEventType.KeyDown) {
                                addSourceActionIndex = (addSourceActionIndex + 1) % 3
                            }
                            true
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (event.type == KeyEventType.KeyUp) {
                                when (addSourceActionIndex) {
                                    0 -> {
                                        viewModel.addSource(sourceUrl)
                                        sourceUrl = ""
                                        showAddSource = false
                                    }
                                    1 -> {
                                        showAddSource = false
                                        onImportSourceFile()
                                    }
                                    else -> showAddSource = false
                                }
                            }
                            true
                        }
                        Key.Back, Key.Menu -> {
                            if (event.type == KeyEventType.KeyDown) showAddSource = false
                            true
                        }
                        else -> false
                    }
                }
                val isOkKey = event.key == Key.DirectionCenter ||
                    event.key == Key.Enter ||
                    event.key == Key.NumPadEnter

                if (showExitConfirm) {
                    when (event.key) {
                        Key.DirectionLeft, Key.DirectionRight -> {
                            if (event.type == KeyEventType.KeyDown) {
                                exitConfirmed = !exitConfirmed
                            }
                            true
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (event.type == KeyEventType.KeyUp) {
                                if (exitConfirmed) {
                                    onExitApp()
                                } else {
                                    showExitConfirm = false
                                    exitConfirmed = true
                                }
                            }
                            true
                        }
                        Key.Back, Key.Menu -> {
                            if (event.type == KeyEventType.KeyDown) {
                                showExitConfirm = false
                                exitConfirmed = true
                            }
                            true
                        }
                        else -> true
                    }
                } else if (showSourcePicker) {
                    when (event.key) {
                        Key.DirectionUp -> {
                            if (event.type == KeyEventType.KeyDown) viewModel.selectSourceOffset(-1)
                            true
                        }
                        Key.DirectionDown -> {
                            if (event.type == KeyEventType.KeyDown) viewModel.selectSourceOffset(1)
                            true
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (event.type == KeyEventType.KeyUp) {
                                viewModel.playSelectedSource()
                                showSourcePicker = false
                            }
                            true
                        }
                        Key.DirectionLeft, Key.Back, Key.Menu -> {
                            if (event.type == KeyEventType.KeyDown) showSourcePicker = false
                            true
                        }
                        else -> true
                    }
                } else pendingDelete?.let { target ->
                    when (event.key) {
                        Key.DirectionLeft, Key.DirectionRight -> {
                            if (event.type == KeyEventType.KeyDown) {
                                deleteConfirmed = !deleteConfirmed
                            }
                            true
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent true
                            if (suppressNextDeleteOkUp) {
                                suppressNextDeleteOkUp = false
                                return@onPreviewKeyEvent true
                            }
                            if (deleteConfirmed) {
                                when (target) {
                                    is DeleteTarget.Group -> viewModel.deleteSelectedGroup()
                                    is DeleteTarget.Channel -> viewModel.deleteSelectedChannel()
                                }
                            }
                            pendingDelete = null
                            deleteConfirmed = false
                            suppressNextDeleteOkUp = false
                            true
                        }
                        Key.Back, Key.Menu -> {
                            if (event.type == KeyEventType.KeyDown) {
                                pendingDelete = null
                                deleteConfirmed = false
                                suppressNextDeleteOkUp = false
                            }
                            true
                        }
                        else -> true
                    }
                } ?: run {
                if (isOkKey) {
                    if (event.type == KeyEventType.KeyDown) {
                        val now = System.currentTimeMillis()
                        if (okPressedAt == null) {
                            okPressedAt = now
                        } else if (showMenu || state.playingChannel == null) {
                            val heldFor = now - (okPressedAt ?: now)
                            if (heldFor >= 650) {
                                pendingDelete = when {
                                    menuLayer == MenuLayer.Groups && !addSourceSelected && state.selectedGroup != "全部" ->
                                        DeleteTarget.Group(state.selectedGroup)
                                    menuLayer == MenuLayer.Channels && state.selectedChannel != null ->
                                        DeleteTarget.Channel(state.selectedChannel!!.name)
                                    else -> null
                                }
                                okPressedAt = null
                                deleteConfirmed = false
                                suppressNextDeleteOkUp = pendingDelete != null
                                return@onPreviewKeyEvent pendingDelete != null
                            }
                        }
                        return@onPreviewKeyEvent true
                    }

                    if (event.type == KeyEventType.KeyUp) {
                        val heldFor = System.currentTimeMillis() - (okPressedAt ?: System.currentTimeMillis())
                        okPressedAt = null
                        if (showMenu || state.playingChannel == null) {
                            if (heldFor >= 650) {
                                pendingDelete = when {
                                    menuLayer == MenuLayer.Groups && !addSourceSelected && state.selectedGroup != "全部" ->
                                        DeleteTarget.Group(state.selectedGroup)
                                    menuLayer == MenuLayer.Channels && state.selectedChannel != null ->
                                        DeleteTarget.Channel(state.selectedChannel!!.name)
                                    else -> null
                                }
                                deleteConfirmed = false
                                suppressNextDeleteOkUp = false
                                return@onPreviewKeyEvent pendingDelete != null
                            }

                            if (menuLayer == MenuLayer.Groups) {
                                if (addSourceSelected) {
                                    addSourceActionIndex = 0
                                    showAddSource = true
                                } else {
                                    menuLayer = MenuLayer.Channels
                                }
                            } else if (menuLayer == MenuLayer.Channels) {
                                state.selectedChannel?.let {
                                    viewModel.play(it, 0)
                                    showMenu = false
                                    menuLayer = MenuLayer.Groups
                                    addSourceSelected = false
                                }
                            } else {
                                state.selectedChannel?.let {
                                    viewModel.play(it, state.selectedSourceIndex)
                                    showMenu = false
                                    menuLayer = MenuLayer.Groups
                                    addSourceSelected = false
                                }
                            }
                            return@onPreviewKeyEvent true
                        }
                    }
                    return@onPreviewKeyEvent true
                }

                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }

                if (showMenu || state.playingChannel == null) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            when (menuLayer) {
                                MenuLayer.Groups -> Unit
                                MenuLayer.Channels -> menuLayer = MenuLayer.Groups
                                MenuLayer.Sources -> menuLayer = MenuLayer.Channels
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            when (menuLayer) {
                                MenuLayer.Groups -> {
                                    if (addSourceSelected) {
                                        addSourceSelected = false
                                    } else {
                                        viewModel.selectGroupOffset(-1)
                                    }
                                }
                                MenuLayer.Channels -> viewModel.selectChannelOffset(-1)
                                MenuLayer.Sources -> viewModel.selectSelectedChannelSourceOffset(-1)
                            }
                            true
                        }
                        Key.DirectionRight -> {
                            when (menuLayer) {
                                MenuLayer.Groups -> {
                                    if (addSourceSelected) {
                                        addSourceActionIndex = 0
                                        showAddSource = true
                                    } else {
                                        menuLayer = MenuLayer.Channels
                                    }
                                }
                                MenuLayer.Channels -> menuLayer = MenuLayer.Sources
                                MenuLayer.Sources -> Unit
                            }
                            true
                        }
                        Key.DirectionDown -> {
                            when (menuLayer) {
                                MenuLayer.Groups -> {
                                    val isLastGroup = state.groups.indexOf(state.selectedGroup) == state.groups.lastIndex
                                    if (isLastGroup || addSourceSelected) {
                                        addSourceSelected = true
                                    } else {
                                        viewModel.selectGroupOffset(1)
                                    }
                                }
                                MenuLayer.Channels -> viewModel.selectChannelOffset(1)
                                MenuLayer.Sources -> viewModel.selectSelectedChannelSourceOffset(1)
                            }
                            true
                        }
                        Key.Menu -> {
                            pendingDelete = when {
                                menuLayer == MenuLayer.Groups && !addSourceSelected && state.selectedGroup != "全部" ->
                                    DeleteTarget.Group(state.selectedGroup)
                                (menuLayer == MenuLayer.Channels || menuLayer == MenuLayer.Sources) && state.selectedChannel != null ->
                                    DeleteTarget.Channel(state.selectedChannel!!.name)
                                else -> null
                            }
                            deleteConfirmed = false
                            suppressNextDeleteOkUp = false
                            pendingDelete != null
                        }
                        Key.Back -> {
                            if (menuLayer == MenuLayer.Sources) {
                                menuLayer = MenuLayer.Channels
                                true
                            } else if (menuLayer == MenuLayer.Channels) {
                                menuLayer = MenuLayer.Groups
                                true
                            } else if (state.playingChannel != null) {
                                showMenu = false
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                } else {
                    when (event.key) {
                        Key.DirectionUp -> {
                            viewModel.playOffset(-1)
                            true
                        }
                        Key.DirectionDown -> {
                            viewModel.playOffset(1)
                            true
                        }
                        Key.DirectionRight -> {
                            showSourcePicker = true
                            true
                        }
                        Key.DirectionLeft, Key.Menu -> {
                            showMenu = true
                            menuLayer = MenuLayer.Groups
                            addSourceSelected = false
                            true
                        }
                        Key.Back -> {
                            exitConfirmed = true
                            showExitConfirm = true
                            true
                        }
                        else -> false
                    }
                }
                }
            },
    ) {
        state.playingChannel?.let { channel ->
            val streamUrl = channel.streamUrls.getOrNull(state.playingStreamIndex).orEmpty()
            PlayerScreen(
                channel = channel,
                streamUrl = streamUrl,
                streamIndex = state.playingStreamIndex,
                sourceCount = channel.streamUrls.size,
                playbackMessage = state.playbackMessage,
                onMenu = { showMenu = true },
                onExitRequest = {
                    exitConfirmed = true
                    showExitConfirm = true
                },
                onSources = { showSourcePicker = true },
                onPrevious = { viewModel.playOffset(-1) },
                onNext = { viewModel.playOffset(1) },
                onPlaybackFailure = viewModel::handlePlaybackFailure,
                onPlaybackReady = viewModel::handlePlaybackReady,
            )
        }

        if (state.isLoading && state.playingChannel == null && !showMenu) {
            InitialLoadingScreen()
        }

        if (showMenu || (state.playingChannel == null && !state.isLoading)) {
            ChannelBrowser(
                state = state,
                menuLayer = menuLayer,
                addSourceSelected = addSourceSelected,
                onRefresh = viewModel::refresh,
                onShowAddSource = {
                    addSourceSelected = true
                    addSourceActionIndex = 0
                    showAddSource = true
                },
                onGroupSelected = {
                    addSourceSelected = false
                    viewModel.selectGroup(it)
                },
                onChannelSelected = viewModel::selectChannel,
                onChannelPlayed = {
                    viewModel.play(it, 0)
                    showMenu = false
                    menuLayer = MenuLayer.Groups
                },
            )
        }

        if (showAddSource) {
            AddSourceOverlay(
                value = sourceUrl,
                selectedActionIndex = addSourceActionIndex,
                onValueChanged = { sourceUrl = it },
                onSubmit = {
                    viewModel.addSource(sourceUrl)
                    sourceUrl = ""
                    showAddSource = false
                },
                onImportSourceFile = {
                    showAddSource = false
                    onImportSourceFile()
                },
                onClose = { showAddSource = false },
            )
        }

        if (showSourcePicker) {
            SourcePickerOverlay(
                channel = state.playingChannel ?: state.selectedChannel,
                selectedIndex = state.selectedSourceIndex,
            )
        }

        pendingDelete?.let { target ->
            DeleteConfirmOverlay(
                title = when (target) {
                    is DeleteTarget.Group -> "删除分类"
                    is DeleteTarget.Channel -> "删除频道"
                },
                message = when (target) {
                    is DeleteTarget.Group -> "确认删除「${target.name}」分类下的所有频道和源？"
                    is DeleteTarget.Channel -> "确认删除「${target.name}」频道及其所有源？"
                },
                confirmSelected = deleteConfirmed,
            )
        }

        if (showExitConfirm) {
            ExitConfirmOverlay(exitSelected = exitConfirmed)
        }
    }
}

private enum class MenuLayer {
    Groups,
    Channels,
    Sources,
}

private sealed interface DeleteTarget {
    data class Group(val name: String) : DeleteTarget
    data class Channel(val name: String) : DeleteTarget
}

@Composable
private fun InitialLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = "正在加载频道...",
            style = TextStyle(
                color = AppColors.muted,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun ChannelBrowser(
    state: TvUiState,
    menuLayer: MenuLayer,
    addSourceSelected: Boolean,
    onRefresh: () -> Unit,
    onShowAddSource: () -> Unit,
    onGroupSelected: (String) -> Unit,
    onChannelSelected: (Channel) -> Unit,
    onChannelPlayed: (Channel) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background.copy(alpha = 0.96f))
            .padding(horizontal = 40.dp, vertical = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Sidebar(
            groups = state.groups,
            selectedGroup = state.selectedGroup,
            active = menuLayer == MenuLayer.Groups,
            addSourceSelected = addSourceSelected,
            onShowAddSource = onShowAddSource,
            onGroupSelected = onGroupSelected,
        )
        ChannelList(
            state = state,
            active = menuLayer == MenuLayer.Channels,
            onChannelSelected = onChannelSelected,
            onChannelPlayed = onChannelPlayed,
        )
        DetailPanel(
            state = state,
            activeSources = menuLayer == MenuLayer.Sources,
            onRefresh = onRefresh,
            onShowAddSource = onShowAddSource,
            onChannelPlayed = onChannelPlayed,
        )
    }
}

@Composable
private fun Sidebar(
    groups: List<String>,
    selectedGroup: String,
    active: Boolean,
    addSourceSelected: Boolean,
    onShowAddSource: () -> Unit,
    onGroupSelected: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val selectedIndex = if (addSourceSelected) {
        groups.size
    } else {
        groups.indexOf(selectedGroup).coerceAtLeast(0)
    }

    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(selectedIndex)
    }

    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight(),
    ) {
        Title("StreamBox TV")
        Spacer(Modifier.height(24.dp))
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(groups) { group ->
                FocusableRow(
                    text = group,
                    selected = !addSourceSelected && group == selectedGroup,
                    active = active,
                    onClick = { onGroupSelected(group) },
                )
            }
            item {
                FocusableRow(
                    text = "添加源 / 导入文件",
                    selected = addSourceSelected,
                    active = active,
                    onClick = onShowAddSource,
                )
            }
        }
    }
}

@Composable
private fun ChannelList(
    state: TvUiState,
    active: Boolean,
    onChannelSelected: (Channel) -> Unit,
    onChannelPlayed: (Channel) -> Unit,
) {
    val listState = rememberLazyListState()
    val selectedIndex = state.visibleChannels.indexOfFirst { it.id == state.selectedChannelId }
        .coerceAtLeast(0)

    LaunchedEffect(state.selectedGroup, selectedIndex) {
        if (state.visibleChannels.isNotEmpty()) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Column(
        modifier = Modifier
            .width(420.dp)
            .fillMaxHeight(),
    ) {
        SectionLabel("${state.selectedGroup}  ${state.visibleChannels.size}")
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(state.visibleChannels, key = { it.id }) { channel ->
                FocusableRow(
                    text = channel.name,
                    subtitle = channel.group,
                    selected = channel.id == state.selectedChannelId,
                    active = active,
                    onFocus = { onChannelSelected(channel) },
                    onClick = { onChannelPlayed(channel) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.DetailPanel(
    state: TvUiState,
    activeSources: Boolean,
    onRefresh: () -> Unit,
    onShowAddSource: () -> Unit,
    onChannelPlayed: (Channel) -> Unit,
) {
    val channel = state.selectedChannel
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
            .padding(top = 2.dp),
    ) {
        SectionLabel("频道详情")
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (activeSources) 120.dp else 170.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.panel),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = channel?.name ?: "等待加载频道",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = Color.White,
                    fontSize = if (activeSources) 25.sp else 30.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.padding(28.dp),
            )
        }
        Spacer(Modifier.height(22.dp))
        if (state.isLoading) {
            BodyText("正在加载内置订阅...")
        }
        state.errorMessage?.let {
            BodyText(it, color = AppColors.warning)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                FocusableButton(text = "重新加载", onClick = onRefresh)
                FocusableButton(text = "导入文件", onClick = onShowAddSource)
            }
        }
        channel?.let {
            if (activeSources) {
                BodyText("分组：${it.group}")
                Spacer(Modifier.height(8.dp))
                BodyText("源数：${it.streamUrls.size}")
                Spacer(Modifier.height(14.dp))
                SourceList(
                    channel = it,
                    selectedIndex = state.selectedSourceIndex,
                    active = true,
                    maxVisibleRows = 5,
                )
                Spacer(Modifier.height(12.dp))
                HintText("上下选择源，OK 播放，左返回频道")
            } else {
                BodyText("分组：${it.group}")
                Spacer(Modifier.height(8.dp))
                BodyText("来源：${it.sourceName}")
                Spacer(Modifier.height(8.dp))
                BodyText("订阅数：${state.sourceCount}")
                Spacer(Modifier.height(14.dp))
                SourceList(
                    channel = it,
                    selectedIndex = state.selectedSourceIndex,
                    active = false,
                    maxVisibleRows = 2,
                )
                Spacer(Modifier.height(16.dp))
                FocusableButton(text = "播放", onClick = { onChannelPlayed(it) })
                Spacer(Modifier.height(18.dp))
                HintText("频道层按右选择源，源层上下切源，OK 播放")
            }
        }
    }
}

@Composable
private fun AddSourceOverlay(
    value: String,
    selectedActionIndex: Int,
    onValueChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onImportSourceFile: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(760.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.background)
                .border(2.dp, AppColors.accent.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(28.dp),
        ) {
            Title("添加 M3U 订阅")
            Spacer(Modifier.height(18.dp))
            SourceInput(
                value = value,
                onValueChanged = onValueChanged,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AddSourceActionButton(
                    text = "保存",
                    selected = selectedActionIndex == 0,
                    onClick = onSubmit,
                )
                AddSourceActionButton(
                    text = "导入文件",
                    selected = selectedActionIndex == 1,
                    onClick = onImportSourceFile,
                )
                AddSourceActionButton(
                    text = "取消",
                    selected = selectedActionIndex == 2,
                    onClick = onClose,
                )
            }
            Spacer(Modifier.height(12.dp))
            HintText("左右选择操作，OK 确认，返回关闭")
        }
    }
}

@Composable
private fun AddSourceActionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FocusableRow(
        text = text,
        selected = selected,
        active = true,
        modifier = Modifier.width(180.dp),
        onClick = onClick,
    )
}

@Composable
private fun SourceList(
    channel: Channel,
    selectedIndex: Int,
    active: Boolean,
    maxVisibleRows: Int = 5,
) {
    val visibleRows = channel.streamUrls.size
        .coerceAtMost(maxVisibleRows)
        .coerceAtLeast(1)
    val firstVisibleSource = when {
        channel.streamUrls.size <= visibleRows -> 0
        selectedIndex <= visibleRows / 2 -> 0
        selectedIndex >= channel.streamUrls.lastIndex - visibleRows / 2 -> channel.streamUrls.size - visibleRows
        else -> selectedIndex - visibleRows / 2
    }
    val visibleSources = channel.streamUrls
        .drop(firstVisibleSource)
        .take(visibleRows)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("播放源 ${selectedIndex + 1}/${channel.streamUrls.size}")
        visibleSources.forEachIndexed { offset, _ ->
            val index = firstVisibleSource + offset
            FocusableRow(
                text = "源 ${index + 1}",
                subtitle = channel.sourceNames.getOrNull(index) ?: channel.sourceName,
                selected = index == selectedIndex,
                active = active,
                onClick = {},
            )
        }
        if (firstVisibleSource > 0 || firstVisibleSource + visibleSources.size < channel.streamUrls.size) {
            HintText("按上下查看更多源")
        }
    }
}

@Composable
private fun SourcePickerOverlay(
    channel: Channel?,
    selectedIndex: Int,
) {
    if (channel == null) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000)),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .fillMaxHeight()
                .background(AppColors.background.copy(alpha = 0.96f))
                .padding(horizontal = 24.dp, vertical = 34.dp),
        ) {
            Title("选择播放源")
            Spacer(Modifier.height(8.dp))
            BodyText(channel.name)
            Spacer(Modifier.height(18.dp))
            SourceList(
                channel = channel,
                selectedIndex = selectedIndex,
                active = true,
                maxVisibleRows = 5,
            )
            Spacer(Modifier.height(18.dp))
            HintText("上下选择，OK 切换，左/返回关闭")
        }
    }
}

@Composable
private fun DeleteConfirmOverlay(
    title: String,
    message: String,
    confirmSelected: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(640.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.background)
                .border(2.dp, AppColors.warning.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .padding(28.dp),
        ) {
            Title(title)
            Spacer(Modifier.height(16.dp))
            BodyText(message)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                DialogChoice(text = "删除", selected = confirmSelected, danger = true)
                DialogChoice(text = "取消", selected = !confirmSelected, danger = false)
            }
            Spacer(Modifier.height(12.dp))
            HintText("左右选择，OK 确认，返回取消")
        }
    }
}

@Composable
private fun ExitConfirmOverlay(exitSelected: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(560.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.background)
                .border(2.dp, AppColors.accent.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .padding(28.dp),
        ) {
            Title("退出 StreamBox TV")
            Spacer(Modifier.height(16.dp))
            BodyText("确认退出程序？")
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                DialogChoice(text = "退出", selected = exitSelected, danger = true)
                DialogChoice(text = "取消", selected = !exitSelected, danger = false)
            }
            Spacer(Modifier.height(12.dp))
            HintText("左右选择，OK 确认，返回取消")
        }
    }
}

@Composable
private fun DialogChoice(
    text: String,
    selected: Boolean,
    danger: Boolean,
) {
    val color = if (danger) AppColors.warning else AppColors.selected
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) color.copy(alpha = 0.28f) else AppColors.panel)
            .border(2.dp, if (selected) color else Color.Transparent, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun SourceInput(
    value: String,
    onValueChanged: (String) -> Unit,
) {
    Column {
        BasicTextField(
            value = value,
            onValueChange = onValueChanged,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.panel)
                .border(2.dp, AppColors.accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp),
        )
        Spacer(Modifier.height(8.dp))
        HintText("支持 http/https 的 M3U 地址")
    }
}

@Composable
private fun FocusableRow(
    text: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    selected: Boolean = false,
    active: Boolean = true,
    onFocus: () -> Unit = {},
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        active && focused -> AppColors.accent
        active && selected -> AppColors.selected
        selected -> AppColors.inactiveSelected
        else -> Color.Transparent
    }
    val background = when {
        active && focused -> AppColors.focused
        active && selected -> AppColors.selected.copy(alpha = 0.30f)
        selected -> AppColors.inactiveSelected.copy(alpha = 0.18f)
        else -> AppColors.panel
    }
    val textColor = when {
        active && selected -> Color.White
        selected -> AppColors.inactiveText
        active -> Color.White
        else -> AppColors.muted
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(if (subtitle == null) 54.dp else 68.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = textColor,
                fontSize = 20.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
        )
        subtitle?.let {
            Spacer(Modifier.height(4.dp))
            BasicText(
                text = it,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = if (active) AppColors.muted else AppColors.inactiveText,
                    fontSize = 14.sp,
                ),
            )
        }
    }
}

@Composable
private fun FocusableButton(
    text: String,
    onClick: () -> Unit,
) {
    FocusableRow(
        text = text,
        selected = true,
        modifier = Modifier.width(180.dp),
        onClick = onClick,
    )
}

@Composable
private fun Title(text: String) {
    BasicText(
        text = text,
        style = TextStyle(
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        ),
    )
}

@Composable
private fun SectionLabel(text: String) {
    BasicText(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = AppColors.accent,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        ),
    )
}

@Composable
private fun BodyText(text: String, color: Color = Color.White) {
    BasicText(
        text = text,
        style = TextStyle(color = color, fontSize = 20.sp),
    )
}

@Composable
private fun HintText(text: String) {
    BasicText(
        text = text,
        style = TextStyle(color = AppColors.muted, fontSize = 16.sp),
    )
}

object AppColors {
    val background = Color(0xFF071014)
    val panel = Color(0xFF142126)
    val focused = Color(0xFF1F3A3B)
    val selected = Color(0xFF2ED3B7)
    val accent = Color(0xFF6EE7D8)
    val muted = Color(0xFF9CB3B9)
    val inactiveSelected = Color(0xFF45676B)
    val inactiveText = Color(0xFF749198)
    val warning = Color(0xFFFFC857)
}
