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
    workspacePath: String = "",
    isTreeViewVisible: Boolean = false,
    showSessionSidebar: Boolean = true,
    selectedAgent: String = "Default",
    onToggleSidebar: () -> Unit = {},
    onToggleTreeView: () -> Unit = {},
    onConfigureRemote: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
                onClick = onToggleTreeView,
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

