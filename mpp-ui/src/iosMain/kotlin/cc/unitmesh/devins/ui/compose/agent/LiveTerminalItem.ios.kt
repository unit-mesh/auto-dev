package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * iOS implementation of LiveTerminalItem
 * Terminal functionality is limited on iOS
 */
@Composable
actual fun LiveTerminalItem(
    sessionId: String,
    command: String,
    workingDirectory: String?,
    ptyHandle: Any?,
    exitCode: Int?,
    executionTimeMs: Long?,
    output: String?
) {
    Column(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
        Text("Terminal functionality is not available on iOS")
        Text("Command: $command")
        if (workingDirectory != null) {
            Text("Working directory: $workingDirectory")
        }
    }
}

