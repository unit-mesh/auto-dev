package cc.unitmesh.devins.ui.compose.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.agent.AgentType
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons

/**
 * Desktop 标题栏 Tabs（简化版）
 * 
 * 只显示 Agent Type 切换标签，直接在标题栏中使用
 * 状态提升到 Main.kt，点击事件直接触发状态变更
 */
@Composable
fun DesktopTitleBarTabs(
    currentAgentType: AgentType,
    onAgentTypeChange: (AgentType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(start = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AgentType.entries.forEach { type ->
            AgentTypeTab(
                type = type,
                isSelected = type == currentAgentType,
                onClick = { onAgentTypeChange(type) }
            )
        }
    }
}

/**
 * Agent Type Tab (类似 Chrome 标签页)
 */
@Composable
private fun AgentTypeTab(
    type: AgentType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    }
    
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier.height(32.dp),
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            topStart = 8.dp,
            topEnd = 8.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        ),
        color = backgroundColor,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = type.getDisplayName(),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}

