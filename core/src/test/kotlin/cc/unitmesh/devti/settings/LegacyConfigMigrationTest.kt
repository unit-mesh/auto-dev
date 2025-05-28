package cc.unitmesh.devti.settings

import cc.unitmesh.devti.llm2.model.Auth
import cc.unitmesh.devti.llm2.model.LlmConfig
import cc.unitmesh.devti.llm2.model.ModelType
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class LegacyConfigMigrationTest {

    /**
     * Test helper to create a test settings instance with legacy configuration
     */
    private fun createLegacySettings(
        customEngineServer: String = "https://api.legacy.com",
        customEngineToken: String = "legacy-token",
        customModel: String = "legacy-model",
        customEngineRequestFormat: String = """{"model":"legacy-model"}""",
        customEngineResponseFormat: String = "$.choices[0].delta.content",
        customLlms: String = "[]",
        defaultModelId: String = ""
    ): AutoDevSettingsState {
        val settings = AutoDevSettingsState()
        @Suppress("DEPRECATION")
        settings.customEngineServer = customEngineServer
        @Suppress("DEPRECATION")
        settings.customEngineToken = customEngineToken
        @Suppress("DEPRECATION")
        settings.customModel = customModel
        @Suppress("DEPRECATION")
        settings.customEngineRequestFormat = customEngineRequestFormat
        @Suppress("DEPRECATION")
        settings.customEngineResponseFormat = customEngineResponseFormat
        settings.customLlms = customLlms
        settings.defaultModelId = defaultModelId
        return settings
    }

    @Test
    fun testCreateLegacyLlmConfig_WithAllFields() {
        // Given: Complete legacy configuration
        val settings = createLegacySettings(
            customEngineServer = "https://api.custom.com/v1/chat/completions",
            customEngineToken = "custom-token-123",
            customModel = "custom-model-v1",
            customEngineRequestFormat = """{"model":"custom-model-v1","temperature":0.7}""",
            customEngineResponseFormat = "$.data.content"
        )

        // When: Create legacy config using reflection to access private method
        val method = LegacyConfigMigration::class.java.getDeclaredMethod("createLegacyLlmConfig", AutoDevSettingsState::class.java)
        method.isAccessible = true
        val config = method.invoke(LegacyConfigMigration, settings) as LlmConfig

        // Then
        assertEquals("Legacy Configuration", config.name)
        assertEquals("Migrated from legacy settings", config.description)
        assertEquals("https://api.custom.com/v1/chat/completions", config.url)
        assertEquals("Bearer", config.auth.type)
        assertEquals("custom-token-123", config.auth.token)
        assertEquals("""{"model":"custom-model-v1","temperature":0.7}""", config.requestFormat)
        assertEquals("$.data.content", config.responseFormat)
        assertEquals(ModelType.Default, config.modelType)
    }

    @Test
    fun testCreateLegacyLlmConfig_WithEmptyFields() {
        // Given: Empty legacy configuration
        val settings = createLegacySettings(
            customEngineServer = "",
            customEngineToken = "",
            customModel = "",
            customEngineRequestFormat = "",
            customEngineResponseFormat = ""
        )

        // When: Create legacy config using reflection
        val method = LegacyConfigMigration::class.java.getDeclaredMethod("createLegacyLlmConfig", AutoDevSettingsState::class.java)
        method.isAccessible = true
        val config = method.invoke(LegacyConfigMigration, settings) as LlmConfig

        // Then: Should use default values
        assertEquals("Legacy Configuration", config.name)
        assertEquals("https://api.openai.com/v1/chat/completions", config.url)
        assertEquals("", config.auth.token)
        assertTrue(config.requestFormat.contains("legacy-model"), "Should contain default model name")
        assertEquals("\$.choices[0].delta.content", config.responseFormat)
        assertEquals(ModelType.Default, config.modelType)
    }

    @Test
    fun testCreateLegacyLlmConfig_WithPartialFields() {
        // Given: Partial legacy configuration
        val settings = createLegacySettings(
            customEngineServer = "https://api.partial.com",
            customEngineToken = "partial-token",
            customModel = "", // Empty model name
            customEngineRequestFormat = """{"temperature":0.5}""",
            customEngineResponseFormat = "" // Empty response format
        )

        // When: Create legacy config using reflection
        val method = LegacyConfigMigration::class.java.getDeclaredMethod("createLegacyLlmConfig", AutoDevSettingsState::class.java)
        method.isAccessible = true
        val config = method.invoke(LegacyConfigMigration, settings) as LlmConfig

        // Then: Should use defaults for empty fields
        assertEquals("Legacy Configuration", config.name)
        assertEquals("https://api.partial.com", config.url)
        assertEquals("partial-token", config.auth.token)
        assertEquals("""{"temperature":0.5}""", config.requestFormat)
        assertEquals("\$.choices[0].delta.content", config.responseFormat) // Default response format
    }

    @Test
    fun testLegacyConfigDetection_HasLegacyServer() {
        // Given: Settings with legacy server configuration
        val settings = createLegacySettings(
            customEngineServer = "https://api.legacy.com",
            customModel = "",
            customLlms = "[]",
            defaultModelId = ""
        )

        // When: Check if migration is needed (simulate the logic)
        @Suppress("DEPRECATION")
        val hasLegacyConfig = settings.customEngineServer.isNotEmpty() || settings.customModel.isNotEmpty()
        val hasNewConfig = settings.customLlms.isNotEmpty() && settings.customLlms != "[]"
        val hasDefaultModel = settings.defaultModelId.isNotEmpty()
        val shouldMigrate = hasLegacyConfig && (!hasNewConfig || !hasDefaultModel)

        // Then
        assertTrue(hasLegacyConfig, "Should detect legacy configuration")
        assertFalse(hasNewConfig, "Should not have new configuration")
        assertFalse(hasDefaultModel, "Should not have default model")
        assertTrue(shouldMigrate, "Should need migration")
    }

    @Test
    fun testLegacyConfigDetection_HasLegacyModel() {
        // Given: Settings with legacy model configuration
        val settings = createLegacySettings(
            customEngineServer = "",
            customModel = "legacy-model",
            customLlms = "[]",
            defaultModelId = ""
        )

        // When: Check if migration is needed
        @Suppress("DEPRECATION")
        val hasLegacyConfig = settings.customEngineServer.isNotEmpty() || settings.customModel.isNotEmpty()
        val hasNewConfig = settings.customLlms.isNotEmpty() && settings.customLlms != "[]"
        val hasDefaultModel = settings.defaultModelId.isNotEmpty()
        val shouldMigrate = hasLegacyConfig && (!hasNewConfig || !hasDefaultModel)

        // Then
        assertTrue(hasLegacyConfig, "Should detect legacy configuration")
        assertFalse(hasNewConfig, "Should not have new configuration")
        assertFalse(hasDefaultModel, "Should not have default model")
        assertTrue(shouldMigrate, "Should need migration")
    }

    @Test
    fun testLegacyConfigDetection_HasBothConfigs() {
        // Given: Settings with both legacy and new configuration
        val existingLlm = LlmConfig(
            name = "existing-model",
            description = "Existing model",
            url = "https://api.existing.com",
            auth = Auth(type = "Bearer", token = "existing-token"),
            requestFormat = "{}",
            responseFormat = "$.content",
            modelType = ModelType.Default
        )
        val json = Json { prettyPrint = true }
        val customLlms = json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(LlmConfig.serializer()),
            listOf(existingLlm)
        )

        val settings = createLegacySettings(
            customEngineServer = "https://api.legacy.com",
            customModel = "legacy-model",
            customLlms = customLlms,
            defaultModelId = "existing-model"
        )

        // When: Check if migration is needed
        @Suppress("DEPRECATION")
        val hasLegacyConfig = settings.customEngineServer.isNotEmpty() || settings.customModel.isNotEmpty()
        val hasNewConfig = settings.customLlms.isNotEmpty() && settings.customLlms != "[]"
        val hasDefaultModel = settings.defaultModelId.isNotEmpty()
        val shouldMigrate = hasLegacyConfig && (!hasNewConfig || !hasDefaultModel)

        // Then
        assertTrue(hasLegacyConfig, "Should detect legacy configuration")
        assertTrue(hasNewConfig, "Should have new configuration")
        assertTrue(hasDefaultModel, "Should have default model")
        assertFalse(shouldMigrate, "Should not need migration")
    }

    @Test
    fun testLegacyConfigDetection_NoLegacyConfig() {
        // Given: Settings with no legacy configuration
        val settings = createLegacySettings(
            customEngineServer = "",
            customModel = "",
            customLlms = "[]",
            defaultModelId = ""
        )

        // When: Check if migration is needed
        @Suppress("DEPRECATION")
        val hasLegacyConfig = settings.customEngineServer.isNotEmpty() || settings.customModel.isNotEmpty()
        val hasNewConfig = settings.customLlms.isNotEmpty() && settings.customLlms != "[]"
        val hasDefaultModel = settings.defaultModelId.isNotEmpty()
        val shouldMigrate = hasLegacyConfig && (!hasNewConfig || !hasDefaultModel)

        // Then
        assertFalse(hasLegacyConfig, "Should not detect legacy configuration")
        assertFalse(hasNewConfig, "Should not have new configuration")
        assertFalse(hasDefaultModel, "Should not have default model")
        assertFalse(shouldMigrate, "Should not need migration")
    }

    @Test
    fun testLegacyModelTypeOthersCompatibility() {
        // Given: JSON configuration with legacy "Others" ModelType
        val legacyConfigJson = """
        [
            {
                "name": "legacy-model",
                "description": "Legacy model with Others type",
                "url": "https://api.legacy.com",
                "auth": {
                    "type": "Bearer",
                    "token": "legacy-token"
                },
                "requestFormat": "{\"model\":\"legacy-model\"}",
                "responseFormat": "$.choices[0].delta.content",
                "modelType": "Others"
            }
        ]
        """.trimIndent()

        // When: Parse the configuration
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        val configs = json.decodeFromString<List<LlmConfig>>(legacyConfigJson)

        // Then: Should successfully parse and convert "Others" to "Default"
        assertEquals(1, configs.size)
        val config = configs[0]
        assertEquals("legacy-model", config.name)
        assertEquals("Legacy model with Others type", config.description)
        assertEquals("https://api.legacy.com", config.url)
        assertEquals("Bearer", config.auth.type)
        assertEquals("legacy-token", config.auth.token)
        assertEquals(ModelType.Default, config.modelType) // "Others" should be converted to "Default"
    }

    @Test
    fun testModelTypeSerializerBackwardCompatibility() {
        // Test all legacy and current ModelType values
        val testCases = mapOf(
            "Others" to ModelType.Default,
            "Default" to ModelType.Default,
            "Plan" to ModelType.Plan,
            "Act" to ModelType.Act,
            "Completion" to ModelType.Completion,
            "Embedding" to ModelType.Embedding,
            "FastApply" to ModelType.FastApply,
            "UnknownType" to ModelType.Default // Should fallback to Default
        )

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        testCases.forEach { (input, expected) ->
            // Create a minimal LlmConfig JSON with the test ModelType
            val configJson = """
            {
                "name": "test-model",
                "url": "https://api.test.com",
                "auth": {"type": "Bearer", "token": "test"},
                "modelType": "$input"
            }
            """.trimIndent()

            val config = json.decodeFromString<LlmConfig>(configJson)
            assertEquals(expected, config.modelType, "ModelType '$input' should be converted to '$expected'")
        }
    }
}
