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
import com.intellij.lang.Language
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.FormBuilder
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

class LLMSettingComponent(private val settings: AutoDevSettingsState) {
    private val languageParam by LLMParam.creating({ LanguageChangedCallback.language = it }) {
        ComboBox(settings.language, HUMAN_LANGUAGES.entries.map { it.display })
    }

    private val delaySecondsParam by LLMParam.creating { Editable(settings.delaySeconds) }
    private val maxTokenLengthParam by LLMParam.creating { Editable(settings.maxTokenLength) }
    private val customModelParam: LLMParam by LLMParam.creating { Editable(settings.customModel) }
    private val customOpenAIHostParam: LLMParam by LLMParam.creating { Editable(settings.customOpenAiHost) }

    private val customEngineServerParam by LLMParam.creating { Editable(settings.customEngineServer) }
    private val customEngineTokenParam by LLMParam.creating { Password(settings.customEngineToken) }

    private val customEngineResponseFormatParam by LLMParam.creating { JsonPathEditable(settings.customEngineResponseFormat) }
    private val customEngineRequestBodyFormatParam by LLMParam.creating { JsonEditable(settings.customEngineRequestFormat) }

    val project = ProjectManager.getInstance().openProjects.firstOrNull()
    private val customLlmParam: EditorTextField by lazy {
        JsonTextProvider.create(
            project,
            settings.customLlms,
            AutoDevBundle.messageWithLanguageFromLLMSetting("autodev.custom.llms.placeholder"),
            AUTODEV_CUSTOM_LLM_FILE
        ).apply {
            LanguageChangedCallback.placeholder("autodev.custom.llms.placeholder", this, 1)
        }
    }

    // Custom model item class to store both display name and model ID
    private data class ModelItem(val displayName: String, val modelId: String) {
        override fun toString(): String = displayName
    }

    // Dropdowns for categorized LLMs
    private val planLLMDropdown = ComboBox<ModelItem>()
    private val actLLMDropdown = ComboBox<ModelItem>()
    private val completionLLMDropdown = ComboBox<ModelItem>()
    private val embeddingLLMDropdown = ComboBox<ModelItem>()
    private val fastApplyLLMDropdown = ComboBox<ModelItem>()

    // Table for displaying all LLMs
    private val llmTableModel = DefaultTableModel(arrayOf("Name", "Type", "URL", "Actions"), 0)
    private val llmTable = JTable(llmTableModel)

    private val currentLLMParams: List<LLMParam>
        get() {
            return listOf(
                customEngineServerParam,
                customEngineTokenParam,
                customEngineResponseFormatParam,
                customEngineRequestBodyFormatParam,
            )
        }

