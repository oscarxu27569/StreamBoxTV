package com.codex.streamboxtv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.codex.streamboxtv.source.SourceStore
import com.codex.streamboxtv.ui.ChannelViewModel
import com.codex.streamboxtv.ui.StreamBoxApp

class MainActivity : ComponentActivity() {
    private val viewModel: ChannelViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChannelViewModel(sourceStore = SourceStore(applicationContext)) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StreamBoxApp(viewModel = viewModel)
        }
    }
}
