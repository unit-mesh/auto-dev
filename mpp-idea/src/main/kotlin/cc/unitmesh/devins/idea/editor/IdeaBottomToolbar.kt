package cc.unitmesh.devins.idea.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import cc.unitmesh.llm.NamedModelConfig
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.component.Icon

/**
 * Bottom toolbar for the input section.
 * Provides send/stop buttons, model selector, MCP config, and token info.
 *
 * Layout: ModelSelector - Token Info | MCP Config - Prompt Optimization - Send Button
 * - Left side: Model configuration (blends with background)
 * - Right side: MCP config, prompt optimization, and send
 *
 * Note: @ and / triggers are now in the top toolbar (IdeaTopToolbar).
 */
@Composable
fun IdeaBottomToolbar(
    onSendClick: () -> Unit,
    sendEnabled: Boolean,
    isExecuting: Boolean = false,
    onStopClick: () -> Unit = {},
    onPromptOptimizationClick: () -> Unit = {},
    totalTokens: Int? = null,
    // Model selector props
    availableConfigs: List<NamedModelConfig> = emptyList(),
    currentConfigName: String? = null,
    onConfigSelect: (NamedModelConfig) -> Unit = {},
    onConfigureClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMcpConfigDialog by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Model selector and token info
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Model selector (transparent, blends with background)
            IdeaModelSelector(
                availableConfigs = availableConfigs,
                currentConfigName = currentConfigName,
                onConfigSelect = onConfigSelect,
                onConfigureClick = onConfigureClick
            )

            // Token usage indicator (subtle)
            if (totalTokens != null && totalTokens > 0) {
                Text(
                    text = "${totalTokens}t",
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 11.sp,
                        color = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f)
                    )
                )
            }
        }

        // Right side: action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // MCP Config button - opens MCP configuration dialog
            IconButton(
                onClick = { showMcpConfigDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = IdeaComposeIcons.Settings,
                    contentDescription = "MCP Configuration",
                    tint = JewelTheme.globalColors.text.normal,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Prompt Optimization button
            IconButton(
                onClick = onPromptOptimizationClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = IdeaComposeIcons.AutoAwesome,
                    contentDescription = "Prompt Optimization",
                    tint = JewelTheme.globalColors.text.normal,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Send or Stop button
            if (isExecuting) {
                DefaultButton(
                    onClick = onStopClick,
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = IdeaComposeIcons.Stop,
                            contentDescription = "Stop",
                            tint = AutoDevColors.Red.c400,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Stop",
                            style = JewelTheme.defaultTextStyle.copy(
                                color = AutoDevColors.Red.c400
                            )
                        )
                    }
                }
            } else {
                DefaultButton(
                    onClick = onSendClick,
                    enabled = sendEnabled,
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = IdeaComposeIcons.Send,
                            contentDescription = "Send",
                            tint = JewelTheme.globalColors.text.normal,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Send",
                            style = JewelTheme.defaultTextStyle
                        )
                    }
                }
            }
        }
    }

    // MCP Configuration Dialog
    if (showMcpConfigDialog) {
        IdeaMcpConfigDialog(
            onDismiss = { showMcpConfigDialog = false }
        )
    }
}

