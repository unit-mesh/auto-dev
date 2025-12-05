package cc.unitmesh.devti.settings

import cc.unitmesh.devti.llm2.model.Auth
import cc.unitmesh.devti.llm2.model.LlmConfig
import cc.unitmesh.devti.llm2.model.ModelType
import com.intellij.testFramework.LightPlatformTestCase
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimplifiedLLMSettingComponentTest : LightPlatformTestCase() {

    override fun setUp() {
        super.setUp()
        // Clear settings before each test
        val settings = AutoDevSettingsState.getInstance()
        settings.customLlms = ""
        settings.defaultModelId = ""
        settings.useDefaultForAllCategories = true
        settings.selectedPlanModelId = ""
        settings.selectedActModelId = ""
        settings.selectedCompletionModelId = ""
        settings.selectedEmbeddingModelId = ""
        settings.selectedFastApplyModelId = ""
        @Suppress("DEPRECATION")
        settings.customEngineServer = ""
        @Suppress("DEPRECATION")
        settings.customEngineToken = ""
        @Suppress("DEPRECATION")
        settings.customModel = ""
    }

    override fun tearDown() {
        // Clean up after each test
        val settings = AutoDevSettingsState.getInstance()
        settings.customLlms = ""
        settings.defaultModelId = ""
        settings.useDefaultForAllCategories = true
        settings.selectedPlanModelId = ""
        settings.selectedActModelId = ""
        settings.selectedCompletionModelId = ""
        settings.selectedEmbeddingModelId = ""
        settings.selectedFastApplyModelId = ""
        @Suppress("DEPRECATION")
        settings.customEngineServer = ""
        @Suppress("DEPRECATION")
        settings.customEngineToken = ""
        @Suppress("DEPRECATION")
        settings.customModel = ""
        super.tearDown()
    }

    fun testSettingsStateDefaultValues() {
        val settings = AutoDevSettingsState()

        // Test default values for new fields
        assertEquals("", settings.defaultModelId)
        assertTrue(settings.useDefaultForAllCategories)
        assertEquals("", settings.selectedPlanModelId)
        assertEquals("", settings.selectedActModelId)
        assertEquals("", settings.selectedCompletionModelId)
        assertEquals("", settings.selectedEmbeddingModelId)
        assertEquals("", settings.selectedFastApplyModelId)
    }

    fun testLlmConfigForCategory() {
        // Get the application settings instance
        val settings = AutoDevSettingsState.getInstance()

        // Create a test LLM configuration
        val testLlm = LlmConfig(
            name = "test-model",
            description = "Test model",
            url = "https://api.test.com/v1/chat/completions",
            auth = Auth(type = "Bearer", token = "test-token"),
            requestFormat = """{ "customFields": {"model": "test-model", "temperature": 0.0, "stream": true} }""",
            responseFormat = "\$.choices[0].delta.content",
            modelType = ModelType.Default
        )

        // Save the test LLM to settings
        val json = Json { prettyPrint = true }
        settings.customLlms = json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(LlmConfig.serializer()),
            listOf(testLlm)
        )

        // Set as default model
        settings.defaultModelId = "test-model"
        settings.useDefaultForAllCategories = true

        // Test that forCategory returns the default model
        val planModel = LlmConfig.forCategory(ModelType.Plan)
        assertEquals("test-model", planModel.name)
        assertEquals("https://api.test.com/v1/chat/completions", planModel.url)
    }

    fun testLegacyConfigMigration() {
        // Get the application settings instance
        val settings = AutoDevSettingsState.getInstance()

        // Set up legacy configuration
        @Suppress("DEPRECATION")
        settings.customEngineServer = "https://api.legacy.com/v1/chat/completions"
        @Suppress("DEPRECATION")
        settings.customEngineToken = "legacy-token"
        @Suppress("DEPRECATION")
        settings.customModel = "legacy-model"

        // Test that default() method handles legacy config
        val defaultConfig = LlmConfig.default()
        assertEquals("legacy-model", defaultConfig.name)
        assertEquals("https://api.legacy.com/v1/chat/completions", defaultConfig.url)
        assertEquals("legacy-token", defaultConfig.auth.token)
    }

    fun testCategorySpecificModels() {
        // Get the application settings instance
        val settings = AutoDevSettingsState.getInstance()

        // Create test LLM configurations
        val chatModel = LlmConfig(
            name = "chat-model",
            description = "Chat model",
            url = "https://api.chat.com/v1/chat/completions",
            auth = Auth(type = "Bearer", token = "chat-token"),
            requestFormat = """{ "customFields": {"model": "chat-model", "temperature": 0.0, "stream": true} }""",
            responseFormat = "\$.choices[0].delta.content",
            modelType = ModelType.Default
        )

        val planModel = LlmConfig(
            name = "plan-model",
            description = "Planning model",
            url = "https://api.plan.com/v1/chat/completions",
            auth = Auth(type = "Bearer", token = "plan-token"),
            requestFormat = """{ "customFields": {"model": "plan-model", "temperature": 0.0, "stream": true} }""",
            responseFormat = "\$.choices[0].delta.content",
            modelType = ModelType.Plan
        )

        // Save the test LLMs to settings
        val json = Json { prettyPrint = true }
        settings.customLlms = json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(LlmConfig.serializer()),
            listOf(chatModel, planModel)
        )

        // Configure category-specific models
        settings.defaultModelId = "chat-model"
        settings.useDefaultForAllCategories = false
        settings.selectedPlanModelId = "plan-model"

        // Test that forCategory returns the correct models
        val defaultModel = LlmConfig.forCategory(ModelType.Default)
        assertEquals("chat-model", defaultModel.name)

        val categoryPlanModel = LlmConfig.forCategory(ModelType.Plan)
        assertEquals("plan-model", categoryPlanModel.name)

        // Test that unspecified categories fall back to default
        val actModel = LlmConfig.forCategory(ModelType.Act)
        assertEquals("chat-model", actModel.name) // Should fallback to default
    }
}
