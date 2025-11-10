package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * iOS implementation of FileSystemTreeView
 * Uses a simplified tree view
 */
@Composable
actual fun FileSystemTreeView(
    rootPath: String,
    onFileClick: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("File system tree view - iOS implementation")
        Text("Root: $rootPath")
    }
}

