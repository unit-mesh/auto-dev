package cc.unitmesh.devins.ui.compose.chat

import androidx.compose.animation.core.animateDpAsState
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
import cc.unitmesh.devins.ui.session.SessionClient
import cc.unitmesh.session.Session
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Session 侧边栏组件
 *
 * 功能：
 * - 显示所有历史会话（本地 + 远程）
 * - 支持折叠/展开
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
    sessionClient: SessionClient? = null,
    onRemoteSessionSelected: ((Session) -> Unit)? = null,
    modifier: Modifier = Modifier,
    onRenameSession: ((String, String) -> Unit)? = null,
    isExpanded: Boolean = true
) {
    val scope = rememberCoroutineScope()

    // 监听 ChatHistoryManager 的更新
    val updateTrigger by chatHistoryManager.sessionsUpdateTrigger.collectAsState()

    // 获取本地会话 - 响应 updateTrigger 变化
    val localSessions = remember(updateTrigger) {
        chatHistoryManager.getAllSessions()
    }

    // 获取远程会话
    var remoteSessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var isLoadingRemote by remember { mutableStateOf(false) }

    // 加载远程会话
    LaunchedEffect(sessionClient, updateTrigger) {
        if (sessionClient != null && sessionClient.authToken != null) {
            isLoadingRemote = true
            try {
                remoteSessions = sessionClient.getSessions()
            } catch (e: Exception) {
                println("⚠️ 加载远程会话失败: ${e.message}")
            } finally {
                isLoadingRemote = false
            }
        }
    }

    val width by animateDpAsState(if (isExpanded) 240.dp else 50.dp)

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(width),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 1.dp
    ) {
        if (isExpanded) {
            ExpandedSessionSidebarContent(
                localSessions = localSessions,
                remoteSessions = remoteSessions,
                isLoadingRemote = isLoadingRemote,
                currentSessionId = currentSessionId,
                onSessionSelected = onSessionSelected,
                onNewChat = onNewChat,
                onRemoteSessionSelected = onRemoteSessionSelected,
                onRenameSession = onRenameSession,
                chatHistoryManager = chatHistoryManager,
                sessionClient = sessionClient,
                scope = scope
            )
        } else {
            CollapsedSessionSidebarContent(onNewChat = onNewChat)
        }
    }
}

@Composable
private fun CollapsedSessionSidebarContent(onNewChat: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Actions
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onNewChat) {
                Icon(
                    imageVector = AutoDevComposeIcons.Add,
                    contentDescription = "New Chat",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ExpandedSessionSidebarContent(
    localSessions: List<ChatSession>,
    remoteSessions: List<Session>,
    isLoadingRemote: Boolean,
    currentSessionId: String?,
    onSessionSelected: (String) -> Unit,
    onNewChat: () -> Unit,
    onRemoteSessionSelected: ((Session) -> Unit)?,
    onRenameSession: ((String, String) -> Unit)?,
    chatHistoryManager: ChatHistoryManager,
    sessionClient: SessionClient?,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        HorizontalDivider()

        // Session List
        val hasAnySessions = localSessions.isNotEmpty() || remoteSessions.isNotEmpty()

        if (!hasAnySessions && !isLoadingRemote) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "No history",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Start a conversation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // 本地会话
                if (localSessions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Local",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    items(localSessions, key = { "local_${it.id}" }) { session ->
                        LocalSessionItem(
                            session = session,
                            isSelected = session.id == currentSessionId,
                            onSelect = { onSessionSelected(session.id) },
                            onRename = { newTitle ->
                                onRenameSession?.invoke(session.id, newTitle)
                            },
                            onDelete = {
                                scope.launch {
                                    chatHistoryManager.deleteSession(session.id)
                                }
                            }
                        )
                    }
                }

                // 远程会话
                if (remoteSessions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Remote",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    items(remoteSessions, key = { "remote_${it.id}" }) { session ->
                        RemoteSessionItem(
                            session = session,
                            onSelect = {
                                onRemoteSessionSelected?.invoke(session)
                            },
                            onDelete = {
                                scope.launch {
                                    try {
                                        sessionClient?.deleteSession(session.id)
                                        // Note: This local update might be overwritten by the next fetch
                                    } catch (e: Exception) {
                                        println("⚠️ 删除远程会话失败: ${e.message}")
                                    }
                                }
                            }
                        )
                    }
                }

                // Loading indicator
                if (isLoadingRemote) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // New Agent Button
        Button(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = AutoDevComposeIcons.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "New Agent",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun LocalSessionItem(
    session: ChatSession,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }

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
        session.title ?: run {
            val firstUserMessage = session.messages.firstOrNull { it.role == MessageRole.USER }
            firstUserMessage?.content?.take(50) ?: "New Chat"
        }
    }

    // 格式化时间
    val timeText = remember(session.updatedAt) {
        formatTimestamp(session.updatedAt)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onSelect),
        color = backgroundColor,
        tonalElevation = if (isSelected) 3.dp else 0.dp,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Local session indicator (no emoji)
                    Surface(
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.size(4.dp, 16.dp)
                    ) {}

                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 10.dp)
                ) {
                    Text(
                        text = "${session.messages.size} msgs",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.4f)
                    )
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.6f)
                    )
                }
            }

            // Menu button
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.MoreVert,
                        contentDescription = "More options",
                        modifier = Modifier.size(16.dp),
                        tint = contentColor.copy(alpha = 0.6f)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            showMenu = false
                            showRenameDialog = true
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = AutoDevComposeIcons.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Delete",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = AutoDevComposeIcons.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }

    // Rename dialog
    if (showRenameDialog) {
        var newTitle by remember { mutableStateOf(title) }

        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Session") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onRename(newTitle)
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RemoteSessionItem(
    session: Session,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val backgroundColor = MaterialTheme.colorScheme.surface
    val contentColor = MaterialTheme.colorScheme.onSurface

    // 获取会话标题（任务描述的摘要）
    val title = remember(session) {
        session.task.take(50).ifEmpty { "Remote Session" }
    }

    // 状态颜色
    val statusColor = when (session.status) {
        cc.unitmesh.session.SessionStatus.RUNNING -> MaterialTheme.colorScheme.primary
        cc.unitmesh.session.SessionStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
        cc.unitmesh.session.SessionStatus.FAILED -> MaterialTheme.colorScheme.error
        cc.unitmesh.session.SessionStatus.CANCELLED -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.secondary
    }

    // 格式化时间
    val timeText = remember(session.updatedAt) {
        formatTimestamp(session.updatedAt)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onSelect),
        color = backgroundColor,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Remote session indicator (no emoji)
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = "R",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 10.dp)
                ) {
                    // 状态指示器
                    Surface(
                        color = statusColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = session.status.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.4f)
                    )
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.6f)
                    )
                }
            }

            // Menu button
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.MoreVert,
                        contentDescription = "More options",
                        modifier = Modifier.size(16.dp),
                        tint = contentColor.copy(alpha = 0.6f)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Delete",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = AutoDevComposeIcons.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * 格式化时间戳为人类可读格式
 */
fun formatTimestamp(timestamp: Long): String {
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

