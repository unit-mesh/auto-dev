package cc.unitmesh.devins.ui.compose.terminal

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
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
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Terminal",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Surface(
                modifier = Modifier.fillMaxSize().weight(1f),
                color = androidx.compose.ui.graphics.Color.Black,
                shape = MaterialTheme.shapes.small
            ) {
                Box(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = if (output.isNotEmpty()) output else "Terminal is not available on iOS due to platform restrictions.",
                        color = androidx.compose.ui.graphics.Color.Green,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

