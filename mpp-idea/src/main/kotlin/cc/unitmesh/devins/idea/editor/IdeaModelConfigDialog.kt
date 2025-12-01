package cc.unitmesh.devins.idea.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.ModelRegistry
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField

/**
 * Dialog for configuring LLM model settings for IntelliJ IDEA plugin.
 * Uses Jewel components for native IntelliJ look and feel.
 *
 * @deprecated Use IdeaModelConfigDialogWrapper.show() instead for proper z-index handling with SwingPanel.
 */
@Composable
fun IdeaModelConfigDialog(
    currentConfig: ModelConfig,
    currentConfigName: String? = null,
    onDismiss: () -> Unit,
    onSave: (configName: String, config: ModelConfig) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        IdeaModelConfigDialogContent(
            currentConfig = currentConfig,
            currentConfigName = currentConfigName,
            onDismiss = onDismiss,
            onSave = onSave
        )
    }
}

/**
 * Content for the model configuration dialog.
 * This is extracted to be used both in Compose Dialog and DialogWrapper.
 */
@Composable
fun IdeaModelConfigDialogContent(
    currentConfig: ModelConfig,
    currentConfigName: String? = null,
    onDismiss: () -> Unit,
    onSave: (configName: String, config: ModelConfig) -> Unit
) {
    // Use TextFieldState for Jewel TextField
    val configNameState = rememberTextFieldState(currentConfigName ?: "")
    var provider by remember { mutableStateOf(currentConfig.provider) }
    val modelNameState = rememberTextFieldState(currentConfig.modelName)
    val apiKeyState = rememberTextFieldState(currentConfig.apiKey)
    val temperatureState = rememberTextFieldState(currentConfig.temperature.toString())
    val maxTokensState = rememberTextFieldState(currentConfig.maxTokens.toString())
    val baseUrlState = rememberTextFieldState(currentConfig.baseUrl)
    var showApiKey by remember { mutableStateOf(false) }
    var providerExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(500.dp)
            .heightIn(max = 600.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(JewelTheme.globalColors.panelBackground)
            .onKeyEvent { event ->
                if (event.key == Key.Escape) {
                    onDismiss()
                    true
                } else false
            }
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Title
            Text(
                text = "Model Configuration",
                style = JewelTheme.defaultTextStyle.copy(fontSize = 18.sp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Config Name
            IdeaConfigFormField(label = "Config Name") {
                TextField(
                    state = configNameState,
                    placeholder = { Text("e.g., my-gpt4") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Provider Selector
            IdeaConfigFormField(label = "Provider") {
                IdeaProviderSelector(
                    provider = provider,
                    expanded = providerExpanded,
                    onExpandedChange = { providerExpanded = it },
                    onProviderSelect = { selectedProvider ->
                        provider = selectedProvider
                        val defaultModels = ModelRegistry.getAvailableModels(selectedProvider)
                        if (defaultModels.isNotEmpty()) {
                            modelNameState.edit { replace(0, length, defaultModels[0]) }
                        }
                        if (selectedProvider == LLMProviderType.OLLAMA && baseUrlState.text.isEmpty()) {
                            baseUrlState.edit { replace(0, length, "http://localhost:11434") }
                        }
                        providerExpanded = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Model Name
            val availableModels = remember(provider) { ModelRegistry.getAvailableModels(provider) }
            IdeaConfigFormField(label = "Model") {
                if (availableModels.isNotEmpty()) {
                    IdeaModelNameSelector(
                        modelNameState = modelNameState,
                        availableModels = availableModels,
                        expanded = modelExpanded,
                        onExpandedChange = { modelExpanded = it },
                        onModelSelect = { selectedModel ->
                            modelNameState.edit { replace(0, length, selectedModel) }
                            modelExpanded = false
                        }
                    )
                } else {
                    TextField(
                        state = modelNameState,
                        placeholder = { Text("Enter model name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // API Key
            IdeaConfigFormField(label = "API Key") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        state = apiKeyState,
                        placeholder = { Text("Enter API key") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showApiKey = !showApiKey },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (showApiKey) IdeaComposeIcons.VisibilityOff else IdeaComposeIcons.Visibility,
                            contentDescription = if (showApiKey) "Hide" else "Show",
                            tint = JewelTheme.globalColors.text.normal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Base URL (for certain providers)
            val needsBaseUrl = provider in listOf(
                LLMProviderType.OLLAMA, LLMProviderType.GLM, LLMProviderType.QWEN,
                LLMProviderType.KIMI, LLMProviderType.CUSTOM_OPENAI_BASE
            )
            if (needsBaseUrl) {
                Spacer(modifier = Modifier.height(12.dp))
                IdeaConfigFormField(label = "Base URL") {
                    TextField(
                        state = baseUrlState,
                        placeholder = { Text("e.g., http://localhost:11434") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Advanced Settings Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvanced = !showAdvanced }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (showAdvanced) IdeaComposeIcons.ExpandLess else IdeaComposeIcons.ExpandMore,
                    contentDescription = null,
                    tint = JewelTheme.globalColors.text.normal,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Advanced Settings",
                    style = JewelTheme.defaultTextStyle.copy(fontSize = 14.sp)
                )
            }

            if (showAdvanced) {
                Spacer(modifier = Modifier.height(8.dp))

                // Temperature
                IdeaConfigFormField(label = "Temperature") {
                    TextField(
                        state = temperatureState,
                        placeholder = { Text("0.0 - 2.0") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Max Tokens
                IdeaConfigFormField(label = "Max Tokens") {
                    TextField(
                        state = maxTokensState,
                        placeholder = { Text("e.g., 128000") },
                        modifier = Modifier.fillMaxWidth()
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
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(12.dp))
                DefaultButton(
                    onClick = {
                        val modelName = modelNameState.text.toString()
                        val apiKey = apiKeyState.text.toString()
                        val config = ModelConfig(
                            provider = provider,
                            modelName = modelName,
                            apiKey = apiKey,
                            temperature = temperatureState.text.toString().toDoubleOrNull() ?: 0.0,
                            maxTokens = maxTokensState.text.toString().toIntOrNull() ?: 128000,
                            baseUrl = baseUrlState.text.toString()
                        )
                        val configName = configNameState.text.toString()
                        val name = configName.ifEmpty { "${provider.name.lowercase()}-${modelName}" }
                        onSave(name, config)
                    },
                    enabled = modelNameState.text.isNotEmpty() && apiKeyState.text.isNotEmpty()
                ) {
                    Text("Save")
                }
            }
        }
    }
}

/**
 * Deprecated: Use IdeaModelConfigDialogWrapper.show() instead.
 */
@Deprecated("Use IdeaModelConfigDialogWrapper.show() for proper z-index handling")
@Composable
fun IdeaModelConfigDialogLegacy(
    currentConfig: ModelConfig,
    currentConfigName: String? = null,
    onDismiss: () -> Unit,
    onSave: (configName: String, config: ModelConfig) -> Unit
) {
    IdeaModelConfigDialog(currentConfig, currentConfigName, onDismiss, onSave)
}

/**
 * Form field wrapper with label
 */
@Composable
private fun IdeaConfigFormField(
    label: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = JewelTheme.defaultTextStyle.copy(fontSize = 13.sp),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        content()
    }
}

/**
 * Provider selector dropdown using Jewel's PopupMenu for proper z-index handling
 */
@Composable
private fun IdeaProviderSelector(
    provider: LLMProviderType,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onProviderSelect: (LLMProviderType) -> Unit
) {
    Box {
        OutlinedButton(
            onClick = { onExpandedChange(true) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(provider.name)
                Icon(
                    imageVector = IdeaComposeIcons.ArrowDropDown,
                    contentDescription = null,
                    tint = JewelTheme.globalColors.text.normal,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (expanded) {
            PopupMenu(
                onDismissRequest = {
                    onExpandedChange(false)
                    true
                },
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.widthIn(min = 200.dp, max = 300.dp)
            ) {
                LLMProviderType.entries.forEach { providerType ->
                    selectableItem(
                        selected = providerType == provider,
                        onClick = { onProviderSelect(providerType) }
                    ) {
                        Text(
                            text = providerType.name,
                            style = JewelTheme.defaultTextStyle.copy(fontSize = 13.sp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Model name selector with dropdown for known models using Jewel's PopupMenu
 */
@Composable
private fun IdeaModelNameSelector(
    modelNameState: TextFieldState,
    availableModels: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onModelSelect: (String) -> Unit
) {
    val modelName = modelNameState.text.toString()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                state = modelNameState,
                placeholder = { Text("Enter or select model") },
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onExpandedChange(!expanded) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = IdeaComposeIcons.ArrowDropDown,
                    contentDescription = "Select model",
                    tint = JewelTheme.globalColors.text.normal,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (expanded) {
            PopupMenu(
                onDismissRequest = {
                    onExpandedChange(false)
                    true
                },
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.widthIn(min = 200.dp, max = 400.dp)
            ) {
                availableModels.forEach { model ->
                    selectableItem(
                        selected = model == modelName,
                        onClick = { onModelSelect(model) }
                    ) {
                        Text(
                            text = model,
                            style = JewelTheme.defaultTextStyle.copy(fontSize = 13.sp)
                        )
                    }
                }
            }
        }
    }
}
