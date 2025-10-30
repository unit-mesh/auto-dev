package cc.unitmesh.devins.ui.compose.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.llm.ModelConfig

/**
 * 底部工具栏
 * 左侧：Agent 选择器 + 模型选择器
 * 右侧：@ 按钮 + / 按钮 + 发送按钮
 */
@Composable
fun BottomToolbar(
    onSendClick: () -> Unit,
    onAtClick: () -> Unit,
    onSlashClick: () -> Unit,
    sendEnabled: Boolean,
    onModelConfigChange: (ModelConfig) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：选择器
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AgentSelector()
            ModelSelector(onConfigChange = onModelConfigChange)
        }
        
        // 右侧：操作按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // @ 按钮 - Agent 提及
            IconButton(
                onClick = onAtClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AlternateEmail,
                    contentDescription = "@ mention",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // / 按钮 - Slash 命令
            IconButton(
                onClick = onSlashClick,
                modifier = Modifier.size(32.dp)
            ) {
                Text(
                    text = "/",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 发送按钮
            IconButton(
                onClick = onSendClick,
                enabled = sendEnabled,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (sendEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    }
                )
            }
        }
    }
}

/**
 * Agent 选择器
 */
@Composable
private fun AgentSelector() {
    var expanded by remember { mutableStateOf(false) }
    var selectedAgent by remember { mutableStateOf("Agent") }
    
    val agents = listOf(
        "Default",
        "clarify",
        "code-review",
        "test-gen",
        "refactor"
    )
    
    Row(
        modifier = Modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = selectedAgent,
                style = MaterialTheme.typography.bodySmall
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            agents.forEach { agent ->
                DropdownMenuItem(
                    text = { Text(agent) },
                    onClick = {
                        selectedAgent = agent
                        expanded = false
                    }
                )
            }
        }
    }
}
