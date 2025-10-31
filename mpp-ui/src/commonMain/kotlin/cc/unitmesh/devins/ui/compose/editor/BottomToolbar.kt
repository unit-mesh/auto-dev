package cc.unitmesh.devins.ui.compose.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.Platform
import cc.unitmesh.llm.ModelConfig

/**
 * 底部工具栏（重新设计版）
 * 布局：Agent - Model Selector - @ Symbol - / Symbol - Send Button
 * - 移动端：通过顶部菜单控制 Agent，底部显示当前选择
 * - Desktop：完整显示所有功能
 */
@Composable
fun BottomToolbar(
    onSendClick: () -> Unit,
    sendEnabled: Boolean,
    onAtClick: () -> Unit = {},
    onSlashClick: () -> Unit = {},
    selectedAgent: String = "Default",
    modifier: Modifier = Modifier,
    initialModelConfig: ModelConfig? = null,
    availableConfigs: List<ModelConfig> = emptyList(),
    onModelConfigChange: (ModelConfig) -> Unit = {}
) {
    val isAndroid = Platform.isAndroid
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：Agent + Model Selector
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Agent 显示（只读，点击打开顶部菜单配置）
            if (!isAndroid || selectedAgent != "Default") {
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
                            imageVector = Icons.Default.SmartToy,
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
            
            // Model Selector（Desktop 或移动端都显示）
            if (!isAndroid) {
                ModelSelector(
                    initialConfig = initialModelConfig,
                    availableConfigs = availableConfigs,
                    onConfigChange = onModelConfigChange
                )
            }
        }
        
        // 右侧：@ Symbol + / Symbol + Send Button
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
                    imageVector = Icons.Default.AlternateEmail,
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
            
            // 发送按钮
            FilledTonalButton(
                onClick = onSendClick,
                enabled = sendEnabled,
                modifier = Modifier.height(if (isAndroid) 40.dp else 38.dp),
                contentPadding = PaddingValues(horizontal = if (isAndroid) 20.dp else 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
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
