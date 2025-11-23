package cc.unitmesh.devti.settings.dialog

import cc.unitmesh.devti.llm2.LLMProvider2
import cc.unitmesh.devti.llm2.model.Auth
import cc.unitmesh.devti.llm2.model.CustomRequest
import cc.unitmesh.devti.llm2.model.LlmConfig
import cc.unitmesh.devti.llm2.model.ModelType
import cc.unitmesh.devti.llms.custom.Message
import cc.unitmesh.devti.provider.local.JsonLanguageField
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

/**
 * Dialog for creating or editing an LLM configuration
 */
class LLMDialog(
    private val project: Project?,
    private val settings: AutoDevSettingsState,
    private val existingLlm: LlmConfig? = null,
    private val onSave: () -> Unit
) : DialogWrapper(project, true) {
    private val nameField = JBTextField()
    private val descriptionField = JBTextField()
    private val urlField = JBTextField()
    private val tokenField = JBTextField()
    private val maxTokensField = JBTextField()

    // Explicit model parameters
    private val modelField = JBTextField()
    private val streamCheckbox = JBCheckBox("Use streaming response", true)
    private val temperatureField = JBTextField()

    // Response resolver field with JSONPath highlighting
    private val responseResolverField = LanguageTextField(
        Language.findLanguageByID("JSONPath") ?: PlainTextLanguage.INSTANCE,
        project,
        ""
    )

    // Custom headers and body fields with JSON highlighting
    private val headersField = JsonLanguageField(project, "{}", "Custom headers JSON", null, false)
    private val bodyField = JsonLanguageField(project, "{}", "Additional request body JSON", null, false)
    private val testResultLabel = JBLabel("")

    init {
        title = if (existingLlm == null) "Add New LLM" else "Edit LLM"

        // Initialize fields with existing values if editing
        if (existingLlm != null) {
            nameField.text = existingLlm.name
            descriptionField.text = existingLlm.description
            urlField.text = existingLlm.url
            tokenField.text = existingLlm.auth.token
            maxTokensField.text = existingLlm.maxTokens.toString()
            streamCheckbox.isSelected = existingLlm.customRequest.stream

            // Extract model and temperature from body
            val modelValue = existingLlm.customRequest.body["model"]?.let {
                when (it) {
                    is JsonPrimitive -> it.content
                    else -> it.toString().removeSurrounding("\"")
                }
            } ?: ""
            modelField.text = modelValue

            val temperatureValue = existingLlm.customRequest.body["temperature"]?.let {
                when (it) {
                    is JsonPrimitive -> it.content
                    else -> it.toString()
                }
            } ?: "0.0"
            temperatureField.text = temperatureValue

            // Initialize response resolver - use legacy responseFormat if available, otherwise determine by stream
            val responseResolver = if (existingLlm.responseFormat.isNotEmpty()) {
                existingLlm.responseFormat
            } else {
                // Default based on streaming mode
                if (existingLlm.customRequest.stream) {
                    "\$.choices[0].delta.content"
                } else {
                    "\$.choices[0].message.content"
                }
            }
            responseResolverField.text = responseResolver

            // Initialize custom headers and body (excluding model and temperature)
            val headersJson = if (existingLlm.customRequest.headers.isNotEmpty()) {
                buildJsonObject {
                    existingLlm.customRequest.headers.forEach { (key, value) ->
                        put(key, value)
                    }
                }.toString()
            } else {
                "{}"
            }
            headersField.text = headersJson

            // Body without model and temperature (they are now explicit fields)
            // Also keep stream if it was explicitly set in body (to support numeric values like 0/1)
            val bodyWithoutModelTemp = existingLlm.customRequest.body.filterKeys {
                it != "model" && it != "temperature"
            }
            val bodyJson = if (bodyWithoutModelTemp.isNotEmpty()) {
                buildJsonObject {
                    bodyWithoutModelTemp.forEach { (key, value) ->
                        put(key, value)
                    }
                }.toString()
            } else {
                "{}"
            }
            bodyField.text = bodyJson
        } else {
            // Default values for new LLM
            maxTokensField.text = "4096"
            modelField.text = "gpt-3.5-turbo"
            temperatureField.text = "0.0"
            // Default response resolver based on streaming (will be set by listener)
            responseResolverField.text = "\$.choices[0].delta.content"
            headersField.text = "{}"
            bodyField.text = "{}"
        }

        // Add listener to update response resolver default when streaming changes
        streamCheckbox.addActionListener {
            // Only update if field is empty or contains default values
            val currentText = responseResolverField.text.trim()
            if (currentText.isEmpty() ||
                currentText == "\$.choices[0].delta.content" ||
                currentText == "\$.choices[0].message.content") {
                responseResolverField.text = if (streamCheckbox.isSelected) {
                    "\$.choices[0].delta.content"
                } else {
                    "\$.choices[0].message.content"
                }
            }
        }

        init()
    }

    override fun createCenterPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(640, 480)

        val formBuilder = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Name:"), nameField)
            .addLabeledComponent(JBLabel("Description:"), descriptionField)
            .addLabeledComponent(JBLabel("URL:"), urlField)
            .addLabeledComponent(JBLabel("Token (optional):"), tokenField)
            .addLabeledComponent(JBLabel("Max tokens:"), maxTokensField)
            .addSeparator()

        // Model parameters section
        formBuilder.addLabeledComponent(JBLabel("Model parameters"), JPanel(), 1, false)
        formBuilder.addLabeledComponent(JBLabel("Model:"), modelField)
        formBuilder.addComponent(streamCheckbox)
        formBuilder.addLabeledComponent(JBLabel("Temperature:"), temperatureField)
        formBuilder.addSeparator()

        // Response resolver section
        formBuilder.addLabeledComponent(JBLabel("Response Resolver (JSONPath):"), responseResolverField)
        formBuilder.addSeparator()

        // Custom headers section with JSON highlighting
        formBuilder.addLabeledComponent(JBLabel("Custom headers (JSON):"), headersField)

        // Custom body section (additional fields) with JSON highlighting
        formBuilder.addLabeledComponent(JBLabel("Additional request body (JSON):"), bodyField)

        formBuilder.addSeparator()

        // Add test button
        val testButton = JButton("Test Connection")
        testButton.addActionListener { testConnection() }
        formBuilder.addComponent(testButton)
        formBuilder.addComponent(testResultLabel)

        panel.add(formBuilder.panel, BorderLayout.CENTER)
        return panel
    }

    private fun testConnection() {
        if (nameField.text.isBlank() || urlField.text.isBlank() || modelField.text.isBlank()) {
            testResultLabel.text = "Name, URL, and Model are required"
            testResultLabel.foreground = JBColor.RED
            return
        }

        // Validate max tokens
        val maxTokens = try {
            maxTokensField.text.toInt()
        } catch (e: NumberFormatException) {
            testResultLabel.text = "Max Tokens must be a valid number"
            testResultLabel.foreground = JBColor.RED
            return
        }

        // Validate temperature
        val temperature = try {
            temperatureField.text.toDouble()
        } catch (e: NumberFormatException) {
            testResultLabel.text = "Temperature must be a valid number"
            testResultLabel.foreground = JBColor.RED
            return
        }

        testResultLabel.text = "Testing connection..."
        testResultLabel.foreground = JBColor.BLUE

        val scope = CoroutineScope(CoroutineName("testConnection"))
        scope.launch {
            try {
                // Parse custom headers
                val headers = try {
                    if (headersField.text.trim().isNotEmpty() && headersField.text.trim() != "{}") {
                        Json.decodeFromString<Map<String, String>>(headersField.text)
                    } else {
                        emptyMap()
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        testResultLabel.text = "Invalid headers JSON: ${e.message}"
                        testResultLabel.foreground = JBColor.RED
                    }
                    return@launch
                }

                // Parse additional body fields and combine with explicit parameters
                val additionalBody = try {
                    if (bodyField.text.trim().isNotEmpty() && bodyField.text.trim() != "{}") {
                        val jsonElement = Json.parseToJsonElement(bodyField.text)
                        if (jsonElement is JsonObject) {
                            jsonElement.toMap()
                        } else {
                            emptyMap()
                        }
                    } else {
                        emptyMap()
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        testResultLabel.text = "Invalid additional body JSON: ${e.message}"
                        testResultLabel.foreground = JBColor.RED
                    }
                    return@launch
                }

                // Combine explicit parameters with additional body
                // If stream is explicitly set in additional body, use that value
                val body = mutableMapOf<String, JsonElement>().apply {
                    put("model", JsonPrimitive(modelField.text))
                    put("temperature", JsonPrimitive(temperature))
                    // Use checkbox value as default
                    put("stream", JsonPrimitive(streamCheckbox.isSelected))
                    // Additional body can override stream if explicitly set
                    putAll(additionalBody)
                }

                // Determine actual stream value (from body if set, otherwise from checkbox)
                val actualStream = when (val streamValue = body["stream"]) {
                    is JsonPrimitive -> {
                        when {
                            streamValue.booleanOrNull != null -> streamValue.boolean
                            streamValue.isString -> streamValue.content.toBoolean()
                            // Handle numeric values: 0 = false, any other number = true
                            streamValue.intOrNull != null -> streamValue.int != 0
                            streamValue.longOrNull != null -> streamValue.long != 0L
                            else -> streamCheckbox.isSelected
                        }
                    }
                    else -> streamCheckbox.isSelected
                }

                // Create a temporary LLM config for testing
                val customRequest = CustomRequest(
                    headers = headers,
                    body = body,
                    stream = actualStream
                )

                // Get response resolver, use default if empty
                val responseResolver = responseResolverField.text.trim().ifEmpty {
                    if (actualStream) {
                        "\$.choices[0].delta.content"
                    } else {
                        "\$.choices[0].message.content"
                    }
                }

                val testConfig = LlmConfig(
                    name = nameField.text,
                    description = descriptionField.text,
                    url = urlField.text,
                    auth = Auth(type = "Bearer", token = tokenField.text),
                    maxTokens = maxTokens,
                    customRequest = customRequest,
                    modelType = ModelType.Default, // Always use Default for new LLMs
                    responseFormat = responseResolver // Store response resolver in legacy field for compatibility
                )

                // Create a provider with the test config
                val provider = LLMProvider2.invoke(
                    requestUrl = testConfig.url,
                    authorizationKey = testConfig.auth.token,
                    responseResolver = responseResolver,
                    requestCustomize = testConfig.toLegacyRequestFormat()
                )

                // Send a test message
                val response = provider.request(Message("user", "Hello, this is a test message."),
                    stream = testConfig.customRequest.stream)
                var responseText = ""
                response.collectLatest {
                    responseText += it.chatMessage.content
                }

                SwingUtilities.invokeLater {
                    testResultLabel.text = "Connection successful!"
                    testResultLabel.foreground = JBColor.GREEN
                }
            } catch (e: Exception) {
                e.printStackTrace()
                SwingUtilities.invokeLater {
                    testResultLabel.text = "Connection failed: ${e.message}"
                    testResultLabel.foreground = JBColor.RED
                }
            }
        }
    }

    override fun doOKAction() {
        if (nameField.text.isBlank() || urlField.text.isBlank() || modelField.text.isBlank()) {
            Messages.showErrorDialog("Name, URL, and Model are required", "Validation Error")
            return
        }

        // Validate max tokens
        val maxTokens = try {
            maxTokensField.text.toInt()
        } catch (e: NumberFormatException) {
            Messages.showErrorDialog("Max Tokens must be a valid number", "Validation Error")
            return
        }

        // Validate temperature
        val temperature = try {
            temperatureField.text.toDouble()
        } catch (e: NumberFormatException) {
            Messages.showErrorDialog("Temperature must be a valid number", "Validation Error")
            return
        }

        try {
            // Parse custom headers
            val headers = try {
                if (headersField.text.trim().isNotEmpty() && headersField.text.trim() != "{}") {
                    Json.decodeFromString<Map<String, String>>(headersField.text)
                } else {
                    emptyMap()
                }
            } catch (e: Exception) {
                Messages.showErrorDialog("Invalid headers JSON: ${e.message}", "Validation Error")
                return
            }

            // Parse additional body fields and combine with explicit parameters
            val additionalBody = try {
                if (bodyField.text.trim().isNotEmpty() && bodyField.text.trim() != "{}") {
                    val jsonElement = Json.parseToJsonElement(bodyField.text)
                    if (jsonElement is JsonObject) {
                        jsonElement.toMap()
                    } else {
                        emptyMap()
                    }
                } else {
                    emptyMap()
                }
            } catch (e: Exception) {
                Messages.showErrorDialog("Invalid additional body JSON: ${e.message}", "Validation Error")
                return
            }

            // Combine explicit parameters with additional body
            // If stream is explicitly set in additional body, use that value
            val body = mutableMapOf<String, JsonElement>().apply {
                put("model", JsonPrimitive(modelField.text))
                put("temperature", JsonPrimitive(temperature))
                // Use checkbox value as default
                put("stream", JsonPrimitive(streamCheckbox.isSelected))
                // Additional body can override stream if explicitly set
                putAll(additionalBody)
            }

            // Determine actual stream value (from body if set, otherwise from checkbox)
            val actualStream = when (val streamValue = body["stream"]) {
                is JsonPrimitive -> {
                    when {
                        streamValue.booleanOrNull != null -> streamValue.boolean
                        streamValue.isString -> streamValue.content.toBoolean()
                        // Handle numeric values: 0 = false, any other number = true
                        streamValue.intOrNull != null -> streamValue.int != 0
                        streamValue.longOrNull != null -> streamValue.long != 0L
                        else -> streamCheckbox.isSelected
                    }
                }
                else -> streamCheckbox.isSelected
            }

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

            // Create custom request
            val customRequest = CustomRequest(
                headers = headers,
                body = body,
                stream = actualStream
            )

            // Get response resolver, use default if empty
            val responseResolver = responseResolverField.text.trim().ifEmpty {
                if (actualStream) {
                    "\$.choices[0].delta.content"
                } else {
                    "\$.choices[0].message.content"
                }
            }

            // Create new LLM config
            val newLlm = LlmConfig(
                name = nameField.text,
                description = descriptionField.text,
                url = urlField.text,
                auth = Auth(type = "Bearer", token = tokenField.text),
                maxTokens = maxTokens,
                customRequest = customRequest,
                modelType = ModelType.Default, // Always use Default for new LLMs
                responseFormat = responseResolver // Store response resolver in legacy field for compatibility
            )

            // Add to list and update settings
            existingLlms.add(newLlm)
            val json = Json { prettyPrint = true }
            settings.customLlms = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(LlmConfig.serializer()),
                existingLlms
            )

            // Call the onSave callback
            onSave()

            super.doOKAction()
        } catch (e: Exception) {
            Messages.showErrorDialog("Error saving LLM: ${e.message}", "Error")
        }
    }
}