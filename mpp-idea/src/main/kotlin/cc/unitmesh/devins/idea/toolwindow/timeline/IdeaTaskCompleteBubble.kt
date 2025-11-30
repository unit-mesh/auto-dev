package cc.unitmesh.devins.idea.toolwindow.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.idea.renderer.JewelRenderer
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text

/**
 * Task complete bubble for displaying task completion status.
 * Similar to TaskCompletedItem in mpp-ui but using Jewel theming.
 */
@Composable
fun IdeaTaskCompleteBubble(
    item: JewelRenderer.TimelineItem.TaskCompleteItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (item.success)
                        AutoDevColors.Green.c400.copy(alpha = 0.2f)
                    else
                        AutoDevColors.Red.c400.copy(alpha = 0.2f)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (item.success) IdeaComposeIcons.CheckCircle else IdeaComposeIcons.Error,
                    contentDescription = if (item.success) "Success" else "Failed",
                    modifier = Modifier.size(16.dp),
                    tint = if (item.success) AutoDevColors.Green.c400 else AutoDevColors.Red.c400
                )
                Text(
                    text = "${item.message} (${item.iterations} iterations)",
                    style = JewelTheme.defaultTextStyle.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

