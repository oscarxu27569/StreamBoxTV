package com.codex.streamboxtv.player

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
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

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channel: Channel,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    var statusText by remember { mutableStateOf("正在缓冲...") }
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    LaunchedEffect(channel.id) {
        statusText = "正在缓冲..."
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                statusText = when (playbackState) {
                    Player.STATE_BUFFERING -> "正在缓冲..."
                    Player.STATE_READY -> ""
                    Player.STATE_ENDED -> "直播已结束或源已断开"
                    else -> statusText
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                statusText = "播放失败：${error.errorCodeName}"
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    DisposableEffect(channel.streamUrl) {
        player.setMediaItem(MediaItem.fromUri(channel.streamUrl))
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
                when (event.key) {
                    Key.DirectionLeft -> {
                        onPrevious()
                        true
                    }
                    Key.DirectionRight -> {
                        onNext()
                        true
                    }
                    Key.Back -> {
                        onClose()
                        true
                    }
                    else -> false
                }
            },
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = true
                    this.player = player
                    setOnKeyListener { _, keyCode, event ->
                        if (event.action != AndroidKeyEvent.ACTION_DOWN) return@setOnKeyListener false
                        when (keyCode) {
                            AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                                onPrevious()
                                true
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                                onNext()
                                true
                            }
                            AndroidKeyEvent.KEYCODE_BACK -> {
                                onClose()
                                true
                            }
                            else -> false
                        }
                    }
                    requestFocus()
                }
            },
            update = {
                it.player = player
            },
            modifier = Modifier.fillMaxSize(),
        )
        NowPlayingOverlay(channel = channel)
        if (statusText.isNotBlank()) {
            PlaybackStatusOverlay(statusText = statusText)
        }
    }
}

@Composable
private fun NowPlayingOverlay(channel: Channel) {
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
            text = channel.group,
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
        contentAlignment = androidx.compose.ui.Alignment.BottomCenter,
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