    // Create a new LLM configuration dialog
    private fun createNewLLM(existingLlm: LlmConfig? = null) {
        val dialog = object : DialogWrapper(project, true) {
            private val nameField = JBTextField()
            private val descriptionField = JBTextField()
            private val urlField = JBTextField()
            private val tokenField = JBTextField()
            private val modelTypeComboBox = JComboBox(ModelType.values())
            private val temperatureField = JBTextField("0.0")
            private val maxTokensField = JBTextField("2048")
            private val topPField = JBTextField("1.0")
            private val frequencyPenaltyField = JBTextField("0.0")
            private val presencePenaltyField = JBTextField("0.0")
            private val requestFormatField =
                JBTextField("""{ "customFields": {"model": "", "temperature": 0.0, "stream": true} }""")
            private val responseFormatField = JBTextField("\$.choices[0].delta.content")
            private val testResultLabel = JBLabel("")

            init {
                title = if (existingLlm == null) "Create New LLM" else "Edit LLM"

                // Initialize fields with existing values if editing
                if (existingLlm != null) {
                    nameField.text = existingLlm.name
                    descriptionField.text = existingLlm.description
                    urlField.text = existingLlm.url
                    tokenField.text = existingLlm.auth.token
                    modelTypeComboBox.selectedItem = existingLlm.modelType
                    requestFormatField.text = existingLlm.requestFormat
                    responseFormatField.text = existingLlm.responseFormat

                    // Use default values for parameters
                    temperatureField.text = "0.0"
                    maxTokensField.text = "128000"
                    topPField.text = "1.0"
                    frequencyPenaltyField.text = "0.0"
                    presencePenaltyField.text = "0.0"
                }

                init()
            }

            override fun createCenterPanel(): JPanel {
                val panel = JPanel(BorderLayout())
                panel.preferredSize = Dimension(600, 500)

                val formBuilder = FormBuilder.createFormBuilder()
                    .addLabeledComponent(JBLabel("Name:"), nameField)
                    .addLabeledComponent(JBLabel("Description:"), descriptionField)
                    .addLabeledComponent(JBLabel("URL:"), urlField)
                    .addLabeledComponent(JBLabel("Token:"), tokenField)
                    .addLabeledComponent(JBLabel("Model Type:"), modelTypeComboBox)
                    .addLabeledComponent(JBLabel("Temperature:"), temperatureField)
                    .addLabeledComponent(JBLabel("Max Tokens:"), maxTokensField)
                    .addLabeledComponent(JBLabel("Top P:"), topPField)
                    .addLabeledComponent(JBLabel("Frequency Penalty:"), frequencyPenaltyField)
                    .addLabeledComponent(JBLabel("Presence Penalty:"), presencePenaltyField)
                    .addLabeledComponent(JBLabel("Request Format:"), requestFormatField)
                    .addLabeledComponent(JBLabel("Response Format:"), responseFormatField)
                    .addSeparator()

                // Add test button
                val testButton = JButton("Test Connection")
                testButton.addActionListener {
                    testConnection()
                }
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
                            description = nameField.text,
                            url = urlField.text,
                            auth = Auth(
                                type = "Bearer",
                                token = tokenField.text
                            ),
                            requestFormat = requestFormatField.text,
                            responseFormat = responseFormatField.text,
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

                        testResultLabel.text = "Connection successful: $responseText"
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

                    // Create request format with all parameters
                    val requestFormat = """
                        {
                            "customFields": {
                                "model": "${nameField.text}",
                                "temperature": ${temperatureField.text},
                                "max_tokens": ${maxTokensField.text},
                                "top_p": ${topPField.text},
                                "frequency_penalty": ${frequencyPenaltyField.text},
                                "presence_penalty": ${presencePenaltyField.text},
                                "stream": true
                            }
                        }
                    """.trimIndent()

                    // Create new LLM config
                    val newLlm = LlmConfig(
                        name = nameField.text,
                        description = descriptionField.text,
                        url = urlField.text,
                        auth = Auth(
                            type = "Bearer",
                            token = tokenField.text
                        ),
                        requestFormat = requestFormatField.text.ifBlank { requestFormat },
                        responseFormat = responseFormatField.text.ifBlank { "\$.choices[0].delta.content" },
                        modelType = modelTypeComboBox.selectedItem as ModelType
                    )

                    // Add to list and update settings
                    existingLlms.add(newLlm)
                    val json = Json { prettyPrint = true }
                    customLlmParam.text = json.encodeToString(
                        kotlinx.serialization.builtins.ListSerializer(LlmConfig.serializer()),
                        existingLlms
                    )

                    // Update dropdowns
                    updateLLMDropdowns()

                    super.doOKAction()
                } catch (e: Exception) {
                    Messages.showErrorDialog("Error creating LLM: ${e.message}", "Error")
                }
            }
        }

