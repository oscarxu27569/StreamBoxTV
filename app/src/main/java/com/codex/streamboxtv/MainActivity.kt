package com.codex.streamboxtv

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.codex.streamboxtv.source.ChannelRepository
import com.codex.streamboxtv.source.SourceStore
import com.codex.streamboxtv.ui.ChannelViewModel
import com.codex.streamboxtv.ui.StreamBoxApp

class MainActivity : ComponentActivity() {
    private val viewModel: ChannelViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChannelViewModel(
                    sourceStore = SourceStore(applicationContext),
                    repository = ChannelRepository(applicationContext),
                ) as T
            }
        }
    }

    private val importSourceLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        viewModel.addImportedSource(uri.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StreamBoxApp(
                viewModel = viewModel,
                onImportSourceFile = {
                    importSourceLauncher.launch(arrayOf("application/x-mpegURL", "audio/x-mpegurl", "text/*", "*/*"))
                },
            )
        }
    }
}
