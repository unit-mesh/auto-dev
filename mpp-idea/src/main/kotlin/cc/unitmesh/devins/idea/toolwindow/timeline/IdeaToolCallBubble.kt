package cc.unitmesh.devins.idea.toolwindow.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.renderer.JewelRenderer
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text

/**
 * Tool call bubble for displaying tool execution with status.
 * Similar to ToolItem/CombinedToolItem in mpp-ui but using Jewel theming.
 */
@Composable
fun IdeaToolCallBubble(
    item: JewelRenderer.TimelineItem.ToolCallItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f))
                .padding(8.dp)
        ) {
            Column {
                // Tool name with status icon
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusIcon = when (item.success) {
                        true -> "+"
                        false -> "x"
                        null -> "..."
                    }
                    // Use AutoDevColors design system for consistency
                    val statusColor = when (item.success) {
                        true -> AutoDevColors.Green.c400
                        false -> AutoDevColors.Red.c400
                        null -> JewelTheme.globalColors.text.info
                    }
                    Text(
                        text = statusIcon,
                        style = JewelTheme.defaultTextStyle.copy(color = statusColor)
                    )
                    Icon(
                        imageVector = IdeaComposeIcons.Build,
                        contentDescription = "Tool",
                        modifier = Modifier.size(14.dp),
                        tint = JewelTheme.globalColors.text.normal
                    )
                    Text(
                        text = item.toolName,
                        style = JewelTheme.defaultTextStyle.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Tool parameters (truncated)
                if (item.params.isNotEmpty()) {
                    Text(
                        text = item.params.take(200) + if (item.params.length > 200) "..." else "",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 12.sp,
                            color = JewelTheme.globalColors.text.info
                        )
                    )
                }

                // Tool output (if available)
                item.output?.let { output ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = output.take(300) + if (output.length > 300) "..." else "",
                        style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp)
                    )
                }
            }
        }
    }
}

