package com.codex.streamboxtv

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
        handleImportedSourceUri(uri, persistable = true)
    }

    private val importSourceFallbackLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        handleImportedSourceUri(uri, persistable = false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StreamBoxApp(
                viewModel = viewModel,
                onImportSourceFile = {
                    launchImportSourceFile()
                },
                onExitApp = {
                    finish()
                },
            )
        }
    }

    private fun launchImportSourceFile() {
        val mimeTypes = arrayOf("application/x-mpegURL", "audio/x-mpegurl", "text/*", "*/*")
        try {
            importSourceLauncher.launch(mimeTypes)
        } catch (_: ActivityNotFoundException) {
            launchFallbackImport()
        } catch (_: IllegalStateException) {
            launchFallbackImport()
        }
    }

    private fun launchFallbackImport() {
        try {
            importSourceFallbackLauncher.launch("*/*")
        } catch (_: ActivityNotFoundException) {
            viewModel.showImportUnavailable()
        } catch (_: IllegalStateException) {
            viewModel.showImportUnavailable()
        }
    }

    private fun handleImportedSourceUri(uri: Uri?, persistable: Boolean) {
        uri ?: return
        if (persistable) {
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        viewModel.addImportedSource(uri.toString())
    }
}
