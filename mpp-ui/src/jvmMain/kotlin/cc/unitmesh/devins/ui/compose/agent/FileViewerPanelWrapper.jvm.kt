package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun FileViewerPanelWrapper(
    filePath: String,
    onClose: () -> Unit,
    modifier: Modifier
) {
    FileViewerPanel(
        filePath = filePath,
        onClose = onClose,
        modifier = modifier
    )
}
