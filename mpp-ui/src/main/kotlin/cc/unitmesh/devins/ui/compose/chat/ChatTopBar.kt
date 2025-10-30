package cc.unitmesh.devins.ui.compose.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlusOne
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 聊天界面顶部工具栏
 */
@Composable
fun ChatTopBar(
    hasHistory: Boolean,
    hasDebugInfo: Boolean,
    onOpenDirectory: () -> Unit,
    onClearHistory: () -> Unit,
    onShowDebug: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "AutoDev - DevIn AI",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Button(
                onClick = onOpenDirectory,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Open Directory"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Directory")
            }
            
            // 清空历史按钮
            if (hasHistory) {
                IconButton(onClick = onClearHistory) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        
        // Debug 图标按钮
        if (hasDebugInfo) {
            IconButton(onClick = onShowDebug) {
                Icon(
                    imageVector = Icons.Outlined.BugReport,
                    contentDescription = "Debug Info",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

