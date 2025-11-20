package cc.unitmesh.devins.ui.compose.chat

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.AgentType
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.state.UIStateManager

/**
 * Desktop 标题栏（VSCode 风格）
 *
 * 布局：
 * - 左侧：Agent Type 菜单（未选中=背景色，选中=圆角突出）
 * - 中间：（保留用于将来扩展）
 * - 右侧：Sidebar Toggle + Project Explorer + Settings + Tools
 */
@Composable
fun DesktopTitleBarTabs(
    currentAgentType: AgentType,
    onAgentTypeChange: (AgentType) -> Unit,
    onConfigureRemote: () -> Unit = {},
    onDoubleClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 当切换到 Code Review 时自动隐藏 SessionSidebar
    LaunchedEffect(currentAgentType) {
        if (currentAgentType == AgentType.CODE_REVIEW) {
            UIStateManager.setSessionSidebarVisible(false)
        } else {
            UIStateManager.setSessionSidebarVisible(true)
        }
    }
    // 从全局状态获取
    val workspacePath by UIStateManager.workspacePath.collectAsState()
    val isTreeViewVisible by UIStateManager.isTreeViewVisible.collectAsState()
    val isSessionSidebarVisible by UIStateManager.isSessionSidebarVisible.collectAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleClick() }
                )
            }
    ) {
        // Center: Workspace Indicator
        if (workspacePath.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .height(28.dp)
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = workspacePath.substringAfterLast('/').ifEmpty { workspacePath },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Left and Right content
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Sidebar Toggle + Agent Tabs
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sidebar Toggle (Left side)
                IconButton(
                    onClick = { UIStateManager.toggleSessionSidebar() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isSessionSidebarVisible) AutoDevComposeIcons.MenuOpen else AutoDevComposeIcons.Menu,
                        contentDescription = if (isSessionSidebarVisible) "Collapse Sidebar" else "Expand Sidebar",
                        tint = if (isSessionSidebarVisible) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Divider
                Surface(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                ) {}

                (AgentType.entries - AgentType.LOCAL_CHAT)
                    .forEach { type ->
                        AgentTypeMenuItem(
                            type = type,
                            isSelected = type == currentAgentType,
                            onClick = { onAgentTypeChange(type) }
                        )
                    }
            }

            // Right: Settings + Explorer (Text Buttons)
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentAgentType == AgentType.REMOTE) {
                    TextButton(
                        onClick = onConfigureRemote,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "Remote",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                TextButton(
                    onClick = { UIStateManager.toggleTreeView() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = if (isTreeViewVisible) "Hide Files" else "Files",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isTreeViewVisible) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentTypeMenuItem(
    type: AgentType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp)),
        shape = RoundedCornerShape(6.dp),
        onClick = onClick,
        color = backgroundColor,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = type.getDisplayName(),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontSize = MaterialTheme.typography.labelSmall.fontSize
            )
        }
    }
}

