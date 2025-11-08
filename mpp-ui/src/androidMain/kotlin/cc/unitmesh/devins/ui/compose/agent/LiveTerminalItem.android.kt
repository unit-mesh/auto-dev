package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Android implementation of LiveTerminalItem.
 * Shows a message that live terminal is not supported on this platform.
 */
@Composable
actual fun LiveTerminalItem(
    sessionId: String,
    command: String,
    workingDirectory: String?,
    ptyHandle: Any?
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "💻",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Live Terminal (Not Supported)",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$ $command",
                modifier = Modifier.padding(start = 28.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ℹ️ Live terminal output is not available on Android. The command output will appear after completion.",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
