package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun FileViewerPanelWrapper(
    filePath: String,
    onClose: () -> Unit,
    modifier: Modifier
) {
    var fileContent by remember(filePath) { mutableStateOf<String?>(null) }
    var error by remember(filePath) { mutableStateOf<String?>(null) }

    LaunchedEffect(filePath) {
        try {
            val content = NSString.stringWithContentsOfFile(
                filePath,
                encoding = NSUTF8StringEncoding,
                error = null
            ) as? String
            
            if (content != null) {
                fileContent = content
                error = null
            } else {
                error = "Unable to read file"
            }
        } catch (e: Exception) {
            error = e.message
        }
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = filePath.substringAfterLast('/'),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            // Content
            Box(
                modifier = Modifier.fillMaxSize().padding(8.dp)
            ) {
                if (error != null) {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (fileContent != null) {
                    SelectionContainer {
                        Text(
                            text = fileContent!!,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        )
                    }
                } else {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}
