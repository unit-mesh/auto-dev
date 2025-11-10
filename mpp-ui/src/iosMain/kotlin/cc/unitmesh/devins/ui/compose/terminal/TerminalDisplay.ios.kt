package cc.unitmesh.devins.ui.compose.terminal

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * iOS implementation of PlatformTerminalDisplay
 * Terminal functionality is not available on iOS
 */
@Composable
actual fun PlatformTerminalDisplay(
    output: String,
    modifier: Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Terminal display not available on iOS")
        if (output.isNotEmpty()) {
            Text("Output: $output")
        }
    }
}

