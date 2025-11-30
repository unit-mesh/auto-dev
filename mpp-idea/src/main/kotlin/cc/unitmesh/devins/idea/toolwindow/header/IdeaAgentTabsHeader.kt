package cc.unitmesh.devins.idea.toolwindow.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.AgentType
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.theme.defaultBannerStyle

/**
 * Agent tabs header for switching between agent types.
 * Similar to AgentTopAppBar in mpp-ui but using Jewel theming.
 */
@Composable
fun IdeaAgentTabsHeader(
    currentAgentType: AgentType,
    onAgentTypeChange: (AgentType) -> Unit,
    onNewChat: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Agent Type Tabs
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Show only main agent types for cleaner UI
            listOf(AgentType.CODING, AgentType.CODE_REVIEW, AgentType.KNOWLEDGE, AgentType.REMOTE).forEach { type ->
                IdeaAgentTab(
                    type = type,
                    isSelected = type == currentAgentType,
                    onClick = { onAgentTypeChange(type) }
                )
            }
        }

        // Right: Actions
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNewChat) {
                Text("+", style = JewelTheme.defaultTextStyle.copy(fontWeight = FontWeight.Bold))
            }
            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = IdeaComposeIcons.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(16.dp),
                    tint = JewelTheme.globalColors.text.normal
                )
            }
        }
    }
}

/**
 * Individual agent tab button.
 */
@Composable
fun IdeaAgentTab(
    type: AgentType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        JewelTheme.defaultBannerStyle.information.colors.background.copy(alpha = 0.5f)
    } else {
        JewelTheme.globalColors.panelBackground
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(28.dp)
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = type.getDisplayName(),
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