        dialog.show()
    }

    // Update the LLM dropdowns with the current LLMs
    private fun updateLLMDropdowns() {
        // Clear all dropdowns
        planLLMDropdown.removeAllItems()
        actLLMDropdown.removeAllItems()
        completionLLMDropdown.removeAllItems()
        embeddingLLMDropdown.removeAllItems()
        fastApplyLLMDropdown.removeAllItems()

        val manager = service<GithubCopilotManager>()
        val githubModels = manager.getSupportedModels(forceRefresh = false)
        val userModels = LlmConfig.load()

        if (manager.isInitialized()) {
            githubModels?.groupBy { it.isEmbedding }
                ?.forEach { (isEmbedding, models) ->
                    val modelItems = models.map { ModelItem(it.id, it.id) }
                    if (isEmbedding) {
                        modelItems.forEach { modelItem ->
                            embeddingLLMDropdown.addItem(modelItem)
                        }
                    } else {
                        modelItems.forEach { modelItem ->
                            planLLMDropdown.addItem(modelItem)
                            actLLMDropdown.addItem(modelItem)
                            completionLLMDropdown.addItem(modelItem)
                            fastApplyLLMDropdown.addItem(modelItem)
                        }
                    }
                }
        } else {
            // all dropdowns show loading item and be disabled
            AutoDevAppScope.workerScope().launch {
                GithubCopilotManager.getInstance().initialize()
            }
        }

        // TODO migrate LlmConfigs

    }

    // Helper method to update a dropdown with models, removing loading items first
    private fun updateDropdownWithModels(dropdown: JComboBox<ModelItem>, models: List<ModelItem>) {
        // Remove loading items
        removeLoadingItems(dropdown)

        // Add models
        models.forEach { dropdown.addItem(it) }
    }

    // Helper method to remove loading items from a dropdown
    private fun removeLoadingItems(dropdown: JComboBox<ModelItem>) {
        for (i in dropdown.itemCount - 1 downTo 0) {
            val item = dropdown.getItemAt(i)
            if (item.displayName == "loading") {
                dropdown.removeItemAt(i)
            }
        }
    }

    // Update the table with all LLMs
    private fun updateLLMTable(llms: List<LlmConfig>) {
        // Clear the table
        llmTableModel.rowCount = 0

        // Add custom LLMs to the table (editable)
        llms.forEach { llm ->
            val editButton = JButton("Edit")
            editButton.addActionListener {
                editLLM(llm)
            }

            val deleteButton = JButton("Delete")
            deleteButton.addActionListener {
                deleteLLM(llm)
            }

            val actionsPanel = JPanel()
            actionsPanel.add(editButton)
            actionsPanel.add(deleteButton)

            llmTableModel.addRow(
                arrayOf(
                    llm.name,
                    llm.modelType.toString(),
                    llm.url,
                    actionsPanel
                )
            )
        }
    }

    // Edit an existing LLM
    private fun editLLM(llm: LlmConfig) {
        createNewLLM(llm)
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
                customLlmParam.text = json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(LlmConfig.serializer()),
                    existingLlms
                )

                // Update dropdowns
                updateLLMDropdowns()
            } catch (e: Exception) {
                Messages.showErrorDialog("Error deleting LLM: ${e.message}", "Error")
            }
        }
    }

    private fun FormBuilder.addLLMParams(llmParams: List<LLMParam>): FormBuilder = apply {
        llmParams.forEach { addLLMParam(it) }
    }

    private fun FormBuilder.addLLMParam(llmParam: LLMParam): FormBuilder = apply {
        llmParam.addToFormBuilder(this)
    }

    private fun LLMParam.addToFormBuilder(formBuilder: FormBuilder) {
        when (this.type) {
            LLMParam.ParamType.Password -> {
                formBuilder.addLabeledComponent(jBLabel(this.label), ReactivePasswordField(this) {
                    this.text = it.value
                    this.isEnabled = it.isEditable
                }, 1, false)
            }

            LLMParam.ParamType.Text -> {
                formBuilder.addLabeledComponent(jBLabel(this.label), ReactiveTextField(this) {
                    this.isEnabled = it.isEditable
                }, 1, false)
            }

            LLMParam.ParamType.JsonText -> {
                formBuilder.addLabeledComponent(
                    jBLabel(this.label), cc.unitmesh.devti.provider.local.JsonLanguageField(
                        project, this.value, AutoDevBundle.messageWithLanguageFromLLMSetting(this.label), null, true
                    ).apply {
                        addDocumentListener(object : DocumentListener {
                            override fun documentChanged(event: DocumentEvent) {
                                this@addToFormBuilder.value = this@apply.document.text
                            }
                        })
                    }, 1, false
                )
            }

            LLMParam.ParamType.JsonPath -> {
                val language = Language.findLanguageByID("JSONPath") ?: PlainTextLanguage.INSTANCE
                formBuilder.addLabeledComponent(
                    jBLabel(this.label), LanguageTextField(
                        language, project, value
                    ).apply {
                        addDocumentListener(object : DocumentListener {
                            override fun documentChanged(event: DocumentEvent) {
                                this@addToFormBuilder.value = this@apply.document.text
                            }
                        })
                    }, 1, false
                )
            }

            LLMParam.ParamType.ComboBox -> {
                formBuilder.addLabeledComponent(jBLabel(this.label), ReactiveComboBox(this), 1, false)
            }

            else -> {
                formBuilder.addSeparator()
            }
        }
    }

    private val formBuilder: FormBuilder = FormBuilder.createFormBuilder()
    val panel: JPanel get() = formBuilder.panel


    fun applySettings(settings: AutoDevSettingsState, updateParams: Boolean = false) {
        panel.removeAll()

        // Update LLM dropdowns
        updateLLMDropdowns()

        // Create new LLM button
        val createLLMButton = JButton("Create New LLM")
        createLLMButton.addActionListener { createNewLLM() }

        formBuilder
            .addLLMParam(languageParam)
            .addSeparator()
            .addLLMParams(currentLLMParams)
            .addLLMParam(maxTokenLengthParam)
            .addLLMParam(delaySecondsParam)
            .addComponent(panel {
                testLLMConnection()
                row {
                    text(AutoDevBundle.message("settings.autodev.coder.testConnectionButton.tips")).apply {
                        this.component.foreground = JBColor.RED
                    }
                }
            })
            .addSeparator()
            .addVerticalGap(2)
            .addLabeledComponent(JBLabel("LLM Categories"), JPanel(), 1, false)
            .addLabeledComponent(JBLabel("Plan:"), planLLMDropdown, 1, false)
            .addLabeledComponent(JBLabel("Act:"), actLLMDropdown, 1, false)
            .addLabeledComponent(JBLabel("Completion:"), completionLLMDropdown, 1, false)
            .addLabeledComponent(JBLabel("Embedding:"), embeddingLLMDropdown, 1, false)
            .addLabeledComponent(JBLabel("FastApply:"), fastApplyLLMDropdown, 1, false)
            .addComponent(createLLMButton)
            .addSeparator()
            .addVerticalGap(2)
            .addLabeledComponent(JBLabel("All LLMs"), JPanel(), 1, false)
            .addComponentFillVertically(llmTable, 0)
            .addSeparator()
            .addVerticalGap(2)
            .addLabeledComponent(jBLabel("settings.autodev.coder.customLlms", 1), customLlmParam, 1, true)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        panel.invalidate()
        panel.repaint()
    }

    fun exportSettings(destination: AutoDevSettingsState) {
        destination.apply {
            maxTokenLength = maxTokenLengthParam.value
            customModel = customModelParam.value
            customOpenAiHost = customOpenAIHostParam.value
            language = languageParam.value
            customEngineServer = customEngineServerParam.value
            customEngineToken = customEngineTokenParam.value
            customLlms = customLlmParam.text
            customEngineResponseFormat = customEngineResponseFormatParam.value
            customEngineRequestFormat = customEngineRequestBodyFormatParam.value
            delaySeconds = delaySecondsParam.value

            // Export selected model IDs
            selectedPlanModelId = (planLLMDropdown.selectedItem as? ModelItem)?.modelId ?: ""
            selectedActModelId = (actLLMDropdown.selectedItem as? ModelItem)?.modelId ?: ""
            selectedCompletionModelId = (completionLLMDropdown.selectedItem as? ModelItem)?.modelId ?: ""
            selectedEmbeddingModelId = (embeddingLLMDropdown.selectedItem as? ModelItem)?.modelId ?: ""
            selectedFastApplyModelId = (fastApplyLLMDropdown.selectedItem as? ModelItem)?.modelId ?: ""
        }
    }

    // Set selected items in dropdowns based on settings
    private fun setSelectedModels() {
        val settings = AutoDevSettingsState.getInstance()

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

        // Set selected items based on settings
        planLLMDropdown.selectModelById(settings.selectedPlanModelId)
        actLLMDropdown.selectModelById(settings.selectedActModelId)
        completionLLMDropdown.selectModelById(settings.selectedCompletionModelId)
        embeddingLLMDropdown.selectModelById(settings.selectedEmbeddingModelId)
        fastApplyLLMDropdown.selectModelById(settings.selectedFastApplyModelId)
    }

    fun isModified(settings: AutoDevSettingsState): Boolean {
        return settings.maxTokenLength != maxTokenLengthParam.value ||
                settings.customModel != customModelParam.value ||
                settings.language != languageParam.value ||
                settings.customEngineServer != customEngineServerParam.value ||
                settings.customEngineToken != customEngineTokenParam.value ||
                settings.customLlms != customLlmParam.text ||
                settings.customOpenAiHost != customOpenAIHostParam.value ||
                settings.customEngineResponseFormat != customEngineResponseFormatParam.value ||
                settings.customEngineRequestFormat != customEngineRequestBodyFormatParam.value ||
                settings.delaySeconds != delaySecondsParam.value ||
                settings.selectedPlanModelId != ((planLLMDropdown.selectedItem as? ModelItem)?.modelId ?: "") ||
                settings.selectedActModelId != ((actLLMDropdown.selectedItem as? ModelItem)?.modelId ?: "") ||
                settings.selectedCompletionModelId != ((completionLLMDropdown.selectedItem as? ModelItem)?.modelId
            ?: "") ||
                settings.selectedEmbeddingModelId != ((embeddingLLMDropdown.selectedItem as? ModelItem)?.modelId
            ?: "") ||
                settings.selectedFastApplyModelId != ((fastApplyLLMDropdown.selectedItem as? ModelItem)?.modelId ?: "")
    }

    init {
        applySettings(settings)
        LanguageChangedCallback.language = AutoDevSettingsState.getInstance().language
    }
}
