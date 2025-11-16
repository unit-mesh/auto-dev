package cc.unitmesh.devins.ui.compose.chat

import androidx.compose.foundation.background
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
 * - 中间：地址栏（显示工作空间路径）
 * - 右侧：Project Explorer + 其他选项
 */
@Composable
fun DesktopTitleBarTabs(
    currentAgentType: AgentType,
    onAgentTypeChange: (AgentType) -> Unit,
    workspacePath: String = "",
    isTreeViewVisible: Boolean = false,
    onToggleTreeView: () -> Unit = {},
    onShowModelConfig: () -> Unit = {},
    onShowToolConfig: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
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
            modifier = Modifier.weight(0.3f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AgentType.entries.forEach { type ->
                AgentTypeMenuItem(
                    type = type,
                    isSelected = type == currentAgentType,
                    onClick = { onAgentTypeChange(type) }
                )
            }
        }

//        Surface(
//            modifier = Modifier
//                .weight(0.3f)
//                .height(28.dp)
//                .padding(horizontal = 8.dp),
//            shape = RoundedCornerShape(6.dp),
//            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
//        ) {
//            Row(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(horizontal = 8.dp),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(6.dp)
//            ) {
//                Icon(
//                    imageVector = AutoDevComposeIcons.Folder,
//                    contentDescription = null,
//                    modifier = Modifier.size(14.dp),
//                    tint = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//                Text(
//                    text = workspacePath.ifEmpty { "No workspace" },
//                    style = MaterialTheme.typography.labelSmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis,
//                    modifier = Modifier.weight(1f)
//                )
//            }
//        }

        Row(
            modifier = Modifier.weight(0.3f, fill = false),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onShowModelConfig,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = AutoDevComposeIcons.Settings,
                    contentDescription = "Model Configuration",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onShowToolConfig,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = AutoDevComposeIcons.Build,
                    contentDescription = "Tool Configuration",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            // More Options
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = AutoDevComposeIcons.MoreVert,
                    contentDescription = "More Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
            Icon(
                imageVector = when (type) {
                    AgentType.REMOTE -> AutoDevComposeIcons.Cloud
                    AgentType.CODE_REVIEW -> AutoDevComposeIcons.RateReview
                    AgentType.CODING -> AutoDevComposeIcons.Code
                    AgentType.LOCAL -> AutoDevComposeIcons.Chat
                },
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = type.getDisplayName(),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontSize = MaterialTheme.typography.labelSmall.fontSize
            )
        }
    }
}

