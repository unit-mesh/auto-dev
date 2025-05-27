package cc.unitmesh.devti.settings

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.custom.schema.AUTODEV_CUSTOM_LLM_FILE
import cc.unitmesh.devti.llm2.GithubCopilotManager
import cc.unitmesh.devti.llm2.LLMProvider2
import cc.unitmesh.devti.llm2.model.Auth
import cc.unitmesh.devti.llm2.model.LlmConfig
import cc.unitmesh.devti.llm2.model.ModelType
import cc.unitmesh.devti.llms.custom.Message
import cc.unitmesh.devti.provider.local.JsonTextProvider
import cc.unitmesh.devti.settings.locale.HUMAN_LANGUAGES
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.jBLabel
import cc.unitmesh.devti.util.AutoDevAppScope
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.table.DefaultTableModel

class SimplifiedLLMSettingComponent(private val settings: AutoDevSettingsState) {

    // Basic settings
    private val languageParam by LLMParam.creating({ LanguageChangedCallback.language = it }) {
        ComboBox(settings.language, HUMAN_LANGUAGES.entries.map { it.display })
    }
    private val delaySecondsParam by LLMParam.creating { Editable(settings.delaySeconds) }
    private val maxTokenLengthParam by LLMParam.creating { Editable(settings.maxTokenLength) }

    // Default model configuration
    private val defaultModelDropdown = ComboBox<ModelItem>()
    private val useDefaultForAllCheckbox = JBCheckBox("Use default model for all categories", settings.useDefaultForAllCategories)

    // Category-specific model dropdowns (only shown when not using default for all)
    private val planLLMDropdown = ComboBox<ModelItem>()
    private val actLLMDropdown = ComboBox<ModelItem>()
    private val completionLLMDropdown = ComboBox<ModelItem>()
    private val embeddingLLMDropdown = ComboBox<ModelItem>()
    private val fastApplyLLMDropdown = ComboBox<ModelItem>()

    // Category section panel for dynamic visibility
    private var categoryPanel: JPanel? = null

    // Model management
    private val llmTableModel = DefaultTableModel(arrayOf("Name", "Type", "URL", "Status", "Actions"), 0)
    private val llmTable = JTable(llmTableModel)

    private val project = ProjectManager.getInstance().openProjects.firstOrNull()

    // Custom model item class to store both display name and model ID
    private data class ModelItem(val displayName: String, val modelId: String, val isCustom: Boolean = false) {
        override fun toString(): String = displayName
    }

    private val formBuilder: FormBuilder = FormBuilder.createFormBuilder()
    val panel: JPanel get() = formBuilder.panel

    init {
        setupEventListeners()

        // Try to initialize GitHub Copilot early
        initializeGitHubCopilot()

        applySettings(settings)
        LanguageChangedCallback.language = AutoDevSettingsState.getInstance().language
    }

    private fun initializeGitHubCopilot() {
        val manager = service<GithubCopilotManager>()
        if (!manager.isInitialized()) {
            AutoDevAppScope.workerScope().launch {
                try {
                    manager.initialize()
                    // After initialization, update the UI on the EDT
                    SwingUtilities.invokeLater {
                        updateAllDropdowns()
                        updateLLMTable()
                    }
                } catch (e: Exception) {
                    // Silently handle initialization failures
                    // GitHub Copilot might not be available
                }
            }
        }
    }

    private fun setupEventListeners() {
        // When checkbox changes, show/hide category dropdowns
        useDefaultForAllCheckbox.addActionListener {
            updateCategoryDropdownsVisibility()
        }
    }

    private fun updateCategoryDropdownsVisibility() {
        // Toggle visibility of category panel without rebuilding entire UI
        categoryPanel?.isVisible = !useDefaultForAllCheckbox.isSelected

        // Trigger layout update
        panel.revalidate()
        panel.repaint()
    }

    private fun createCategoryPanel(): JPanel {
        val categoryFormBuilder = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Category-Specific Models"), JPanel(), 1, false)
            .addLabeledComponent(JBLabel("Plan:"), planLLMDropdown, 1, false)
            .addLabeledComponent(JBLabel("Act:"), actLLMDropdown, 1, false)
            .addLabeledComponent(JBLabel("Completion:"), completionLLMDropdown, 1, false)
            .addLabeledComponent(JBLabel("Embedding:"), embeddingLLMDropdown, 1, false)
            .addLabeledComponent(JBLabel("FastApply:"), fastApplyLLMDropdown, 1, false)
            .addSeparator()

        return categoryFormBuilder.panel
    }

