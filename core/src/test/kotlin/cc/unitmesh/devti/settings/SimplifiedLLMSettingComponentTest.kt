package cc.unitmesh.devti.settings

import cc.unitmesh.devti.llm2.model.Auth
import cc.unitmesh.devti.llm2.model.LlmConfig
import cc.unitmesh.devti.llm2.model.ModelType
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimplifiedLLMSettingComponentTest {

    @Test
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

    @Test
    fun testLlmConfigForCategory() {
        val settings = AutoDevSettingsState()
        
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

    @Test
    fun testLegacyConfigMigration() {
        val settings = AutoDevSettingsState()
        
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

    @Test
    fun testCategorySpecificModels() {
        val settings = AutoDevSettingsState()
        
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
