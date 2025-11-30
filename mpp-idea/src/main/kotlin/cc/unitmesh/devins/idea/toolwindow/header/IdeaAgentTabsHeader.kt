package cc.unitmesh.devins.idea.toolwindow.header

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.AgentType
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text

/**
 * Agent tabs header for switching between agent types.
 * Modern segmented control design with smooth animations and hover effects.
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
            .height(40.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Segmented Agent Type Control
        SegmentedAgentTabs(
            currentAgentType = currentAgentType,
            onAgentTypeChange = onAgentTypeChange
        )

        // Right: Actions with tooltips
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // New Chat button with plus icon
            FancyActionButton(
                icon = IdeaComposeIcons.Add,
                contentDescription = "New Chat",
                onClick = onNewChat
            )

            // Settings button
            FancyActionButton(
                icon = IdeaComposeIcons.Settings,
                contentDescription = "Settings",
                onClick = onSettings
            )
        }
    }
}

/**
 * Segmented control container for agent tabs.
 * Provides a unified background with individual tab pills.
 */
@Composable
private fun SegmentedAgentTabs(
    currentAgentType: AgentType,
    onAgentTypeChange: (AgentType) -> Unit,
    modifier: Modifier = Modifier
) {
    val agentTypes = listOf(AgentType.CODING, AgentType.CODE_REVIEW, AgentType.KNOWLEDGE, AgentType.REMOTE)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        agentTypes.forEach { type ->
            IdeaAgentTabPill(
                type = type,
                isSelected = type == currentAgentType,
                onClick = { onAgentTypeChange(type) }
            )
        }
    }
}

/**
 * Individual agent tab pill with icon, animated selection state, and hover effect.
 */
@Composable
private fun IdeaAgentTabPill(
    type: AgentType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Animated background color
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> getAgentTypeColor(type).copy(alpha = 0.2f)
            isHovered -> JewelTheme.globalColors.panelBackground.copy(alpha = 0.8f)
            else -> Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabBackground"
    )

    // Animated text color
    val textColor by animateColorAsState(
        targetValue = when {
            isSelected -> getAgentTypeColor(type)
            isHovered -> JewelTheme.globalColors.text.normal
            else -> JewelTheme.globalColors.text.normal.copy(alpha = 0.7f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabText"
    )

    // Animated indicator height
    val indicatorHeight by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "indicator"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon for agent type
            Icon(
                imageVector = getAgentTypeIcon(type),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = textColor
            )

            Text(
                text = type.getDisplayName(),
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor
                )
            )
        }

        // Bottom indicator
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(indicatorHeight)
                .clip(RoundedCornerShape(1.dp))
                .background(getAgentTypeColor(type))
        )
    }
}

/**
 * Fancy action button with hover effect.
 */
@Composable
private fun FancyActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        targetValue = if (isHovered) {
            JewelTheme.globalColors.panelBackground.copy(alpha = 0.8f)
        } else {
            Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "actionBg"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isHovered) {
            AutoDevColors.Blue.c400
        } else {
            JewelTheme.globalColors.text.normal.copy(alpha = 0.7f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "actionIcon"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .hoverable(interactionSource)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = iconColor
        )
    }
}

/**
 * Get themed color for each agent type.
 */
@Composable
private fun getAgentTypeColor(type: AgentType): Color = when (type) {
    AgentType.CODING -> AutoDevColors.Blue.c400
    AgentType.CODE_REVIEW -> AutoDevColors.Indigo.c400
    AgentType.KNOWLEDGE -> AutoDevColors.Green.c400
    AgentType.REMOTE -> AutoDevColors.Amber.c400
    AgentType.LOCAL_CHAT -> JewelTheme.globalColors.text.normal
}

/**
 * Get icon for each agent type.
 */
private fun getAgentTypeIcon(type: AgentType): ImageVector = when (type) {
    AgentType.CODING -> IdeaComposeIcons.Code
    AgentType.CODE_REVIEW -> IdeaComposeIcons.Review
    AgentType.KNOWLEDGE -> IdeaComposeIcons.Book
    AgentType.REMOTE -> IdeaComposeIcons.Cloud
    AgentType.LOCAL_CHAT -> IdeaComposeIcons.Chat
}

