package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun FileViewerPanelWrapper(
    filePath: String,
    onClose: () -> Unit,
    modifier: Modifier
) {
    // Android placeholder - could implement with WebView or system intent
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📄 File Viewer",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "File: $filePath",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "File viewer not yet implemented for Android",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onClose) {
                Text("Close")
            }
        }
    }
}
