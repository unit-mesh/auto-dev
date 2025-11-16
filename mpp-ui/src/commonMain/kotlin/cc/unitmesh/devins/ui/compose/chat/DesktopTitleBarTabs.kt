package cc.unitmesh.devins.ui.compose.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.agent.AgentType
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sidebar Toggle (左侧第一个按钮)
            IconButton(
                onClick = { UIStateManager.toggleSessionSidebar() },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isSessionSidebarVisible) AutoDevComposeIcons.MenuOpen else AutoDevComposeIcons.Menu,
                    contentDescription = if (isSessionSidebarVisible) "Hide Sidebar" else "Show Sidebar",
                    tint = if (isSessionSidebarVisible) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(16.dp)
                )
            }

            // 分隔线
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

        if (workspacePath.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .weight(0.4f)
                    .height(28.dp)
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
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
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(0.4f))
        }

        // Right: Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Remote Config (only for REMOTE agent)
            if (currentAgentType == AgentType.REMOTE) {
                IconButton(
                    onClick = onConfigureRemote,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Settings,
                        contentDescription = "Configure Remote Server",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            IconButton(
                onClick = { UIStateManager.toggleTreeView() },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isTreeViewVisible) AutoDevComposeIcons.MenuOpen else AutoDevComposeIcons.Menu,
                    contentDescription = if (isTreeViewVisible) "Hide Explorer" else "Show Explorer",
                    tint = if (isTreeViewVisible) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(16.dp)
                )
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

