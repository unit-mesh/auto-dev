package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * WASM implementation of FileSystemTreeView
 * Simplified version without tree view
 */
@Composable
actual fun FileSystemTreeView(
    rootPath: String,
    onFileClick: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier.fillMaxSize().padding(8.dp)) {
        Text("File tree not supported in WASM\nRoot: $rootPath")
    }
}

