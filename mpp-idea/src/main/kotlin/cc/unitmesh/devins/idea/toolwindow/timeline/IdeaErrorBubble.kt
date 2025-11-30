package cc.unitmesh.devins.idea.toolwindow.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text

/**
 * Error bubble for displaying error messages.
 * Uses AutoDevColors design system for consistent error styling.
 */
@Composable
fun IdeaErrorBubble(
    message: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .background(AutoDevColors.Red.c400.copy(alpha = 0.2f))
                .padding(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = IdeaComposeIcons.Error,
                    contentDescription = "Error",
                    modifier = Modifier.size(16.dp),
                    tint = AutoDevColors.Red.c400
                )
                Text(
                    text = message,
                    style = JewelTheme.defaultTextStyle.copy(
                        color = AutoDevColors.Red.c400
                    )
                )
            }
        }
    }
}

