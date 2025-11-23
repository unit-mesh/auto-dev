package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.runtime.Composable

/**
 * File viewer dialog - platform-specific implementation
 * Shows file content in a dialog with modern features:
 * - Keyboard shortcuts (Esc to close, Ctrl+F/Cmd+F to search, Ctrl+W/Cmd+W to close)
 * - Syntax highlighting
 * - Line number highlighting
 */
@Composable
expect fun FileViewerDialog(
    filePath: String,
    onClose: () -> Unit,
    startLine: Int? = null,
    endLine: Int? = null
)

