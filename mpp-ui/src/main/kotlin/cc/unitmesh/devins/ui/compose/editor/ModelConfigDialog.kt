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
import cc.unitmesh.devins.llm.LLMProviderType
import cc.unitmesh.devins.llm.ModelConfig

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
    var topP by remember { mutableStateOf(currentConfig.topP.toString()) }
    var baseUrl by remember { mutableStateOf(currentConfig.baseUrl) }
    var showApiKey by remember { mutableStateOf(false) }
    var expandedProvider by remember { mutableStateOf(false) }
    var expandedModel by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .width(600.dp)
                .heightIn(max = 700.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title
                Text(
                    text = "LLM Model Configuration",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Provider Selection
                Text(
                    text = "Provider",
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
                        modifier = Modifier
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
                                    val defaultModels = ModelConfig.getDefaultModelsForProvider(providerType)
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
                    text = "Model",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedModel,
                    onExpandedChange = { expandedModel = it }
                ) {
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModel) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        placeholder = { Text("Enter or select model name") }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedModel,
                        onDismissRequest = { expandedModel = false }
                    ) {
                        ModelConfig.getDefaultModelsForProvider(provider).forEach { model ->
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

                Spacer(modifier = Modifier.height(16.dp))

                // API Key (not for Ollama)
                if (provider != LLMProviderType.OLLAMA) {
                    Text(
                        text = "API Key",
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
                                    contentDescription = if (showApiKey) "Hide API key" else "Show API key"
                                )
                            }
                        },
                        placeholder = { Text("Enter your API key") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Base URL (for Ollama and custom endpoints)
                if (provider == LLMProviderType.OLLAMA) {
                    Text(
                        text = "Base URL",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("http://localhost:11434") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Advanced Parameters Section
                Text(
                    text = "Advanced Parameters",
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
                        label = { Text("Temperature") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = { Text("0.0 - 2.0", style = MaterialTheme.typography.bodySmall) }
                    )

                    // Top P
                    OutlinedTextField(
                        value = topP,
                        onValueChange = { topP = it },
                        label = { Text("Top P") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = { Text("0.0 - 1.0", style = MaterialTheme.typography.bodySmall) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Max Tokens
                OutlinedTextField(
                    value = maxTokens,
                    onValueChange = { maxTokens = it },
                    label = { Text("Max Tokens") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Maximum response length", style = MaterialTheme.typography.bodySmall) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val config = ModelConfig(
                                provider = provider,
                                modelName = modelName.trim(),
                                apiKey = apiKey.trim(),
                                temperature = temperature.toDoubleOrNull() ?: 0.7,
                                maxTokens = maxTokens.toIntOrNull() ?: 2000,
                                topP = topP.toDoubleOrNull() ?: 1.0,
                                baseUrl = baseUrl.trim()
                            )
                            onSave(config)
                        },
                        enabled = when (provider) {
                            LLMProviderType.OLLAMA -> modelName.isNotBlank() && baseUrl.isNotBlank()
                            else -> apiKey.isNotBlank() && modelName.isNotBlank()
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

