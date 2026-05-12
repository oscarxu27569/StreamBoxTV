package com.codex.streamboxtv.player

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.KeyEvent as AndroidKeyEvent
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.codex.streamboxtv.data.Channel
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channel: Channel,
    streamUrl: String,
    streamIndex: Int,
    sourceCount: Int,
    playbackMessage: String?,
    onMenu: () -> Unit,
    onExitRequest: () -> Unit,
    onSources: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlaybackFailure: (String, Boolean) -> Unit,
    onPlaybackReady: () -> Unit,
) {
    val context = LocalContext.current
    val rootView = LocalView.current
    var statusText by remember { mutableStateOf("正在缓冲...") }
    var overlayVisible by remember { mutableStateOf(true) }
    var overlayPulse by remember { mutableStateOf(0) }
    var hasEverReady by remember { mutableStateOf(false) }
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    LaunchedEffect(channel.id, streamUrl) {
        statusText = "正在缓冲..."
        hasEverReady = false
        overlayVisible = true
        overlayPulse += 1
    }

    LaunchedEffect(channel.id, streamUrl, playbackMessage, statusText, overlayPulse) {
        overlayVisible = true
        delay(5000)
        overlayVisible = false
    }

    LaunchedEffect(channel.id, streamUrl) {
        delay(10000)
        if (!hasEverReady && (player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_IDLE)) {
            onPlaybackFailure("BUFFER_TIMEOUT", true)
            statusText = "缓冲超时"
            overlayVisible = true
            overlayPulse += 1
        }
    }

    DisposableEffect(rootView) {
        val previousKeepScreenOn = rootView.keepScreenOn
        rootView.keepScreenOn = true
        onDispose {
            rootView.keepScreenOn = previousKeepScreenOn
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                statusText = when (playbackState) {
                    Player.STATE_BUFFERING -> "正在缓冲..."
                    Player.STATE_READY -> {
                        hasEverReady = true
                        onPlaybackReady()
                        ""
                    }
                    Player.STATE_ENDED -> {
                        onPlaybackFailure("ENDED", !hasEverReady)
                        "直播源已断开"
                    }
                    else -> statusText
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                onPlaybackFailure(error.errorCodeName, !hasEverReady)
                statusText = playbackErrorMessage(error)
                overlayVisible = true
                overlayPulse += 1
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    DisposableEffect(streamUrl) {
        player.setMediaItem(MediaItem.fromUri(streamUrl))
        player.prepare()
        player.play()

        onDispose {
            player.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                overlayVisible = true
                overlayPulse += 1
                when (event.key) {
                    Key.DirectionUp -> {
                        onPrevious()
                        true
                    }
                    Key.DirectionDown -> {
                        onNext()
                        true
                    }
                    Key.DirectionRight -> {
                        onSources()
                        true
                    }
                    Key.DirectionLeft, Key.Menu -> {
                        onMenu()
                        true
                    }
                    Key.Back -> {
                        onExitRequest()
                        true
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> true
                    else -> false
                }
            },
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = false
                    keepScreenOn = true
                    this.player = player
                    setOnKeyListener { _, keyCode, event ->
                        if (event.action != AndroidKeyEvent.ACTION_DOWN) return@setOnKeyListener false
                        overlayVisible = true
                        overlayPulse += 1
                        when (keyCode) {
                            AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                                onPrevious()
                                true
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                                onNext()
                                true
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                                onSources()
                                true
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                                onMenu()
                                true
                            }
                            AndroidKeyEvent.KEYCODE_BACK -> {
                                onExitRequest()
                                true
                            }
                            AndroidKeyEvent.KEYCODE_MENU -> {
                                onMenu()
                                true
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                            AndroidKeyEvent.KEYCODE_ENTER -> true
                            else -> false
                        }
                    }
                    requestFocus()
                }
            },
            update = {
                it.keepScreenOn = true
                it.player = player
            },
            modifier = Modifier.fillMaxSize(),
        )
        val visibleStatus = playbackMessage ?: statusText
        if (overlayVisible) {
            NowPlayingOverlay(channel = channel, streamIndex = streamIndex, sourceCount = sourceCount)
            if (visibleStatus.isNotBlank()) {
                PlaybackStatusOverlay(statusText = visibleStatus)
            }
            PlayerHintOverlay()
        }
    }
}

@Composable
private fun NowPlayingOverlay(
    channel: Channel,
    streamIndex: Int,
    sourceCount: Int,
) {
    Column(
        modifier = Modifier
            .padding(34.dp),
    ) {
        BasicText(
            text = channel.name,
            style = TextStyle(
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(Modifier.height(6.dp))
        BasicText(
            text = "${channel.group}  源 ${streamIndex + 1}/$sourceCount",
            style = TextStyle(color = Color(0xFFB4C7CC), fontSize = 16.sp),
        )
    }
}

@Composable
private fun PlaybackStatusOverlay(statusText: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        BasicText(
            text = statusText,
            style = TextStyle(
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier
                .background(Color(0xAA000000))
                .padding(horizontal = 22.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun PlayerHintOverlay() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 34.dp, vertical = 30.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        BasicText(
            text = "上/下切台    右键换源    菜单/返回频道列表",
            style = TextStyle(color = Color(0xFFB4C7CC), fontSize = 16.sp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x66000000))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

private fun playbackErrorMessage(error: PlaybackException): String {
    return when (error.errorCodeName) {
        "ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED" -> "当前源格式不支持"
        "ERROR_CODE_IO_NETWORK_CONNECTION_FAILED" -> "网络连接失败"
        "ERROR_CODE_IO_BAD_HTTP_STATUS" -> "直播源无响应"
        else -> "播放失败：${error.errorCodeName}"
    }
}
