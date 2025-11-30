package cc.unitmesh.devins.idea.toolwindow.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.idea.renderer.JewelRenderer
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.theme.defaultBannerStyle

/**
 * Message bubble for displaying user and assistant messages.
 * Uses Jewel theming aligned with IntelliJ IDEA.
 */
@Composable
fun IdeaMessageBubble(
    role: JewelRenderer.MessageRole,
    content: String,
    modifier: Modifier = Modifier
) {
    val isUser = role == JewelRenderer.MessageRole.USER
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .background(
                    if (isUser)
                        JewelTheme.defaultBannerStyle.information.colors.background.copy(alpha = 0.75f)
                    else
                        JewelTheme.globalColors.panelBackground
                )
                .padding(8.dp)
        ) {
            Text(
                text = content,
                style = JewelTheme.defaultTextStyle
            )
        }
    }
}

/**
 * Streaming message bubble with cursor indicator.
 */
@Composable
fun IdeaStreamingMessageBubble(
    content: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .background(JewelTheme.globalColors.panelBackground)
                .padding(8.dp)
        ) {
            Text(
                text = content + "|",
                style = JewelTheme.defaultTextStyle
            )
        }
    }
}

