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
    onSlashClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    selectedAgent: String = "Default",
    workspacePath: String? = null,
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
        // 左侧：Workspace + Agent + Model Selector
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Workspace 指示器（显示项目名称）
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
            // Agent 显示（只读，点击打开顶部菜单配置）
            if (!isMobile || selectedAgent != "Default") {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.SmartToy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = selectedAgent,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            if (!isMobile) {
                ModelSelector(
                    onConfigChange = onModelConfigChange
                )
            }
        }

        // 右侧：@ Symbol + / Symbol + Settings + Send Button
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // @ 按钮 - 触发 Agent 补全
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

            // / 按钮 - 触发命令补全
            IconButton(
                onClick = onSlashClick,
                modifier = Modifier.size(36.dp)
            ) {
                Text(
                    text = "/",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Settings 按钮 - 打开 MCP 工具配置
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

            // 发送按钮 / 停止按钮
            if (isExecuting) {
                // 执行中显示 Stop 按钮
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
                // 未执行时显示 Send 按钮
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