    // Create a new LLM configuration dialog with simplified interface
    private fun createNewLLM(existingLlm: LlmConfig? = null) {
        val dialog = object : DialogWrapper(project, true) {
            private val nameField = JBTextField()
            private val descriptionField = JBTextField()
            private val urlField = JBTextField()
            private val tokenField = JBTextField()
            private val modelTypeComboBox = JComboBox(ModelType.values())
            private val testResultLabel = JBLabel("")

            init {
                title = if (existingLlm == null) "Add New LLM" else "Edit LLM"

                // Initialize fields with existing values if editing
                if (existingLlm != null) {
                    nameField.text = existingLlm.name
                    descriptionField.text = existingLlm.description
                    urlField.text = existingLlm.url
                    tokenField.text = existingLlm.auth.token
                    modelTypeComboBox.selectedItem = existingLlm.modelType
                }

                init()
            }

            override fun createCenterPanel(): JPanel {
                val panel = JPanel(BorderLayout())
                panel.preferredSize = Dimension(500, 400)

                val formBuilder = FormBuilder.createFormBuilder()
                    .addLabeledComponent(JBLabel("Name:"), nameField)
                    .addLabeledComponent(JBLabel("Description:"), descriptionField)
                    .addLabeledComponent(JBLabel("URL:"), urlField)
                    .addLabeledComponent(JBLabel("Token:"), tokenField)
                    .addLabeledComponent(JBLabel("Model Type:"), modelTypeComboBox)
                    .addSeparator()

                // Add test button
                val testButton = JButton("Test Connection")
                testButton.addActionListener { testConnection() }
                formBuilder.addComponent(testButton)
                formBuilder.addComponent(testResultLabel)

                panel.add(formBuilder.panel, BorderLayout.CENTER)
                return panel
            }

            private fun testConnection() {
                if (nameField.text.isBlank() || urlField.text.isBlank() || tokenField.text.isBlank()) {
                    testResultLabel.text = "Name, URL, and Token are required"
                    testResultLabel.foreground = JBColor.RED
                    return
                }

                testResultLabel.text = "Testing connection..."
                testResultLabel.foreground = JBColor.BLUE

                val scope = CoroutineScope(CoroutineName("testConnection"))
                scope.launch {
                    try {
                        // Create a temporary LLM config for testing
                        val testConfig = LlmConfig(
                            name = nameField.text,
                            description = descriptionField.text,
                            url = urlField.text,
                            auth = Auth(type = "Bearer", token = tokenField.text),
                            requestFormat = """{ "customFields": {"model": "${nameField.text}", "temperature": 0.0, "stream": true} }""",
                            responseFormat = "\$.choices[0].delta.content",
                            modelType = modelTypeComboBox.selectedItem as ModelType
                        )

                        // Create a provider with the test config
                        val provider = LLMProvider2.invoke(
                            requestUrl = testConfig.url,
                            authorizationKey = testConfig.auth.token,
                            responseResolver = testConfig.responseFormat,
                            requestCustomize = testConfig.requestFormat
                        )

                        // Send a test message
                        val response = provider.request(Message("user", "Hello, this is a test message."))
                        var responseText = ""
                        response.collectLatest {
                            responseText += it.chatMessage.content
                        }

                        testResultLabel.text = "Connection successful!"
                        testResultLabel.foreground = JBColor.GREEN
                    } catch (e: Exception) {
                        e.printStackTrace()
                        testResultLabel.text = "Connection failed: ${e.message}"
                        testResultLabel.foreground = JBColor.RED
                    }
                }
            }

            override fun doOKAction() {
                if (nameField.text.isBlank() || urlField.text.isBlank() || tokenField.text.isBlank()) {
                    Messages.showErrorDialog("Name, URL, and Token are required", "Validation Error")
                    return
                }

                try {
                    // Get existing LLMs
                    val existingLlms = try {
                        LlmConfig.load().toMutableList()
                    } catch (e: Exception) {
                        mutableListOf()
                    }

                    // Remove existing LLM if editing
                    if (existingLlm != null) {
                        existingLlms.removeIf { it.name == existingLlm.name }
                    }

                    // Create new LLM config
                    val newLlm = LlmConfig(
                        name = nameField.text,
                        description = descriptionField.text,
                        url = urlField.text,
                        auth = Auth(type = "Bearer", token = tokenField.text),
                        requestFormat = """{ "customFields": {"model": "${nameField.text}", "temperature": 0.0, "stream": true} }""",
                        responseFormat = "\$.choices[0].delta.content",
                        modelType = modelTypeComboBox.selectedItem as ModelType
                    )

                    // Add to list and update settings
                    existingLlms.add(newLlm)
                    val json = Json { prettyPrint = true }
                    settings.customLlms = json.encodeToString(
                        kotlinx.serialization.builtins.ListSerializer(LlmConfig.serializer()),
                        existingLlms
                    )

                    // Update UI
                    updateAllDropdowns()
                    updateLLMTable()

                    super.doOKAction()
                } catch (e: Exception) {
                    Messages.showErrorDialog("Error saving LLM: ${e.message}", "Error")
                }
            }
        }

        dialog.show()
    }

