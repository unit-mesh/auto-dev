package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific file viewer panel wrapper
 */
@Composable
expect fun FileViewerPanelWrapper(
    filePath: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
)

