package cc.unitmesh.devins.idea.agent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.agent.TokenInfo
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.markdown.Markdown
import org.jetbrains.jewel.markdown.MarkdownBlock
import org.jetbrains.jewel.markdown.processing.MarkdownProcessor

/**
 * Message item component for displaying user or assistant messages.
 */
@Composable
fun MessageItem(
    content: String,
    isUser: Boolean,
    tokenInfo: TokenInfo? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(JewelTheme.globalColors.panelBackground)
                .padding(8.dp)
        ) {
            Column {
                if (isUser) {
                    Text(
                        text = content,
                        style = JewelTheme.defaultTextStyle.copy(
                            fontFamily = FontFamily.Monospace
                        )
                    )
                } else {
                    // Render markdown for assistant messages
                    val processor = remember { MarkdownProcessor() }
                    val blocks = remember(content) { processor.processMarkdownDocument(content) }
                    Markdown(blocks = blocks, modifier = Modifier.fillMaxWidth())
                }

                // Token info for assistant messages
                if (!isUser && tokenInfo != null && tokenInfo.totalTokens > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${tokenInfo.inputTokens} + ${tokenInfo.outputTokens} (${tokenInfo.totalTokens} tokens)",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = JewelTheme.globalColors.text.info
                        )
                    )
                }
            }
        }
    }
}

/**
 * Streaming message item for displaying in-progress responses.
 */
@Composable
fun StreamingMessageItem(
    content: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(JewelTheme.globalColors.panelBackground)
            .padding(12.dp)
    ) {
        Column {
            if (content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val processor = remember { MarkdownProcessor() }
                val blocks = remember(content) { processor.processMarkdownDocument(content) }
                Markdown(blocks = blocks, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * Task completed item for displaying task completion status.
 */
@Composable
fun TaskCompletedItem(
    success: Boolean,
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (success) JewelTheme.globalColors.panelBackground
                else JewelTheme.globalColors.panelBackground
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (success) "✓" else "⚠",
                style = JewelTheme.defaultTextStyle.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (success) JewelTheme.globalColors.text.info
                           else JewelTheme.globalColors.text.error
                )
            )
            Text(
                text = message,
                style = JewelTheme.defaultTextStyle.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp
                )
            )
        }
    }
}

/**
 * Error item for displaying error messages.
 */
@Composable
fun ErrorItem(
    error: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(JewelTheme.globalColors.panelBackground)
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⚠",
                style = JewelTheme.defaultTextStyle.copy(
                    color = JewelTheme.globalColors.text.error
                )
            )
            Text(
                text = error,
                style = JewelTheme.defaultTextStyle.copy(
                    color = JewelTheme.globalColors.text.error
                )
            )
        }
    }
}

