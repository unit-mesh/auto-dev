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
 * Provides send/stop buttons, @ trigger for agent completion, / command trigger, model selector, settings, and token info.
 *
 * Layout: ModelSelector - Workspace - Token Info | @ Symbol - / Symbol - MCP Settings - Prompt Optimization - Send Button
 * - Left side: Model configuration
 * - Right side: MCP and prompt optimization
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
    onSlashClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onPromptOptimizationClick: () -> Unit = {},
    workspacePath: String? = null,
    totalTokens: Int? = null,
    // Model selector props
    availableConfigs: List<NamedModelConfig> = emptyList(),
    currentConfigName: String? = null,
    onConfigSelect: (NamedModelConfig) -> Unit = {},
    onConfigureClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Model selector, workspace and token info
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Model selector (moved to left)
            IdeaModelSelector(
                availableConfigs = availableConfigs,
                currentConfigName = currentConfigName,
                onConfigSelect = onConfigSelect,
                onConfigureClick = onConfigureClick
            )

            // Workspace indicator
            if (!workspacePath.isNullOrEmpty()) {
                // Extract project name from path, handling both Unix and Windows separators
                val projectName = workspacePath
                    .replace('\\', '/')  // Normalize to Unix separator
                    .substringAfterLast('/')
                    .ifEmpty { "Project" }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.8f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = IdeaComposeIcons.Folder,
                            contentDescription = null,
                            tint = JewelTheme.globalColors.text.normal,
                            modifier = Modifier.size(12.dp)
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
                        .clip(RoundedCornerShape(4.dp))
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

        // Right side: action buttons (MCP and prompt optimization on the right)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // @ trigger button for agent completion
            IconButton(
                onClick = onAtClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = IdeaComposeIcons.AlternateEmail,
                    contentDescription = "@ Agent",
                    tint = JewelTheme.globalColors.text.normal,
                    modifier = Modifier.size(18.dp)
                )
            }

            // / trigger button for slash commands
            IconButton(
                onClick = onSlashClick,
                modifier = Modifier.size(32.dp)
            ) {
                Text(
                    text = "/",
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // MCP Settings button (moved to right side)
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = IdeaComposeIcons.Settings,
                    contentDescription = "MCP Settings",
                    tint = JewelTheme.globalColors.text.normal,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Prompt Optimization button (new, on right side)
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
}

