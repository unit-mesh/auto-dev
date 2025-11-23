package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cc.unitmesh.devins.ui.compose.agent.FileViewerPanelWrapper

/**
 * Android implementation of file viewer dialog
 * Note: Keyboard shortcuts may not work on Android due to virtual keyboard
 */
@Composable
actual fun FileViewerDialog(
    filePath: String,
    onClose: () -> Unit,
    startLine: Int?,
    endLine: Int?
) {
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
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.95f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            FileViewerPanelWrapper(
                filePath = filePath,
                onClose = onClose,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
