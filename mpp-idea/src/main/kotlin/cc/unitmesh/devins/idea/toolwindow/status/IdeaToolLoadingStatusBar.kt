package cc.unitmesh.devins.idea.toolwindow.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.toolwindow.IdeaAgentViewModel
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * Tool loading status bar for displaying MCP tools and SubAgents status.
 */
@Composable
fun IdeaToolLoadingStatusBar(
    viewModel: IdeaAgentViewModel,
    modifier: Modifier = Modifier
) {
    val mcpPreloadingMessage by viewModel.mcpPreloadingMessage.collectAsState()
    val mcpPreloadingStatus by viewModel.mcpPreloadingStatus.collectAsState()
    // Recompute when preloading status changes to make it reactive
    val toolStatus = remember(mcpPreloadingStatus) { viewModel.getToolLoadingStatus() }

    Row(
        modifier = modifier
            .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.8f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // SubAgents status
        IdeaToolStatusChip(
            label = "SubAgents",
            count = toolStatus.subAgentsEnabled,
            total = toolStatus.subAgentsTotal,
            isLoading = false,
            color = AutoDevColors.Blue.c400
        )

        // MCP Tools status
        IdeaToolStatusChip(
            label = "MCP Tools",
            count = toolStatus.mcpToolsEnabled,
            total = if (toolStatus.isLoading) -1 else toolStatus.mcpToolsTotal,
            isLoading = toolStatus.isLoading,
            color = if (!toolStatus.isLoading && toolStatus.mcpToolsEnabled > 0)
                AutoDevColors.Green.c400
            else
                JewelTheme.globalColors.text.info
        )

        Spacer(modifier = Modifier.weight(1f))

        // Status message
        if (mcpPreloadingMessage.isNotEmpty()) {
            Text(
                text = mcpPreloadingMessage,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 11.sp,
                    color = JewelTheme.globalColors.text.info
                ),
                maxLines = 1
            )
        } else if (!toolStatus.isLoading && toolStatus.mcpServersLoaded > 0) {
            Text(
                text = "✓ All tools ready",
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 11.sp,
                    color = AutoDevColors.Green.c400
                )
            )
        }
    }
}

/**
 * Individual tool status chip with count indicator.
 */
@Composable
fun IdeaToolStatusChip(
    label: String,
    count: Int,
    total: Int,
    isLoading: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Status indicator dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (isLoading) JewelTheme.globalColors.text.info.copy(alpha = 0.5f) else color,
                    shape = CircleShape
                )
        )

        val totalDisplay = if (total < 0) "..." else total.toString()
        Text(
            text = "$label ($count/$totalDisplay)",
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 11.sp,
                color = if (isLoading)
                    JewelTheme.globalColors.text.info.copy(alpha = 0.7f)
                else
                    JewelTheme.globalColors.text.info
            )
        )
    }
}

