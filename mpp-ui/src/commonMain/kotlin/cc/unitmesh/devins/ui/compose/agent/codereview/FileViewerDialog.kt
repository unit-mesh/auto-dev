package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.runtime.Composable

/**
 * File viewer dialog - platform-specific implementation
 * Shows file content in a dialog
 */
@Composable
expect fun FileViewerDialog(
    filePath: String,
    onClose: () -> Unit
)

