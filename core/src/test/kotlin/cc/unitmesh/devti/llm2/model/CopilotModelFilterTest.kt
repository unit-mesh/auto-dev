package cc.unitmesh.devti.llm2.model

import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

class CopilotModelFilterTest {

    @Test
    fun `should filter out models with disabled policy state`() {
        // Given
        val json = Json { ignoreUnknownKeys = true }
        val responseJson = """
        {
            "data": [
                {
                    "id": "gpt-4o",
                    "name": "GPT-4o",
                    "object": "model",
                    "vendor": "openai",
                    "version": "2024-05-13",
                    "preview": false,
                    "model_picker_enabled": true,
                    "capabilities": {
                        "type": "chat"
                    },
                    "policy": {
                        "state": "enabled"
                    }
                },
                {
                    "id": "claude-3-5-sonnet",
                    "name": "Claude 3.5 Sonnet",
                    "object": "model",
                    "vendor": "anthropic",
                    "version": "20241022",
                    "preview": false,
                    "model_picker_enabled": true,
                    "capabilities": {
                        "type": "chat"
                    },
                    "policy": {
                        "state": "disabled"
                    }
                },
                {
                    "id": "o1-preview",
                    "name": "o1-preview",
                    "object": "model",
                    "vendor": "openai",
                    "version": "2024-09-12",
                    "preview": true,
                    "model_picker_enabled": true,
                    "capabilities": {
                        "type": "chat"
                    }
                }
            ]
        }
        """.trimIndent()

        // When
        val parsedResponse = json.decodeFromString<CopilotModelsResponse>(responseJson)
        
        // Apply the same filtering logic as in the actual code
        val filteredModels = parsedResponse.data.filter { model ->
            model.policy?.state == "enabled" || model.policy == null
        }

        // Then
        assertEquals(3, parsedResponse.data.size, "Original response should have 3 models")
        assertEquals(2, filteredModels.size, "Filtered response should have 2 models")
        
        // Verify the correct models are kept
        val filteredIds = filteredModels.map { it.id }
        assertEquals(listOf("gpt-4o", "o1-preview"), filteredIds)
        
        // Verify the disabled model is filtered out
        val disabledModel = parsedResponse.data.find { it.id == "claude-3-5-sonnet" }
        assertEquals("disabled", disabledModel?.policy?.state)
        assert(!filteredIds.contains("claude-3-5-sonnet"))
    }

    @Test
    fun `should keep all models when all have enabled policy state`() {
        // Given
        val json = Json { ignoreUnknownKeys = true }
        val responseJson = """
        {
            "data": [
                {
                    "id": "gpt-4o",
                    "name": "GPT-4o",
                    "object": "model",
                    "vendor": "openai",
                    "version": "2024-05-13",
                    "preview": false,
                    "model_picker_enabled": true,
                    "capabilities": {
                        "type": "chat"
                    },
                    "policy": {
                        "state": "enabled"
                    }
                },
                {
                    "id": "claude-3-5-sonnet",
                    "name": "Claude 3.5 Sonnet",
                    "object": "model",
                    "vendor": "anthropic",
                    "version": "20241022",
                    "preview": false,
                    "model_picker_enabled": true,
                    "capabilities": {
                        "type": "chat"
                    },
                    "policy": {
                        "state": "enabled"
                    }
                }
            ]
        }
        """.trimIndent()

        // When
        val parsedResponse = json.decodeFromString<CopilotModelsResponse>(responseJson)
        
        // Apply the same filtering logic as in the actual code
        val filteredModels = parsedResponse.data.filter { model ->
            model.policy?.state == "enabled" || model.policy == null
        }

        // Then
        assertEquals(2, parsedResponse.data.size, "Original response should have 2 models")
        assertEquals(2, filteredModels.size, "Filtered response should have 2 models")
        
        // Verify all models are kept
        val filteredIds = filteredModels.map { it.id }
        assertEquals(listOf("gpt-4o", "claude-3-5-sonnet"), filteredIds)
    }

    @Test
    fun `should keep models with null policy for backward compatibility`() {
        // Given
        val json = Json { ignoreUnknownKeys = true }
        val responseJson = """
        {
            "data": [
                {
                    "id": "legacy-model",
                    "name": "Legacy Model",
                    "object": "model",
                    "vendor": "openai",
                    "version": "2024-01-01",
                    "preview": false,
                    "model_picker_enabled": true,
                    "capabilities": {
                        "type": "chat"
                    }
                }
            ]
        }
        """.trimIndent()

        // When
        val parsedResponse = json.decodeFromString<CopilotModelsResponse>(responseJson)
        
        // Apply the same filtering logic as in the actual code
        val filteredModels = parsedResponse.data.filter { model ->
            model.policy?.state == "enabled" || model.policy == null
        }

        // Then
        assertEquals(1, parsedResponse.data.size, "Original response should have 1 model")
        assertEquals(1, filteredModels.size, "Filtered response should have 1 model")
        
        // Verify the model with null policy is kept
        val filteredModel = filteredModels.first()
        assertEquals("legacy-model", filteredModel.id)
        assertEquals(null, filteredModel.policy)
    }
}
