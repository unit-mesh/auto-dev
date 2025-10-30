package cc.unitmesh.devins.ui.compose.editor

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
 * @param onConfigChange Callback when model configuration changes
 */
@Composable
fun ModelSelector(
    initialConfig: ModelConfig? = null,
    onConfigChange: (ModelConfig) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var currentConfig by remember(initialConfig) { 
        mutableStateOf(initialConfig ?: ModelConfig.default()) 
    }

    // Display text showing provider and model
    val displayText = remember(currentConfig) {
        "${currentConfig.provider.displayName} / ${currentConfig.modelName}"
    }

    val recentConfigs = remember {
        mutableStateListOf(
            ModelConfig.default(),
        )
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
        // Quick switch to recent configs
        recentConfigs.forEachIndexed { index, config ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = "${config.provider.displayName} / ${config.modelName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                onClick = {
                    if (config.isValid()) {
                        currentConfig = config
                        onConfigChange(config)
                        expanded = false
                    } else {
                        // If config is invalid, open settings dialog
                        showConfigDialog = true
                        expanded = false
                    }
                },
                trailingIcon = {
                    if (!config.isValid()) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configure",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }

        Divider()

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

    // Configuration Dialog
    if (showConfigDialog) {
        ModelConfigDialog(
            currentConfig = currentConfig,
            onDismiss = { showConfigDialog = false },
            onSave = { newConfig ->
                currentConfig = newConfig
                
                // Update or add to recent configs
                val existingIndex = recentConfigs.indexOfFirst { 
                    it.provider == newConfig.provider && it.modelName == newConfig.modelName 
                }
                if (existingIndex >= 0) {
                    recentConfigs[existingIndex] = newConfig
                } else {
                    // Add new config and keep only last 5
                    recentConfigs.add(0, newConfig)
                    if (recentConfigs.size > 5) {
                        recentConfigs.removeAt(recentConfigs.size - 1)
                    }
                }
                
                onConfigChange(newConfig)
                showConfigDialog = false
            }
        )
    }
}