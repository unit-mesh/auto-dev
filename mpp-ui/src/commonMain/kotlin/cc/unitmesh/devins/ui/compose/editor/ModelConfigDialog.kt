package cc.unitmesh.devins.ui.compose.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
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
    currentConfigName: String? = null,
    onDismiss: () -> Unit,
    onSave: (configName: String, config: ModelConfig) -> Unit
) {
    var configName by remember { mutableStateOf(currentConfigName ?: "") }
    var provider by remember { mutableStateOf(currentConfig.provider) }
    var modelName by remember { mutableStateOf(currentConfig.modelName) }
    var apiKey by remember { mutableStateOf(currentConfig.apiKey) }
    var temperature by remember { mutableStateOf(currentConfig.temperature.toString()) }
    var maxTokens by remember { mutableStateOf(currentConfig.maxTokens.toString()) }
    var baseUrl by remember { mutableStateOf(currentConfig.baseUrl) }
    var showApiKey by remember { mutableStateOf(false) }
    var expandedProvider by remember { mutableStateOf(false) }
    var expandedModel by remember { mutableStateOf(false) }
    var expandedAdvanced by remember { mutableStateOf(false) }

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
                Text(
                    text = Strings.modelConfigTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = Strings.configName,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = configName,
                    onValueChange = { configName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(Strings.configNamePlaceholder) },
                    supportingText = {
                        Text(
                            Strings.configNameHint,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    singleLine = true
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
                                when (provider) {
                                    LLMProviderType.GLM -> Strings.modelPlaceholderGLM
                                    LLMProviderType.QWEN -> Strings.modelPlaceholderQwen
                                    LLMProviderType.KIMI -> Strings.modelPlaceholderKimi
                                    LLMProviderType.CUSTOM_OPENAI_BASE -> Strings.modelPlaceholderCustom
                                    else -> Strings.enterModel
                                }
                            )
                        },
                        supportingText = {
                            Text(
                                when (provider) {
                                    LLMProviderType.GLM -> Strings.modelHintGLM
                                    LLMProviderType.QWEN -> Strings.modelHintQwen
                                    LLMProviderType.KIMI -> Strings.modelHintKimi
                                    LLMProviderType.CUSTOM_OPENAI_BASE -> Strings.modelHintCustom
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
                                    imageVector = if (showApiKey) AutoDevComposeIcons.Visibility else AutoDevComposeIcons.VisibilityOff,
                                    contentDescription = if (showApiKey) Strings.hideApiKey else Strings.showApiKey
                                )
                            }
                        },
                        placeholder = { Text(Strings.enterApiKey) },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Base URL (for Ollama, GLM, Qwen, Kimi and Custom OpenAI-compatible providers)
                if (provider == LLMProviderType.OLLAMA || provider == LLMProviderType.GLM ||
                    provider == LLMProviderType.QWEN || provider == LLMProviderType.KIMI ||
                    provider == LLMProviderType.CUSTOM_OPENAI_BASE
                ) {
                    Text(
                        text = Strings.baseUrl,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Pre-fill baseUrl if empty
                    if (baseUrl.isEmpty()) {
                        baseUrl = ModelRegistry.getDefaultBaseUrl(provider)
                    }

                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                when (provider) {
                                    LLMProviderType.OLLAMA -> Strings.baseUrlPlaceholderOllama
                                    LLMProviderType.GLM -> Strings.baseUrlPlaceholderGLM
                                    LLMProviderType.QWEN -> Strings.baseUrlPlaceholderQwen
                                    LLMProviderType.KIMI -> Strings.baseUrlPlaceholderKimi
                                    LLMProviderType.CUSTOM_OPENAI_BASE -> Strings.baseUrlPlaceholderCustom
                                    else -> Strings.baseUrlPlaceholderDefault
                                }
                            )
                        },
                        supportingText = {
                            Text(
                                when (provider) {
                                    LLMProviderType.OLLAMA -> Strings.baseUrlHintOllama
                                    LLMProviderType.GLM -> Strings.baseUrlHintGLM
                                    LLMProviderType.QWEN -> Strings.baseUrlHintQwen
                                    LLMProviderType.KIMI -> Strings.baseUrlHintKimi
                                    LLMProviderType.CUSTOM_OPENAI_BASE -> Strings.baseUrlHintCustom
                                    else -> ""
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Advanced Parameters Section - Collapsible
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedAdvanced = !expandedAdvanced }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.advancedParameters,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Icon(
                        imageVector = if (expandedAdvanced) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                        contentDescription = if (expandedAdvanced) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                AnimatedVisibility(
                    visible = expandedAdvanced,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
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
                                placeholder = { Text("0.7") },
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
                            placeholder = { Text("2000") },
                            supportingText = { Text(Strings.maxResponseLength, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

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
                            onSave(configName.trim(), config)
                        },
                        enabled =
                            configName.trim().isNotBlank() &&
                                when (provider) {
                                    LLMProviderType.OLLAMA ->
                                        modelName.isNotBlank() && baseUrl.isNotBlank()
                                    LLMProviderType.GLM, LLMProviderType.QWEN, LLMProviderType.KIMI, LLMProviderType.CUSTOM_OPENAI_BASE ->
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
