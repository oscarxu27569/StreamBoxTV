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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun StreamBoxApp(viewModel: ChannelViewModel) {
    val state by viewModel.state.collectAsState()

    BackHandler(enabled = state.playingChannel != null) {
        viewModel.closePlayer()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background),
    ) {
        ChannelBrowser(
            state = state,
            onRefresh = viewModel::refresh,
            onAddSource = viewModel::addSource,
            onGroupSelected = viewModel::selectGroup,
            onChannelSelected = viewModel::selectChannel,
            onChannelPlayed = viewModel::play,
        )

        state.playingChannel?.let { channel ->
            PlayerScreen(
                channel = channel,
                onClose = viewModel::closePlayer,
                onPrevious = { viewModel.playOffset(-1) },
                onNext = { viewModel.playOffset(1) },
            )
        }
    }
}

@Composable
private fun ChannelBrowser(
    state: TvUiState,
    onRefresh: () -> Unit,
    onAddSource: (String) -> Unit,
    onGroupSelected: (String) -> Unit,
    onChannelSelected: (Channel) -> Unit,
    onChannelPlayed: (Channel) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Sidebar(
            groups = state.groups,
            selectedGroup = state.selectedGroup,
            onGroupSelected = onGroupSelected,
        )
        ChannelList(
            state = state,
            onChannelSelected = onChannelSelected,
            onChannelPlayed = onChannelPlayed,
        )
        DetailPanel(
            state = state,
            onRefresh = onRefresh,
            onAddSource = onAddSource,
            onChannelPlayed = onChannelPlayed,
        )
    }
}

@Composable
private fun Sidebar(
    groups: List<String>,
    selectedGroup: String,
    onGroupSelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight(),
    ) {
        Title("StreamBox TV")
        Spacer(Modifier.height(24.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(groups) { group ->
                FocusableRow(
                    text = group,
                    selected = group == selectedGroup,
                    onClick = { onGroupSelected(group) },
                )
            }
        }
    }
}

@Composable
private fun ChannelList(
    state: TvUiState,
    onChannelSelected: (Channel) -> Unit,
    onChannelPlayed: (Channel) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(420.dp)
            .fillMaxHeight(),
    ) {
        SectionLabel("${state.selectedGroup}  ${state.visibleChannels.size}")
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(state.visibleChannels, key = { it.id }) { channel ->
                FocusableRow(
                    text = channel.name,
                    subtitle = channel.group,
                    selected = channel.id == state.selectedChannelId,
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
    onRefresh: () -> Unit,
    onAddSource: (String) -> Unit,
    onChannelPlayed: (Channel) -> Unit,
) {
    val channel = state.selectedChannel
    var showAddSource by remember { mutableStateOf(false) }
    var sourceUrl by remember { mutableStateOf("") }
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
                .height(230.dp)
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
                    fontSize = 34.sp,
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
            FocusableButton(text = "重新加载", onClick = onRefresh)
        }
        channel?.let {
            BodyText("分组：${it.group}")
            Spacer(Modifier.height(8.dp))
            BodyText("来源：${it.sourceName}")
            Spacer(Modifier.height(8.dp))
            BodyText("订阅数：${state.sourceCount}")
            Spacer(Modifier.height(22.dp))
            FocusableButton(text = "播放", onClick = { onChannelPlayed(it) })
            Spacer(Modifier.height(18.dp))
            HintText("播放中：左右键切台，返回键退出")
        }
        Spacer(Modifier.height(18.dp))
        FocusableButton(text = "添加订阅", onClick = { showAddSource = !showAddSource })
        if (showAddSource) {
            Spacer(Modifier.height(14.dp))
            SourceInput(
                value = sourceUrl,
                onValueChanged = { sourceUrl = it },
                onSubmit = {
                    onAddSource(sourceUrl)
                    sourceUrl = ""
                    showAddSource = false
                },
            )
        }
    }
}

@Composable
private fun SourceInput(
    value: String,
    onValueChanged: (String) -> Unit,
    onSubmit: () -> Unit,
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
        Spacer(Modifier.height(10.dp))
        FocusableButton(text = "保存订阅", onClick = onSubmit)
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
    onFocus: () -> Unit = {},
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        focused -> AppColors.accent
        selected -> AppColors.selected
        else -> Color.Transparent
    }
    val background = when {
        focused -> AppColors.focused
        selected -> AppColors.selected.copy(alpha = 0.22f)
        else -> AppColors.panel
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
                color = Color.White,
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
                style = TextStyle(color = AppColors.muted, fontSize = 14.sp),
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
    val warning = Color(0xFFFFC857)
}
