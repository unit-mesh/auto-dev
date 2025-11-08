package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * File system tree view - platform-specific implementation
 *
 * JVM/Android: Uses Bonsai library for rich tree view
 * JS: Provides a simplified placeholder (Bonsai doesn't support JS)
 */
@Composable
expect fun FileSystemTreeView(
    rootPath: String,
    onFileClick: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier
)
