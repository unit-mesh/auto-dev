package cc.unitmesh.devins.ui.compose.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.Platform
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.llm.ModelConfig

/**
 * 底部工具栏（重新设计版）
 * 布局：Workspace - Agent - Model Selector - @ Symbol - / Symbol - Settings - Send Button
 * - 移动端：通过顶部菜单控制 Agent，底部显示当前选择
 * - Desktop：完整显示所有功能
 *
 * ModelSelector now loads configs from ConfigManager internally.
 */
@Composable
fun BottomToolbar(
    onSendClick: () -> Unit,
    sendEnabled: Boolean,
    isExecuting: Boolean = false,
    onStopClick: () -> Unit = {},
    onAtClick: () -> Unit = {},
    onEnhanceClick: () -> Unit = {},
    isEnhancing: Boolean = false,
    onSettingsClick: () -> Unit = {},
    workspacePath: String? = null,
    totalTokenInfo: cc.unitmesh.llm.compression.TokenInfo? = null,
    modifier: Modifier = Modifier,
    onModelConfigChange: (ModelConfig) -> Unit = {}
) {
    val isMobile = Platform.isAndroid || Platform.isIOS

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (!workspacePath.isNullOrEmpty()) {
                val projectName =
                    workspacePath.substringAfterLast('/')
                        .ifEmpty { workspacePath.substringAfterLast('\\') }
                        .ifEmpty { "Project" }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = projectName,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            modifier = Modifier.widthIn(max = if (isMobile) 80.dp else 120.dp)
                        )
                    }
                }
            }

//            if (!isMobile || selectedAgent != "Default") {
//                Surface(
//                    shape = MaterialTheme.shapes.small,
//                    color = MaterialTheme.colorScheme.secondaryContainer,
//                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
//                ) {
//                    Row(
//                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(4.dp)
//                    ) {
//                        Icon(
//                            imageVector = AutoDevComposeIcons.SmartToy,
//                            contentDescription = null,
//                            modifier = Modifier.size(14.dp)
//                        )
//                        Text(
//                            text = selectedAgent,
//                            style = MaterialTheme.typography.labelSmall
//                        )
//                    }
//                }
//            }

            if (!isMobile) {
                ModelSelector(
                    onConfigChange = onModelConfigChange
                )
            }

            // Display total token usage
            if (totalTokenInfo != null && totalTokenInfo.totalTokens > 0) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Token",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "${totalTokenInfo.totalTokens}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onAtClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = AutoDevComposeIcons.AlternateEmail,
                    contentDescription = "@ Agent",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Prompt Enhancement button (Ctrl+P)
            IconButton(
                onClick = onEnhanceClick,
                enabled = !isEnhancing,
                modifier = Modifier.size(36.dp)
            ) {
                if (isEnhancing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = AutoDevComposeIcons.AutoAwesome,
                        contentDescription = "Enhance Prompt (Ctrl+P)",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = AutoDevComposeIcons.Settings,
                    contentDescription = "MCP Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (isExecuting) {
                FilledTonalButton(
                    onClick = onStopClick,
                    modifier = Modifier.height(if (isMobile) 40.dp else 38.dp),
                    contentPadding = PaddingValues(horizontal = if (isMobile) 20.dp else 16.dp),
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Stop",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            } else {
                FilledTonalButton(
                    onClick = onSendClick,
                    enabled = sendEnabled,
                    modifier = Modifier.height(if (isMobile) 40.dp else 38.dp),
                    contentPadding = PaddingValues(horizontal = if (isMobile) 20.dp else 16.dp)
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Send",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
