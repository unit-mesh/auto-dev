package cc.unitmesh.devins.ui.compose.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.llm.ChatHistoryManager
import cc.unitmesh.devins.llm.ChatSession
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Session 侧边栏组件
 * 
 * 功能：
 * - 显示所有历史会话
 * - 图标区分：📝 本地会话、☁️ 远程会话（TODO: 未来集成）
 * - 支持切换、删除会话
 * - 显示会话的第一条消息作为标题
 * - 显示最后更新时间
 */
@Composable
fun SessionSidebar(
    chatHistoryManager: ChatHistoryManager,
    currentSessionId: String?,
    onSessionSelected: (String) -> Unit,
    onNewChat: () -> Unit,
    // 功能按钮回调
    onOpenProject: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    onShowModelConfig: () -> Unit = {},
    onShowToolConfig: () -> Unit = {},
    onShowDebug: () -> Unit = {},
    hasDebugInfo: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    // 获取所有会话
    val sessions by remember {
        derivedStateOf {
            chatHistoryManager.getAllSessions()
        }
    }
    
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chat History",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // New Chat Button
                IconButton(
                    onClick = onNewChat,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Add,
                        contentDescription = "New Chat",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            HorizontalDivider()
            
            // Action Buttons（功能按钮）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Open Project
                IconButton(
                    onClick = onOpenProject,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Folder,
                        contentDescription = "Open Project",
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // Model Config
                IconButton(
                    onClick = onShowModelConfig,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Settings,
                        contentDescription = "Model Config",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Tool Config
                IconButton(
                    onClick = onShowToolConfig,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Build,
                        contentDescription = "Tool Config",
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // Clear History
                IconButton(
                    onClick = onClearHistory,
                    modifier = Modifier.size(28.dp),
                    enabled = sessions.isNotEmpty()
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Delete,
                        contentDescription = "Clear History",
                        modifier = Modifier.size(16.dp),
                        tint = if (sessions.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
                
                // Debug Info
                if (hasDebugInfo) {
                    IconButton(
                        onClick = onShowDebug,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.BugReport,
                            contentDescription = "Debug Info",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            
            HorizontalDivider()
            
            // Session List
            if (sessions.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "No chat history",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Start a new conversation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        SessionItem(
                            session = session,
                            isSelected = session.id == currentSessionId,
                            onSelect = { onSessionSelected(session.id) },
                            onDelete = {
                                scope.launch {
                                    chatHistoryManager.deleteSession(session.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionItem(
    session: ChatSession,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    // 获取会话标题（第一条用户消息的摘要）
    val title = remember(session) {
        val firstUserMessage = session.messages.firstOrNull { it.role == MessageRole.USER }
        firstUserMessage?.content?.take(50) ?: "New Chat"
    }
    
    // 格式化时间
    val timeText = remember(session.updatedAt) {
        formatTimestamp(session.updatedAt)
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect),
        color = backgroundColor,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Local session icon
                    Text(
                        text = "📝",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${session.messages.size} messages",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.5f)
                    )
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Delete button
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = AutoDevComposeIcons.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(16.dp),
                    tint = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Chat?") },
            text = { Text("This will permanently delete this chat session and all its messages.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * 格式化时间戳为人类可读格式
 */
private fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    
    val now = kotlinx.datetime.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    
    // Calculate yesterday's date
    val yesterdayDate = now.date.let {
        kotlinx.datetime.LocalDate(it.year, it.monthNumber, it.dayOfMonth - 1)
    }
    
    return when {
        dateTime.date == now.date -> "Today"
        dateTime.date == yesterdayDate -> "Yesterday"
        dateTime.date.year == now.date.year -> "${dateTime.monthNumber}/${dateTime.dayOfMonth}"
        else -> "${dateTime.year}/${dateTime.monthNumber}/${dateTime.dayOfMonth}"
    }
}

