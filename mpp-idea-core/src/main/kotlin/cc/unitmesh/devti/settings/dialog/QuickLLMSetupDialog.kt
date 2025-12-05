package cc.unitmesh.devti.settings.dialog

import cc.unitmesh.devti.llm2.model.Auth
import cc.unitmesh.devti.llm2.model.CustomRequest
import cc.unitmesh.devti.llm2.model.LlmConfig
import cc.unitmesh.devti.llm2.model.ModelType
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

/**
 * Quick setup dialog for popular LLM providers with predefined configurations
 */
class QuickLLMSetupDialog(
    private val project: Project?,
    private val settings: AutoDevSettingsState,
    private val onSave: () -> Unit
) : DialogWrapper(project, true) {

    private val providerComboBox = JComboBox(arrayOf(
        "OpenAI",
        "Azure OpenAI", 
        "Anthropic Claude",
        "Google Gemini",
        "DeepSeek",
        "GLM (智谱清言)",
        "Qwen (通义千问)",
        "Moonshot (月之暗面)",
        "Baichuan (百川)",
        "Ollama (Local)",
        "Custom"
    ))

    private val nameField = JBTextField()
    private val urlField = JBTextField()
    private val tokenField = JBTextField()
    private val modelField = JBTextField()
    private val testButton = JButton("Test Connection")
    private val testResultLabel = JBLabel("")

    init {
        title = "Quick LLM Setup"
        init()

        // Set up provider change listener
        providerComboBox.addActionListener {
            updateFieldsForProvider()
        }

        // Set up test button listener
        testButton.addActionListener {
            testConnection()
        }

        // Initialize with first provider
        updateFieldsForProvider()
    }

    override fun createCenterPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(600, 500)

        val formBuilder = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Provider:"), providerComboBox)
            .addSeparator()
            .addLabeledComponent(JBLabel("Configuration Name:"), nameField)
            .addLabeledComponent(JBLabel("API URL:"), urlField)
            .addLabeledComponent(JBLabel("API Token:"), tokenField)
            .addLabeledComponent(JBLabel("Model Name:"), modelField)
            .addSeparator()

        // Add test connection section
        val testPanel = JPanel()
        testPanel.add(testButton)
        testPanel.add(testResultLabel)
        formBuilder.addLabeledComponent(JBLabel("Connection Test:"), testPanel)

        panel.add(formBuilder.panel, BorderLayout.CENTER)
        return panel
    }

    private fun updateFieldsForProvider() {
        when (providerComboBox.selectedItem as String) {
            "OpenAI" -> {
                nameField.text = "OpenAI GPT"
                urlField.text = "https://api.openai.com/v1/chat/completions"
                modelField.text = "gpt-3.5-turbo"
                tokenField.text = ""
            }
            "Azure OpenAI" -> {
                nameField.text = "Azure OpenAI"
                urlField.text = "https://your-resource.openai.azure.com/openai/deployments/your-deployment/chat/completions?api-version=2023-05-15"
                modelField.text = "gpt-35-turbo"
                tokenField.text = ""
            }
            "Anthropic Claude" -> {
                nameField.text = "Claude"
                urlField.text = "https://api.anthropic.com/v1/messages"
                modelField.text = "claude-3-sonnet-20240229"
                tokenField.text = ""
            }
            "Google Gemini" -> {
                nameField.text = "Gemini"
                urlField.text = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"
                modelField.text = "gemini-pro"
                tokenField.text = ""
            }
            "DeepSeek" -> {
                nameField.text = "DeepSeek"
                urlField.text = "https://api.deepseek.com/v1/chat/completions"
                modelField.text = "deepseek-chat"
                tokenField.text = ""
            }
            "GLM (智谱清言)" -> {
                nameField.text = "GLM"
                urlField.text = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
                modelField.text = "glm-4"
                tokenField.text = ""
            }
            "Qwen (通义千问)" -> {
                nameField.text = "Qwen"
                urlField.text = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation"
                modelField.text = "qwen-turbo"
                tokenField.text = ""
            }
            "Moonshot (月之暗面)" -> {
                nameField.text = "Moonshot"
                urlField.text = "https://api.moonshot.cn/v1/chat/completions"
                modelField.text = "moonshot-v1-8k"
                tokenField.text = ""
            }
            "Baichuan (百川)" -> {
                nameField.text = "Baichuan"
                urlField.text = "https://api.baichuan-ai.com/v1/chat/completions"
                modelField.text = "Baichuan2-Turbo"
                tokenField.text = ""
            }
            "Ollama (Local)" -> {
                nameField.text = "Ollama Local"
                urlField.text = "http://localhost:11434/v1/chat/completions"
                modelField.text = "llama2"
                tokenField.text = "" // Not required for local
            }
            "Custom" -> {
                nameField.text = "Custom LLM"
                urlField.text = ""
                modelField.text = ""
                tokenField.text = ""
            }
        }
    }

    override fun doOKAction() {
        // Validate inputs
        if (nameField.text.isBlank() || urlField.text.isBlank() || modelField.text.isBlank()) {
            Messages.showErrorDialog("Please fill in all required fields", "Validation Error")
            return
        }

        // Token is optional for some providers
        val provider = providerComboBox.selectedItem as String
        if (tokenField.text.isBlank() && provider !in listOf("Ollama (Local)", "Custom")) {
            Messages.showErrorDialog("API Token is required for this provider", "Validation Error")
            return
        }

        try {
            // Create custom request with model and basic settings
            val customRequest = CustomRequest(
                headers = emptyMap(),
                body = mapOf(
                    "model" to JsonPrimitive(modelField.text),
                    "temperature" to JsonPrimitive(0.0),
                    "stream" to JsonPrimitive(true)
                ),
                stream = true
            )

            // Create the LLM configuration
            val llmConfig = LlmConfig(
                name = nameField.text,
                description = "Created by Quick Setup - $provider",
                url = urlField.text,
                auth = Auth(
                    type = "Bearer",
                    token = tokenField.text
                ),
                maxTokens = 4096, // Default max tokens
                customRequest = customRequest,
                modelType = ModelType.Default
            )

            // Get existing LLMs and add the new one
            val existingLlms = try {
                LlmConfig.load().toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            // Check if name already exists
            if (existingLlms.any { it.name == nameField.text }) {
                Messages.showErrorDialog("A configuration with this name already exists", "Duplicate Name")
                return
            }

            existingLlms.add(llmConfig)

            // Save to settings
            val json = Json { prettyPrint = true }
            settings.customLlms = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(LlmConfig.serializer()),
                existingLlms
            )

            // Set as default model if no default is set
            if (settings.defaultModelId.isEmpty()) {
                settings.defaultModelId = nameField.text
                settings.useDefaultForAllCategories = true
            }

            Messages.showInfoMessage(
                "LLM configuration '${nameField.text}' has been created successfully.",
                "Configuration Added"
            )

            // Notify parent to refresh UI
            onSave()

            super.doOKAction()
        } catch (e: Exception) {
            Messages.showErrorDialog("Error creating LLM configuration: ${e.message}", "Error")
        }
    }

    private fun testConnection() {
        // Validate required fields first
        if (urlField.text.isBlank() || modelField.text.isBlank()) {
            testResultLabel.text = "Please fill in URL and Model fields first"
            testResultLabel.foreground = com.intellij.ui.JBColor.RED
            return
        }

        val provider = providerComboBox.selectedItem as String
        if (tokenField.text.isBlank() && provider !in listOf("Ollama (Local)", "Custom")) {
            testResultLabel.text = "API Token is required for this provider"
            testResultLabel.foreground = com.intellij.ui.JBColor.RED
            return
        }

        // Disable test button during testing
        testButton.isEnabled = false
        testResultLabel.text = "Testing connection..."
        testResultLabel.foreground = com.intellij.ui.JBColor.BLUE

        // Test connection in background thread
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.CoroutineName("testConnection"))
        scope.launch {
            try {
                // Create test configuration
                val customRequest = CustomRequest(
                    headers = emptyMap(),
                    body = mapOf(
                        "model" to JsonPrimitive(modelField.text),
                        "temperature" to JsonPrimitive(0.0),
                        "stream" to JsonPrimitive(true)
                    ),
                    stream = true
                )

                val testConfig = LlmConfig(
                    name = nameField.text.ifBlank { "Test" },
                    description = "Test configuration",
                    url = urlField.text,
                    auth = Auth(type = "Bearer", token = tokenField.text),
                    maxTokens = 4096,
                    customRequest = customRequest,
                    modelType = ModelType.Default
                )

                // Create provider and test
                val provider = cc.unitmesh.devti.llm2.LLMProvider2.invoke(
                    requestUrl = testConfig.url,
                    authorizationKey = testConfig.auth.token,
                    responseResolver = testConfig.getResponseFormatByStream(),
                    requestCustomize = testConfig.toLegacyRequestFormat()
                )

                val response = provider.request(
                    cc.unitmesh.devti.llms.custom.Message("user", "Hello, this is a test message."),
                    stream = testConfig.customRequest.stream
                )

                var responseText = ""
                response.collectLatest {
                    responseText += it.chatMessage.content
                }

                SwingUtilities.invokeLater {
                    testResultLabel.text = "Connection successful!"
                    testResultLabel.foreground = com.intellij.ui.JBColor.GREEN
                    testButton.isEnabled = true
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    testResultLabel.text = "Connection failed: ${e.message}"
                    testResultLabel.foreground = com.intellij.ui.JBColor.RED
                    testButton.isEnabled = true
                }
            }
        }
    }
}
