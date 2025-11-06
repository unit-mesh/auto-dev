package cc.unitmesh.devins.ui.compose.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cc.unitmesh.devins.ui.i18n.Strings
import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.ModelRegistry

/**
 * Dialog for configuring LLM model settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelConfigDialog(
    currentConfig: ModelConfig,
    onDismiss: () -> Unit,
    onSave: (ModelConfig) -> Unit
) {
    var provider by remember { mutableStateOf(currentConfig.provider) }
    var modelName by remember { mutableStateOf(currentConfig.modelName) }
    var apiKey by remember { mutableStateOf(currentConfig.apiKey) }
    var temperature by remember { mutableStateOf(currentConfig.temperature.toString()) }
    var maxTokens by remember { mutableStateOf(currentConfig.maxTokens.toString()) }
    var baseUrl by remember { mutableStateOf(currentConfig.baseUrl) }
    var showApiKey by remember { mutableStateOf(false) }
    var expandedProvider by remember { mutableStateOf(false) }
    var expandedModel by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier =
                Modifier
                    .width(600.dp)
                    .heightIn(max = 700.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
            ) {
                // Title
                Text(
                    text = Strings.modelConfigTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Provider Selection
                Text(
                    text = Strings.provider,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedProvider,
                    onExpandedChange = { expandedProvider = it }
                ) {
                    OutlinedTextField(
                        value = provider.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProvider) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedProvider,
                        onDismissRequest = { expandedProvider = false }
                    ) {
                        LLMProviderType.entries.forEach { providerType ->
                            DropdownMenuItem(
                                text = { Text(providerType.displayName) },
                                onClick = {
                                    provider = providerType
                                    // Update model list when provider changes
                                    val defaultModels = ModelRegistry.getAvailableModels(providerType)
                                    if (defaultModels.isNotEmpty()) {
                                        modelName = defaultModels[0]
                                    }
                                    // Set default base URL for Ollama
                                    if (providerType == LLMProviderType.OLLAMA && baseUrl.isEmpty()) {
                                        baseUrl = "http://localhost:11434"
                                    }
                                    expandedProvider = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Model Name Selection
                Text(
                    text = Strings.model,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Get available models for this provider
                val availableModels = remember(provider) { ModelRegistry.getAvailableModels(provider) }
                
                if (availableModels.isNotEmpty()) {
                    // Show dropdown with predefined models + allow custom input
                    ExposedDropdownMenuBox(
                        expanded = expandedModel,
                        onExpandedChange = { expandedModel = it }
                    ) {
                        OutlinedTextField(
                            value = modelName,
                            onValueChange = { modelName = it },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModel) },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            placeholder = { Text(Strings.enterModel) },
                            supportingText = { Text(Strings.modelHint, style = MaterialTheme.typography.bodySmall) }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedModel,
                            onDismissRequest = { expandedModel = false }
                        ) {
                            availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = {
                                        modelName = model
                                        expandedModel = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // For custom providers (CUSTOM_OPENAI_BASE, etc.), show plain text input
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { 
                            Text(
                                if (provider == LLMProviderType.CUSTOM_OPENAI_BASE) 
                                    "e.g., glm-4-plus, deepseek-chat" 
                                else 
                                    Strings.enterModel
                            ) 
                        },
                        supportingText = { 
                            Text(
                                when (provider) {
                                    LLMProviderType.CUSTOM_OPENAI_BASE -> 
                                        "输入 OpenAI 兼容模型名称（如 glm-4-plus）"
                                    else -> Strings.modelHint
                                },
                                style = MaterialTheme.typography.bodySmall
                            ) 
                        },
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // API Key (not for Ollama)
                if (provider != LLMProviderType.OLLAMA) {
                    Text(
                        text = Strings.apiKey,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (showApiKey) Strings.hideApiKey else Strings.showApiKey
                                )
                            }
                        },
                        placeholder = { Text(Strings.enterApiKey) },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Base URL (for Ollama and Custom OpenAI-compatible providers)
                if (provider == LLMProviderType.OLLAMA || provider == LLMProviderType.CUSTOM_OPENAI_BASE) {
                    Text(
                        text = Strings.baseUrl,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { 
                            Text(
                                when (provider) {
                                    LLMProviderType.OLLAMA -> "http://localhost:11434"
                                    LLMProviderType.CUSTOM_OPENAI_BASE -> "https://open.bigmodel.cn/api/paas/v4"
                                    else -> "https://api.example.com"
                                }
                            ) 
                        },
                        supportingText = {
                            Text(
                                when (provider) {
                                    LLMProviderType.OLLAMA -> "Ollama 服务器地址"
                                    LLMProviderType.CUSTOM_OPENAI_BASE -> "OpenAI 兼容 API 地址（不含 /chat/completions）"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Advanced Parameters Section
                Text(
                    text = Strings.advancedParameters,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Temperature
                    OutlinedTextField(
                        value = temperature,
                        onValueChange = { temperature = it },
                        label = { Text(Strings.temperature) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = { Text(Strings.temperatureRange, style = MaterialTheme.typography.bodySmall) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Max Tokens
                OutlinedTextField(
                    value = maxTokens,
                    onValueChange = { maxTokens = it },
                    label = { Text(Strings.maxTokens) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text(Strings.maxResponseLength, style = MaterialTheme.typography.bodySmall) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Strings.cancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val config =
                                ModelConfig(
                                    provider = provider,
                                    modelName = modelName.trim(),
                                    apiKey = apiKey.trim(),
                                    temperature = temperature.toDoubleOrNull() ?: 0.0,
                                    maxTokens = maxTokens.toIntOrNull() ?: 2000,
                                    baseUrl = baseUrl.trim()
                                )
                            onSave(config)
                        },
                        enabled =
                            when (provider) {
                                LLMProviderType.OLLAMA -> 
                                    modelName.isNotBlank() && baseUrl.isNotBlank()
                                LLMProviderType.CUSTOM_OPENAI_BASE -> 
                                    modelName.isNotBlank() && baseUrl.isNotBlank() && apiKey.isNotBlank()
                                else -> 
                                    apiKey.isNotBlank() && modelName.isNotBlank()
                            }
                    ) {
                        Text(Strings.save)
                    }
                }
            }
        }
    }
}
