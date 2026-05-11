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
import androidx.compose.runtime.remember
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
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
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
