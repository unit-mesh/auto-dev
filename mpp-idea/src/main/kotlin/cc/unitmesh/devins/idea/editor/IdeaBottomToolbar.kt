package cc.unitmesh.devins.idea.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*

/**
 * Bottom toolbar for the input section.
 * Provides send/stop buttons, @ trigger for agent completion, settings, and token info.
 * 
 * Uses Jewel components for native IntelliJ IDEA look and feel.
 */
@Composable
fun IdeaBottomToolbar(
    onSendClick: () -> Unit,
    sendEnabled: Boolean,
    isExecuting: Boolean = false,
    onStopClick: () -> Unit = {},
    onAtClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    workspacePath: String? = null,
    totalTokens: Int? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: workspace and token info
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Workspace indicator
            if (!workspacePath.isNullOrEmpty()) {
                val projectName = workspacePath.substringAfterLast('/')
                    .ifEmpty { workspacePath.substringAfterLast('\\') }
                    .ifEmpty { "Project" }

                Box(
                    modifier = Modifier
                        .background(
                            JewelTheme.globalColors.panelBackground.copy(alpha = 0.8f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "📁",
                            style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp)
                        )
                        Text(
                            text = projectName,
                            style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp),
                            maxLines = 1
                        )
                    }
                }
            }

            // Token usage indicator
            if (totalTokens != null && totalTokens > 0) {
                Box(
                    modifier = Modifier
                        .background(AutoDevColors.Blue.c400.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Token",
                            style = JewelTheme.defaultTextStyle.copy(fontSize = 11.sp)
                        )
                        Text(
                            text = "$totalTokens",
                            style = JewelTheme.defaultTextStyle.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // Right side: action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // @ trigger button for agent completion
            IconButton(
                onClick = onAtClick,
                modifier = Modifier.size(32.dp)
            ) {
                Text(
                    text = "@",
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Settings button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(32.dp)
            ) {
                Text(
                    text = "⚙",
                    style = JewelTheme.defaultTextStyle.copy(fontSize = 14.sp)
                )
            }

            // Send or Stop button
            if (isExecuting) {
                DefaultButton(
                    onClick = onStopClick,
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "⏹ Stop",
                        style = JewelTheme.defaultTextStyle.copy(
                            color = AutoDevColors.Red.c400
                        )
                    )
                }
            } else {
                DefaultButton(
                    onClick = onSendClick,
                    enabled = sendEnabled,
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Send",
                        style = JewelTheme.defaultTextStyle
                    )
                }
            }
        }
    }
}

