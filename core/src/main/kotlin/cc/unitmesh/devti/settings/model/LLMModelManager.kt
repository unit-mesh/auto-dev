package cc.unitmesh.devti.settings.model

import cc.unitmesh.devti.llm2.GithubCopilotManager
import cc.unitmesh.devti.llm2.LLMProvider2
import cc.unitmesh.devti.llm2.model.LlmConfig
import cc.unitmesh.devti.llms.custom.Message
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.settings.dialog.LLMDialog
import cc.unitmesh.devti.settings.ui.ModelItem
import cc.unitmesh.devti.util.AutoDevAppScope
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.builtins.ListSerializer
import javax.swing.JTable
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableModel

/**
 * Manager class for LLM models
 */
class LLMModelManager(
    private val project: Project?,
    private val settings: AutoDevSettingsState,
    private val onModified: () -> Unit
) {
    /**
     * Check if a model is custom (not GitHub Copilot)
     */
    fun isCustomLLM(modelName: String): Boolean {
        // GitHub models start with "Github: "
        if (modelName.startsWith("Github: ")) {
            return false
        }
        val userModels = LlmConfig.load()
        return userModels.any { it.name == modelName }
    }

    /**
     * Get all available models (GitHub Copilot + Custom LLMs)
     * Used by AutoDevInputSection to populate model selector
     */
    fun getAllAvailableModels(): List<ModelItem> {
        val models = mutableListOf<ModelItem>()
        val manager = service<GithubCopilotManager>()
        if (manager.isInitialized()) {
            val githubModels = manager.getSupportedModels(forceRefresh = false)
            githubModels?.forEach { model ->
                models.add(ModelItem(
                    displayName = "Github: ${model.id}",
                    id = model.id,
                    isCustom = false
                ))
            }
        }
        
        val userModels = LlmConfig.load()
        userModels.forEach { llm ->
            models.add(ModelItem(
                displayName = llm.name,
                id = llm.name,
                isCustom = true
            ))
        }
        
        return models
    }
    
    /**
     * Get model ID from provider name
     * Used by AutoDevInputSection to find current selected model
     */
    fun getModelIdFromProvider(providerName: String): String {
        if (providerName.isEmpty() || providerName == "Default") {
            return "Default"
        }
        
        // For GitHub models, extract model ID from "Github: model-id" format
        if (providerName.startsWith("Github: ")) {
            return providerName.removePrefix("Github: ")
        }
        
        // For custom models, the provider name is the model name
        return providerName
    }
    
    /**
     * Get provider name from model ID
     * Used by AutoDevInputSection when model selection changes
     */
    fun getProviderFromModelId(modelId: String): String {
        if (modelId.isEmpty() || modelId == "Default") {
            return "Default"
        }
        
        // Check if it's a GitHub Copilot model
        val manager = service<GithubCopilotManager>()
        if (manager.isInitialized()) {
            val githubModels = manager.getSupportedModels(forceRefresh = false)
            val isGithubModel = githubModels?.any { it.id == modelId } == true
            if (isGithubModel) {
                return "Github: $modelId"
            }
        }
        
        // For custom models, return the model name directly
        return modelId
    }

    /**
     * Create a new LLM configuration dialog
     */
    fun createNewLLM(existingLlm: LlmConfig? = null) {
        val dialog = LLMDialog(
            project = project,
            settings = settings,
            existingLlm = existingLlm,
            onSave = {
                onModified()
            }
        )

        dialog.show()
    }

    /**
     * Edit LLM at specific row in the table
     */
    fun editLLMAtRow(table: JTable, row: Int) {
        val modelName = table.getValueAt(row, 0) as? String ?: return

        // Only allow editing custom LLMs
        if (isCustomLLM(modelName)) {
            val userModels = LlmConfig.load()
            val llmToEdit = userModels.find { it.name == modelName }
            if (llmToEdit != null) {
                createNewLLM(llmToEdit)
            }
        } else {
            Messages.showInfoMessage(
                "GitHub Copilot models cannot be edited.",
                "Read-only Model"
            )
        }
    }

    /**
     * Delete LLM at specific row in the table
     */
    fun deleteLLMAtRow(table: JTable, row: Int) {
        val modelName = table.getValueAt(row, 0) as? String ?: return

        // Only allow deleting custom LLMs
        if (isCustomLLM(modelName)) {
            val result = Messages.showYesNoDialog(
                "Are you sure you want to delete the LLM '$modelName'?",
                "Delete LLM",
                Messages.getQuestionIcon()
            )

            if (result == Messages.YES) {
                try {
                    // Get existing LLMs
                    val existingLlms = LlmConfig.load().toMutableList()

                    // Remove the LLM
                    existingLlms.removeIf { it.name == modelName }

                    // Update settings
                    val json = Json { prettyPrint = true }
                    settings.customLlms = json.encodeToString(
                        ListSerializer(LlmConfig.serializer()),
                        existingLlms
                    )

                    // Mark as modified
                    onModified()
                } catch (e: Exception) {
                    Messages.showErrorDialog("Error deleting LLM: ${e.message}", "Error")
                }
            }
        } else {
            Messages.showInfoMessage(
                "GitHub Copilot models cannot be deleted.",
                "Read-only Model"
            )
        }
    }

    /**
     * Update all model dropdowns with available models
     */
    fun updateAllDropdowns(
        defaultModelDropdown: ComboBox<ModelItem>,
        planLLMDropdown: ComboBox<ModelItem>,
        actLLMDropdown: ComboBox<ModelItem>,
        completionLLMDropdown: ComboBox<ModelItem>,
        embeddingLLMDropdown: ComboBox<ModelItem>,
        fastApplyLLMDropdown: ComboBox<ModelItem>
    ) {
        // Clear all dropdowns
        defaultModelDropdown.removeAllItems()
        planLLMDropdown.removeAllItems()
        actLLMDropdown.removeAllItems()
        completionLLMDropdown.removeAllItems()
        embeddingLLMDropdown.removeAllItems()
        fastApplyLLMDropdown.removeAllItems()

        val manager = service<GithubCopilotManager>()
        val githubModels = manager.getSupportedModels(forceRefresh = false)
        val userModels = LlmConfig.load()

        // Add GitHub Copilot models
        if (manager.isInitialized()) {
            githubModels?.forEach { model ->
                val modelItem = ModelItem("Github: ${model.id}", model.id, false)
                if (model.isEmbedding) {
                    embeddingLLMDropdown.addItem(modelItem)
                } else {
                    defaultModelDropdown.addItem(modelItem)
                    planLLMDropdown.addItem(modelItem)
                    actLLMDropdown.addItem(modelItem)
                    completionLLMDropdown.addItem(modelItem)
                    fastApplyLLMDropdown.addItem(modelItem)
                }
            }
        } else {
            // Initialize GitHub Copilot in background and update UI when ready
            AutoDevAppScope.workerScope().launch {
                try {
                    GithubCopilotManager.getInstance().initialize()
                    // After initialization, update the UI on the EDT
                    SwingUtilities.invokeLater {
                        updateAllDropdowns(
                            defaultModelDropdown,
                            planLLMDropdown,
                            actLLMDropdown,
                            completionLLMDropdown,
                            embeddingLLMDropdown,
                            fastApplyLLMDropdown
                        )
                    }
                } catch (e: Exception) {
                    // Silently handle initialization failures
                    // GitHub Copilot might not be available
                }
            }
        }

        // Add custom models
        userModels.forEach { llm ->
            val modelItem = ModelItem(llm.name, llm.name, true)
            when (llm.modelType) {
                cc.unitmesh.devti.llm2.model.ModelType.Embedding -> embeddingLLMDropdown.addItem(modelItem)
                else -> {
                    defaultModelDropdown.addItem(modelItem)
                    planLLMDropdown.addItem(modelItem)
                    actLLMDropdown.addItem(modelItem)
                    completionLLMDropdown.addItem(modelItem)
                    fastApplyLLMDropdown.addItem(modelItem)
                }
            }
        }
    }

    /**
     * Set selected items in dropdowns based on settings
     */
    fun setSelectedModels(
        settings: AutoDevSettingsState,
        defaultModelDropdown: ComboBox<ModelItem>,
        planLLMDropdown: ComboBox<ModelItem>,
        actLLMDropdown: ComboBox<ModelItem>,
        completionLLMDropdown: ComboBox<ModelItem>,
        embeddingLLMDropdown: ComboBox<ModelItem>,
        fastApplyLLMDropdown: ComboBox<ModelItem>
    ) {
        // Helper function to find and select a model by ID
        fun ComboBox<ModelItem>.selectModelById(modelId: String) {
            if (modelId.isEmpty()) return

            for (i in 0 until this.itemCount) {
                val item = this.getItemAt(i)
                if (item.modelId == modelId) {
                    this.selectedItem = item
                    break
                }
            }
        }

        // Set default model
        defaultModelDropdown.selectModelById(settings.defaultModelId)

        // Set category-specific models
        planLLMDropdown.selectModelById(settings.selectedPlanModelId)
        actLLMDropdown.selectModelById(settings.selectedActModelId)
        completionLLMDropdown.selectModelById(settings.selectedCompletionModelId)
        embeddingLLMDropdown.selectModelById(settings.selectedEmbeddingModelId)
        fastApplyLLMDropdown.selectModelById(settings.selectedFastApplyModelId)
    }

    /**
     * Update the table with all LLMs
     */
    fun updateLLMTable(tableModel: DefaultTableModel) {
        // Clear the table
        tableModel.setRowCount(0)

        val manager = service<GithubCopilotManager>()
        val githubModels = manager.getSupportedModels(forceRefresh = false)
        val userModels = LlmConfig.load()

        // Add GitHub Copilot models (read-only)
        githubModels?.forEach { model ->
            tableModel.addRow(
                arrayOf(
                    "Github: ${model.id}", // Name
                    model.id,             // Model
                    "true",               // Streaming (GitHub models use streaming by default)
                    "0.1",                // Temperature (GitHub models default temperature)
                    ""                    // Delete (empty for read-only models)
                )
            )
        }

        // Add custom LLMs (editable)
        userModels.forEach { llm ->
            val modelValue = llm.customRequest.body["model"]?.let {
                when (it) {
                    is JsonPrimitive -> it.content
                    else -> it.toString().removeSurrounding("\"")
                }
            } ?: ""

            val temperatureValue = llm.customRequest.body["temperature"]?.let {
                when (it) {
                    is JsonPrimitive -> it.content
                    else -> it.toString()
                }
            } ?: "0.0"

            tableModel.addRow(
                arrayOf(
                    llm.name,             // Name
                    modelValue,           // Model
                    llm.customRequest.stream.toString(), // Streaming
                    temperatureValue,     // Temperature
                    "Delete"              // Delete button placeholder
                )
            )
        }
    }

    /**
     * Test GitHub Copilot connection
     */
    fun testGitHubCopilotConnection() {
        val scope = CoroutineScope(CoroutineName("testGitHubCopilot"))
        scope.launch {
            try {
                val provider = LLMProvider2.GithubCopilot(modelName = "gpt-4")
                val response = provider.request(Message("user", "Hello, this is a test message."))
                var responseText = ""
                response.collectLatest {
                    responseText += it.chatMessage.content
                }
                Messages.showInfoMessage("GitHub Copilot connection successful!", "Test Result")
            } catch (e: Exception) {
                Messages.showErrorDialog("GitHub Copilot connection failed: ${e.message}", "Test Failed")
            }
        }
    }

    /**
     * Test custom LLM connection
     */
    fun testCustomLLM(llm: LlmConfig) {
        val scope = CoroutineScope(CoroutineName("testCustomLLM"))
        scope.launch {
            try {
                val provider = LLMProvider2.invoke(
                    requestUrl = llm.url,
                    authorizationKey = llm.auth.token,
                    responseResolver = llm.getResponseFormatByStream(),
                    requestCustomize = llm.toLegacyRequestFormat()
                )

                val response = provider.request(Message("user", "Hello, this is a test message."))
                var responseText = ""
                response.collectLatest {
                    responseText += it.chatMessage.content
                }
                Messages.showInfoMessage("Connection to ${llm.name} successful!", "Test Result")
            } catch (e: Exception) {
                Messages.showErrorDialog("Connection to ${llm.name} failed: ${e.message}", "Test Failed")
            }
        }
    }

    /**
     * Refresh GitHub Copilot models
     */
    fun refreshGitHubCopilotModels(
        defaultModelDropdown: ComboBox<ModelItem>,
        planLLMDropdown: ComboBox<ModelItem>,
        actLLMDropdown: ComboBox<ModelItem>,
        completionLLMDropdown: ComboBox<ModelItem>,
        embeddingLLMDropdown: ComboBox<ModelItem>,
        fastApplyLLMDropdown: ComboBox<ModelItem>,
        tableModel: DefaultTableModel
    ) {
        val manager = service<GithubCopilotManager>()

        AutoDevAppScope.workerScope().launch {
            try {
                // Force refresh GitHub Copilot models
                manager.getSupportedModels(forceRefresh = true)

                // Update UI on EDT
                SwingUtilities.invokeLater {
                    updateAllDropdowns(
                        defaultModelDropdown,
                        planLLMDropdown,
                        actLLMDropdown,
                        completionLLMDropdown,
                        embeddingLLMDropdown,
                        fastApplyLLMDropdown
                    )
                    updateLLMTable(tableModel)
                    Messages.showInfoMessage(
                        "GitHub Copilot models refreshed successfully!",
                        "Refresh Complete"
                    )
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    Messages.showErrorDialog(
                        "Failed to refresh GitHub Copilot models: ${e.message}",
                        "Refresh Failed"
                    )
                }
            }
        }
    }

    companion object {
        fun getInstance(): LLMModelManager {
            val project = ProjectManager.getInstance().openProjects.firstOrNull()
            val settings = AutoDevSettingsState.getInstance()
            return LLMModelManager(project, settings, {})
        }
    }
}
