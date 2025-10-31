package cc.unitmesh.devins.ui.compose.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.llm.ModelConfig

/**
 * 底部工具栏（移动端优化版）
 * 左侧：模型选择器
 * 右侧：发送按钮
 */
@Composable
fun BottomToolbar(
    onSendClick: () -> Unit,
    sendEnabled: Boolean,
    initialModelConfig: ModelConfig? = null,
    availableConfigs: List<ModelConfig> = emptyList(),
    onModelConfigChange: (ModelConfig) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：模型选择器
        ModelSelector(
            initialConfig = initialModelConfig,
            availableConfigs = availableConfigs,
            onConfigChange = onModelConfigChange
        )
        
        // 右侧：发送按钮 - 使用更醒目的 FilledTonalButton
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
