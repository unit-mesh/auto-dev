package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cc.unitmesh.devins.ui.compose.agent.FileViewerPanel

/**
 * JVM implementation of file viewer dialog with keyboard shortcuts:
 * - Esc: Close dialog
 * - Ctrl+W / Cmd+W: Close dialog
 * - Ctrl+F / Cmd+F: Open search (handled by RSyntaxTextArea)
 */
@Composable
actual fun FileViewerDialog(
    filePath: String,
    onClose: () -> Unit,
    startLine: Int?,
    endLine: Int?
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    when {
                        // Esc key to close
                        event.type == KeyEventType.KeyDown && event.key == Key.Escape -> {
                            onClose()
                            true
                        }
                        // Ctrl+W or Cmd+W to close
                        event.type == KeyEventType.KeyDown &&
                        event.key == Key.W &&
                        (event.isCtrlPressed || event.isMetaPressed) -> {
                            onClose()
                            true
                        }
                        else -> false
                    }
                },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            FileViewerPanel(
                filePath = filePath,
                onClose = onClose,
                startLine = startLine,
                endLine = endLine
            )
        }
    }
}

