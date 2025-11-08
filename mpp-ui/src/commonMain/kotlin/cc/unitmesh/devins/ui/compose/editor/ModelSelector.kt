package cc.unitmesh.devins.ui.compose.editor

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.ui.i18n.Strings
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.NamedModelConfig
import kotlinx.coroutines.launch

/**
 * 模型选择器
 * Provides a UI for selecting and configuring LLM models
 *
 * Loads configurations from ~/.autodev/config.yaml using ConfigManager.
 * No longer depends on database or external config lists.
 *
 * @param onConfigChange Callback when model configuration changes
 */
@Composable
fun ModelSelector(onConfigChange: (ModelConfig) -> Unit = {}) {
    var expanded by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }

    // Load configurations from file
    var availableConfigs by remember { mutableStateOf<List<NamedModelConfig>>(emptyList()) }
    var currentConfigName by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Load initial configuration
    LaunchedEffect(Unit) {
        try {
            val wrapper = ConfigManager.load()
            availableConfigs = wrapper.getAllConfigs()
            currentConfigName = wrapper.getActiveName()

            // Notify parent of initial config
            wrapper.getActiveModelConfig()?.let { onConfigChange(it) }
        } catch (e: Exception) {
            println("Failed to load configs: ${e.message}")
        }
    }

    // Get current config
    val currentConfig =
        remember(currentConfigName, availableConfigs) {
            availableConfigs.find { it.name == currentConfigName }
        }

    // 显示文本：如果有配置显示配置信息，否则显示提示
    val displayText =
        remember(currentConfig) {
            if (currentConfig != null) {
                "${currentConfig.provider} / ${currentConfig.model}"
            } else {
                "⚙️ ${Strings.configureModel}"
            }
        }

    OutlinedButton(
        onClick = { expanded = true },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
        )
        Icon(
            imageVector = AutoDevComposeIcons.ArrowDropDown,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
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
                            text = "${config.provider} / ${config.model}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        scope.launch {
                            try {
                                ConfigManager.setActive(config.name)
                                currentConfigName = config.name
                                onConfigChange(config.toModelConfig())
                                expanded = false
                            } catch (e: Exception) {
                                println("Failed to set active config: ${e.message}")
                            }
                        }
                    },
                    trailingIcon = {
                        // 如果是当前选中的配置，显示对勾
                        if (config.name == currentConfigName) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Check,
                                contentDescription = Strings.selected,
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
                        text = Strings.noSavedConfigs,
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
            text = { Text(Strings.configureModel) },
            onClick = {
                showConfigDialog = true
                expanded = false
            },
            leadingIcon = {
                Icon(
                    imageVector = AutoDevComposeIcons.Settings,
                    contentDescription = Strings.configure,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }

    if (showConfigDialog) {
        ModelConfigDialog(
            currentConfig = currentConfig?.toModelConfig() ?: ModelConfig(),
            currentConfigName = currentConfigName,
            onDismiss = { showConfigDialog = false },
            onSave = { configName, newModelConfig ->
                scope.launch {
                    try {
                        // If creating a new config (not editing current), ensure unique name
                        val existingNames = availableConfigs.map { it.name }
                        val finalConfigName =
                            if (currentConfigName != configName && configName in existingNames) {
                                // Auto-increment: my-glm -> my-glm-1 -> my-glm-2, etc.
                                ConfigManager.generateUniqueConfigName(configName, existingNames)
                            } else {
                                configName
                            }

                        // Convert ModelConfig to NamedModelConfig
                        val namedConfig =
                            NamedModelConfig.fromModelConfig(
                                name = finalConfigName,
                                config = newModelConfig
                            )

                        // Save to file
                        ConfigManager.saveConfig(namedConfig, setActive = true)

                        // Reload configs
                        val wrapper = ConfigManager.load()
                        availableConfigs = wrapper.getAllConfigs()
                        currentConfigName = wrapper.getActiveName()

                        // Notify parent
                        onConfigChange(newModelConfig)
                        showConfigDialog = false

                        if (finalConfigName != configName) {
                            println("✅ 配置名称已存在，自动重命名为: $finalConfigName")
                        }
                    } catch (e: Exception) {
                        println("Failed to save config: ${e.message}")
                    }
                }
            }
        )
    }
}
