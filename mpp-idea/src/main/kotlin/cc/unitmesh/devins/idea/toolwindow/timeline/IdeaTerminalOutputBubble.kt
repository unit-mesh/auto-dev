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
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * Terminal output bubble for displaying shell command results.
 * Similar to TerminalOutputItem in mpp-ui but using Jewel theming.
 */
@Composable
fun IdeaTerminalOutputBubble(
    item: JewelRenderer.TimelineItem.TerminalOutputItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .background(AutoDevColors.Neutral.c900)
                .padding(8.dp)
        ) {
            Column {
                // Command header
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$ ${item.command}",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontWeight = FontWeight.Bold,
                            color = AutoDevColors.Cyan.c400
                        )
                    )
                    val exitColor = if (item.exitCode == 0) AutoDevColors.Green.c400 else AutoDevColors.Red.c400
                    Text(
                        text = "exit: ${item.exitCode}",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 11.sp,
                            color = exitColor
                        )
                    )
                    Text(
                        text = "${item.executionTimeMs}ms",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 11.sp,
                            color = JewelTheme.globalColors.text.info
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Output content
                val outputText = item.output.take(1000) + if (item.output.length > 1000) "\n..." else ""
                Text(
                    text = outputText,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 12.sp,
                        color = AutoDevColors.Neutral.c300
                    )
                )
            }
        }
    }
}

