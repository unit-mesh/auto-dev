package cc.unitmesh.devti.settings

import cc.unitmesh.devti.llm2.model.Auth
import cc.unitmesh.devti.llm2.model.LlmConfig
import cc.unitmesh.devti.llm2.model.ModelType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import kotlinx.serialization.json.Json
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

/**
 * A wizard to help users quickly configure their first LLM
 */
class LLMConfigurationWizard(private val project: Project?) : DialogWrapper(project, true) {
    
    private val providerComboBox = JComboBox(arrayOf(
        "OpenAI",
        "Azure OpenAI", 
        "Anthropic Claude",
        "Google Gemini",
        "Ollama (Local)",
        "Custom"
    ))
    
    private val nameField = JBTextField()
    private val urlField = JBTextField()
    private val tokenField = JBTextField()
    private val modelField = JBTextField()
    
    init {
        title = "LLM Configuration Wizard"
        init()
        
        // Set up provider change listener
        providerComboBox.addActionListener {
            updateFieldsForProvider()
        }
        
        // Initialize with first provider
        updateFieldsForProvider()
    }
    
    override fun createCenterPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(500, 400)
        
        val formBuilder = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Provider:"), providerComboBox)
            .addSeparator()
            .addLabeledComponent(JBLabel("Configuration Name:"), nameField)
            .addLabeledComponent(JBLabel("API URL:"), urlField)
            .addLabeledComponent(JBLabel("API Token:"), tokenField)
            .addLabeledComponent(JBLabel("Model Name:"), modelField)
            .addSeparator()
        
        // Add help text
        val helpLabel = JBLabel("<html><body style='width: 400px'>" +
                "<b>Quick Setup Guide:</b><br>" +
                "1. Choose your LLM provider<br>" +
                "2. Enter your API credentials<br>" +
                "3. This will be set as your default model<br>" +
                "4. You can add more models later in settings" +
                "</body></html>")
        formBuilder.addComponent(helpLabel)
        
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
            "Ollama (Local)" -> {
                nameField.text = "Ollama Local"
                urlField.text = "http://localhost:11434/v1/chat/completions"
                modelField.text = "llama2"
                tokenField.text = "not-required"
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
        
        if (tokenField.text.isBlank() && providerComboBox.selectedItem != "Ollama (Local)") {
            Messages.showErrorDialog("API Token is required for this provider", "Validation Error")
            return
        }
        
        try {
            // Create the LLM configuration
            val llmConfig = LlmConfig(
                name = nameField.text,
                description = "Created by Configuration Wizard",
                url = urlField.text,
                auth = Auth(
                    type = "Bearer",
                    token = tokenField.text
                ),
                requestFormat = """{ "customFields": {"model": "${modelField.text}", "temperature": 0.0, "stream": true} }""",
                responseFormat = "\$.choices[0].delta.content",
                modelType = ModelType.Default
            )
            
            // Get existing LLMs and add the new one
            val existingLlms = try {
                LlmConfig.load().toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            existingLlms.add(llmConfig)
            
            // Save to settings
            val settings = AutoDevSettingsState.getInstance()
            val json = Json { prettyPrint = true }
            settings.customLlms = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(LlmConfig.serializer()),
                existingLlms
            )
            
            // Set as default model
            settings.defaultModelId = nameField.text
            settings.useDefaultForAllCategories = true
            
            Messages.showInfoMessage(
                "LLM configuration '${nameField.text}' has been created and set as your default model.",
                "Configuration Complete"
            )
            
            super.doOKAction()
        } catch (e: Exception) {
            Messages.showErrorDialog("Error creating LLM configuration: ${e.message}", "Error")
        }
    }
    
    companion object {
        /**
         * Show the wizard if no LLMs are configured
         */
        fun showIfNeeded(project: Project?): Boolean {
            val settings = AutoDevSettingsState.getInstance()
            
            // Check if user has any LLMs configured
            val hasCustomLlms = settings.customLlms.isNotEmpty() && settings.customLlms != "[]"
            @Suppress("DEPRECATION")
            val hasLegacyConfig = settings.customEngineServer.isNotEmpty()
            
            if (!hasCustomLlms && !hasLegacyConfig) {
                val result = Messages.showYesNoDialog(
                    "No LLM is configured. Would you like to run the configuration wizard to set up your first LLM?",
                    "LLM Configuration",
                    Messages.getQuestionIcon()
                )
                
                if (result == Messages.YES) {
                    val wizard = LLMConfigurationWizard(project)
                    return wizard.showAndGet()
                }
            }
            
            return false
        }
    }
}
