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
 * 底部工具栏（平台自适应）
 * - 移动端：只显示 Send 按钮（配置在顶部菜单）
 * - Desktop：显示模型选择器 + Send 按钮
 */
@Composable
fun BottomToolbar(
    onSendClick: () -> Unit,
    sendEnabled: Boolean,
    modifier: Modifier = Modifier,
    // Desktop 相关参数（移动端不使用）
    initialModelConfig: ModelConfig? = null,
    availableConfigs: List<ModelConfig> = emptyList(),
    onModelConfigChange: (ModelConfig) -> Unit = {}
) {
    val isAndroid = Platform.isAndroid
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = if (isAndroid) Arrangement.End else Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Desktop: 显示模型选择器
        if (!isAndroid) {
            ModelSelector(
                initialConfig = initialModelConfig,
                availableConfigs = availableConfigs,
                onConfigChange = onModelConfigChange
            )
        }
        
        // 发送按钮 - 平台自适应样式
        if (isAndroid) {
            // 移动端：更大更醒目
            FilledTonalButton(
                onClick = onSendClick,
                enabled = sendEnabled,
                modifier = Modifier.height(44.dp),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Send",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        } else {
            // Desktop: 标准样式
            FilledTonalButton(
                onClick = onSendClick,
                enabled = sendEnabled,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send")
            }
        }
    }
}
