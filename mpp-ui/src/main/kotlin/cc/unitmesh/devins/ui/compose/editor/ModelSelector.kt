package cc.unitmesh.devins.ui.compose.editor

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.llm.ModelConfig

/**
 * 模型选择器
 * Provides a UI for selecting and configuring LLM models
 * 
 * @param initialConfig Initial model configuration (from database or previous session)
 * @param availableConfigs List of all saved configurations from database
 * @param onConfigChange Callback when model configuration changes
 */
@Composable
fun ModelSelector(
    initialConfig: ModelConfig? = null,
    availableConfigs: List<ModelConfig> = emptyList(),
    onConfigChange: (ModelConfig) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    
    // 当前配置：如果有 initialConfig 且有效则使用，否则为 null
    var currentConfig by remember(initialConfig) { 
        mutableStateOf(if (initialConfig?.isValid() == true) initialConfig else null)
    }

    // 显示文本：如果有配置显示配置信息，否则显示提示
    val displayText = remember(currentConfig) {
        if (currentConfig != null && currentConfig!!.isValid()) {
            "${currentConfig!!.provider.displayName} / ${currentConfig!!.modelName}"
        } else {
            "⚙️ Configure Model"
        }
    }

    TextButton(
        onClick = { expanded = true },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1
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
        // 显示已保存的配置
        if (availableConfigs.isNotEmpty()) {
            availableConfigs.forEach { config ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${config.provider.displayName} / ${config.modelName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        currentConfig = config
                        onConfigChange(config)
                        expanded = false
                    },
                    trailingIcon = {
                        // 如果是当前选中的配置，显示对勾
                        if (config == currentConfig) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
            
            HorizontalDivider()
        } else {
            // 没有配置时显示提示
            DropdownMenuItem(
                text = {
                    Text(
                        text = "No saved configurations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = { },
                enabled = false
            )
            
            HorizontalDivider()
        }

        // Configure button
        DropdownMenuItem(
            text = { Text("Configure Model...") },
            onClick = {
                showConfigDialog = true
                expanded = false
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configure",
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }

    if (showConfigDialog) {
        ModelConfigDialog(
            currentConfig = currentConfig ?: ModelConfig(),
            onDismiss = { showConfigDialog = false },
            onSave = { newConfig ->
                currentConfig = newConfig
                
                // 通知父组件配置已更改（父组件负责保存到数据库）
                onConfigChange(newConfig)
                showConfigDialog = false
            }
        )
    }
}