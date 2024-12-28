package cc.unitmesh.devti.agent.model

import cc.unitmesh.devti.custom.team.InteractionType
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.serialization.json.Json
import org.junit.Test

class CustomAgentConfigTest {

    @Test
    fun should_load_enabled_agents_from_valid_json_config() {
        // Given
        val result = mockProjectWithJsonConfig("""
            [
                {
                    "name": "CustomAgent1",
                    "enabled": true
                },
                {
                    "name": "CustomAgent2",
                    "enabled": false
                }
            ]
        """)

        // Then
        assertEquals(2, result.size)
        assertEquals("CustomAgent1", result[0].name)
    }

    @Test
    fun should_load_all_fields_from_valid_json_config() {
        // Given
        val result = mockProjectWithJsonConfig("""
            [
                {
                    "name": "CustomAgent",
                    "description": "This is a custom agent configuration example.",
                    "url": "https://custom-agent.example.com",
                    "icon": "https://custom-agent.example.com/icon.png",
                    "responseAction": "Direct",
                    "transition": [
                        {
                            "source": "$.from",
                            "target": "$.to"
                        }
                    ],
                    "interactive": "ChatPanel",
                    "auth": {
                        "type": "Bearer",
                        "token": "<PASSWORD>"
                    },
                    "defaultTimeout": 20,
                    "enabled": true
                }
            ]
        """)
        // Then
        assertEquals(1, result.size)
        val config = result[0]
        assertEquals("CustomAgent", config.name)
        assertEquals("This is a custom agent configuration example.", config.description)
        assertEquals("https://custom-agent.example.com", config.url)
        assertEquals("https://custom-agent.example.com/icon.png", config.icon)
        assertEquals(CustomAgentResponseAction.Direct, config.responseAction)
        assertEquals(1, config.transition.size)
        assertEquals("$.from", config.transition[0].source)
        assertEquals("$.to", config.transition[0].target)
        assertEquals(InteractionType.ChatPanel, config.interactive)
        assertEquals(AuthType.Bearer, config.auth?.type)
        assertEquals("<PASSWORD>", config.auth?.token)
        assertEquals(20, config.defaultTimeout)
        assertEquals(true, config.enabled)
    }

    @Test
    fun should_load_all_fields_from_valid_json_config_with_connector() {
        // Given
        val result = mockProjectWithJsonConfig("""
            [
                {
                    "name": "dify",
                    "description": "Dify Example",
                    "url": "https://api.dify.ai/v1/completion-messages",
                    "auth": {
                        "type": "Bearer",
                        "token": "app-abc"
                    },
                    "connector": {
                        "requestFormat": "{\"fields\": {\"inputs\": {\"feature\": \"${'$'}content\"}, \"response_mode\": \"streaming\" }}",
                        "responseFormat": "$.answer"
                    },
                    "responseAction": "Stream",
                    "defaultTimeout": 20
                }
            ]
        """)

        assertEquals(1, result.size)
        val config = result[0]
        assertEquals("dify", config.name)
        assertEquals("Dify Example", config.description)
        assertEquals("https://api.dify.ai/v1/completion-messages", config.url)
        assertEquals(AuthType.Bearer, config.auth?.type)
        assertEquals("app-abc", config.auth?.token)
        assertEquals("{\"fields\": {\"inputs\": {\"feature\": \"\$content\"}, \"response_mode\": \"streaming\" }}", config.connector?.requestFormat)
        assertEquals("$.answer", config.connector?.responseFormat)
        assertEquals(CustomAgentResponseAction.Stream, config.responseAction)
        assertEquals(20, config.defaultTimeout)
    }

    private fun mockProjectWithJsonConfig(jsonConfig: String): List<CustomAgentConfig> {
        return Json.decodeFromString(jsonConfig)
    }
}
