package cc.unitmesh.devins.ui.compose.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.Platform

/**
 * 聊天界面顶部工具栏
 * 根据平台自动适配布局（Android 使用紧凑布局）
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
    val isAndroid = Platform.isAndroid

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isAndroid) 16.dp else 32.dp,
                vertical = if (isAndroid) 8.dp else 16.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isAndroid) 8.dp else 16.dp)
        ) {
            // Android: 只显示图标按钮，不显示标题文字
            // Desktop: 显示完整标题和按钮
            if (!isAndroid) {
                Text(
                    text = "AutoDev - DevIn AI",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Android: 使用图标按钮，Desktop: 使用带文字的按钮
            if (isAndroid) {
                IconButton(onClick = onOpenDirectory) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Open Project",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
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