    // Update all model dropdowns with available models
    private fun updateAllDropdowns() {
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
                val modelItem = ModelItem(model.name, model.id, false)
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
                        updateAllDropdowns()
                        updateLLMTable()
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
                ModelType.Embedding -> embeddingLLMDropdown.addItem(modelItem)
                else -> {
                    defaultModelDropdown.addItem(modelItem)
                    planLLMDropdown.addItem(modelItem)
                    actLLMDropdown.addItem(modelItem)
                    completionLLMDropdown.addItem(modelItem)
                    fastApplyLLMDropdown.addItem(modelItem)
                }
            }
        }

        // Set selected items based on settings
        setSelectedModels()
    }

    // Set selected items in dropdowns based on settings
    private fun setSelectedModels() {
        // Helper function to find and select a model by ID
        fun JComboBox<ModelItem>.selectModelById(modelId: String) {
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

    // Update the table with all LLMs
    private fun updateLLMTable() {
        // Clear the table
        llmTableModel.rowCount = 0

        val manager = service<GithubCopilotManager>()
        val githubModels = manager.getSupportedModels(forceRefresh = false)
        val userModels = LlmConfig.load()

        // Add GitHub Copilot models (read-only)
        githubModels?.forEach { model ->
            llmTableModel.addRow(
                arrayOf(
                    model.name,
                    if (model.isEmbedding) "Embedding" else "Chat",
                    "GitHub Copilot",
                    "Available",
                    createReadOnlyActionsPanel()
                )
            )
        }

        // Add custom LLMs (editable)
        userModels.forEach { llm ->
            llmTableModel.addRow(
                arrayOf(
                    llm.name,
                    llm.modelType.toString(),
                    llm.url,
                    "Custom",
                    createEditableActionsPanel(llm)
                )
            )
        }
    }

    private fun createReadOnlyActionsPanel(): JPanel {
        val panel = JPanel()
        val testButton = JButton("Test")
        testButton.addActionListener {
            testGitHubCopilotConnection()
        }
        panel.add(testButton)
        return panel
    }

    private fun createEditableActionsPanel(llm: LlmConfig): JPanel {
        val panel = JPanel()

        val testButton = JButton("Test")
        testButton.addActionListener { testCustomLLM(llm) }

        val editButton = JButton("Edit")
        editButton.addActionListener { createNewLLM(llm) }

        val deleteButton = JButton("Delete")
        deleteButton.addActionListener { deleteLLM(llm) }

        panel.add(testButton)
        panel.add(editButton)
        panel.add(deleteButton)
        return panel
    }

    private fun testGitHubCopilotConnection() {
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

    private fun testCustomLLM(llm: LlmConfig) {
        val scope = CoroutineScope(CoroutineName("testCustomLLM"))
        scope.launch {
            try {
                val provider = LLMProvider2.invoke(
                    requestUrl = llm.url,
                    authorizationKey = llm.auth.token,
                    responseResolver = llm.responseFormat,
                    requestCustomize = llm.requestFormat
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

    // Delete an LLM
    private fun deleteLLM(llm: LlmConfig) {
        val result = Messages.showYesNoDialog(
            "Are you sure you want to delete the LLM '${llm.name}'?",
            "Delete LLM",
            Messages.getQuestionIcon()
        )

        if (result == Messages.YES) {
            try {
                // Get existing LLMs
                val existingLlms = LlmConfig.load().toMutableList()

                // Remove the LLM
                existingLlms.removeIf { it.name == llm.name }

                // Update settings
                val json = Json { prettyPrint = true }
                settings.customLlms = json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(LlmConfig.serializer()),
                    existingLlms
                )

                // Update UI
                updateAllDropdowns()
                updateLLMTable()
            } catch (e: Exception) {
                Messages.showErrorDialog("Error deleting LLM: ${e.message}", "Error")
            }
        }
    }

    private fun refreshGitHubCopilotModels() {
        val manager = service<GithubCopilotManager>()

        AutoDevAppScope.workerScope().launch {
            try {
                // Force refresh GitHub Copilot models
                manager.getSupportedModels(forceRefresh = true)

                // Update UI on EDT
                SwingUtilities.invokeLater {
                    updateAllDropdowns()
                    updateLLMTable()
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

    // Build the main settings panel
    fun applySettings(settings: AutoDevSettingsState, updateParams: Boolean = false) {
        panel.removeAll()

        // Update dropdowns and table
        updateAllDropdowns()
        updateLLMTable()

        // Create add LLM button
        val addLLMButton = JButton("Add New LLM")
        addLLMButton.addActionListener { createNewLLM() }

        // Create refresh button for GitHub Copilot models
        val refreshButton = JButton("Refresh GitHub Copilot Models")
        refreshButton.addActionListener { refreshGitHubCopilotModels() }

        // Create button panel
        val buttonPanel = JPanel()
        buttonPanel.add(addLLMButton)
        buttonPanel.add(refreshButton)

        // Create category panel separately for dynamic visibility
        categoryPanel = createCategoryPanel()

        formBuilder
            // Basic Settings Section
            .addLabeledComponent(JBLabel("Basic Settings"), JPanel(), 1, false)
            .addLLMParam(languageParam)
            .addLLMParam(maxTokenLengthParam)
            .addLLMParam(delaySecondsParam)
            .addSeparator()

            // Default Model Configuration Section
            .addLabeledComponent(JBLabel("Default Model Configuration"), JPanel(), 1, false)
            .addLabeledComponent(JBLabel("Default Model:"), defaultModelDropdown, 1, false)
            .addComponent(useDefaultForAllCheckbox)
            .addSeparator()

        // Add category panel (visibility controlled dynamically)
        formBuilder.addComponent(categoryPanel!!)

        formBuilder
            // Model Management Section
            .addLabeledComponent(JBLabel("Model Management"), JPanel(), 1, false)
            .addComponent(buttonPanel)
            .addComponentFillVertically(JScrollPane(llmTable), 0)
            .addComponentFillVertically(JPanel(), 0)

        // Set initial visibility
        updateCategoryDropdownsVisibility()

        panel.invalidate()
        panel.repaint()
    }

    // Export settings to AutoDevSettingsState
    fun exportSettings(destination: AutoDevSettingsState) {
        destination.apply {
            maxTokenLength = maxTokenLengthParam.value
            language = languageParam.value
            delaySeconds = delaySecondsParam.value

            // Export default model configuration
            defaultModelId = (defaultModelDropdown.selectedItem as? ModelItem)?.modelId ?: ""
            useDefaultForAllCategories = useDefaultForAllCheckbox.isSelected

            // Export category-specific model selections
            selectedPlanModelId = (planLLMDropdown.selectedItem as? ModelItem)?.modelId ?: ""
            selectedActModelId = (actLLMDropdown.selectedItem as? ModelItem)?.modelId ?: ""
            selectedCompletionModelId = (completionLLMDropdown.selectedItem as? ModelItem)?.modelId ?: ""
            selectedEmbeddingModelId = (embeddingLLMDropdown.selectedItem as? ModelItem)?.modelId ?: ""
            selectedFastApplyModelId = (fastApplyLLMDropdown.selectedItem as? ModelItem)?.modelId ?: ""

            // Custom LLMs are already saved in the customLlms field when created/edited
        }
    }

    // Check if settings have been modified
    fun isModified(settings: AutoDevSettingsState): Boolean {
        return settings.maxTokenLength != maxTokenLengthParam.value ||
                settings.language != languageParam.value ||
                settings.delaySeconds != delaySecondsParam.value ||
                settings.defaultModelId != ((defaultModelDropdown.selectedItem as? ModelItem)?.modelId ?: "") ||
                settings.useDefaultForAllCategories != useDefaultForAllCheckbox.isSelected ||
                settings.selectedPlanModelId != ((planLLMDropdown.selectedItem as? ModelItem)?.modelId ?: "") ||
                settings.selectedActModelId != ((actLLMDropdown.selectedItem as? ModelItem)?.modelId ?: "") ||
                settings.selectedCompletionModelId != ((completionLLMDropdown.selectedItem as? ModelItem)?.modelId ?: "") ||
                settings.selectedEmbeddingModelId != ((embeddingLLMDropdown.selectedItem as? ModelItem)?.modelId ?: "") ||
                settings.selectedFastApplyModelId != ((fastApplyLLMDropdown.selectedItem as? ModelItem)?.modelId ?: "")
    }

    // Helper extension function for FormBuilder
    private fun FormBuilder.addLLMParam(llmParam: LLMParam): FormBuilder = apply {
        llmParam.addToFormBuilder(this)
    }

    private fun LLMParam.addToFormBuilder(formBuilder: FormBuilder) {
        when (this.type) {
            LLMParam.ParamType.Text -> {
                formBuilder.addLabeledComponent(jBLabel(this.label), ReactiveTextField(this) {
                    this.isEnabled = it.isEditable
                }, 1, false)
            }
            LLMParam.ParamType.ComboBox -> {
                formBuilder.addLabeledComponent(jBLabel(this.label), ReactiveComboBox(this), 1, false)
            }
            else -> {
                formBuilder.addSeparator()
            }
        }
    }
}
