package cc.unitmesh.devti.agent.a2a

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ToolAgentCardTest {

    @Test
    fun `should create ToolAgentCard with minimal fields`() {
        val card = ToolAgentCard.create(
            name = "TestAgent",
            description = "A test agent for demonstration",
            url = "https://example.com/agent",
            providerName = "TestProvider"
        )

        assertEquals("TestAgent", card.name)
        assertEquals("A test agent for demonstration", card.description)
        assertEquals("https://example.com/agent", card.url)
        assertEquals("TestProvider", card.provider.name)
        assertEquals("1.0.0", card.protocolVersion)
        assertEquals("1.0.0", card.version)
    }

    @Test
    fun `should inherit from Tool interface`() {
        val card = ToolAgentCard.create(
            name = "TestAgent",
            description = "A test agent",
            url = "https://example.com/agent",
            providerName = "TestProvider"
        )

        // Verify it implements Tool interface
        assertTrue(card is cc.unitmesh.devti.agent.Tool)
        assertEquals("TestAgent", card.name)
        assertEquals("A test agent", card.description)
    }

    @Test
    fun `should convert to A2A AgentCard format`() {
        val provider = AgentProvider(
            name = "TestProvider",
            url = "https://provider.com",
            description = "Test provider"
        )

        val card = ToolAgentCard.fromA2AAgentCard(
            name = "TestAgent",
            description = "Test agent",
            url = "https://example.com/agent",
            provider = provider,
            version = "2.0.0"
        )

        val a2aCard = card.toA2AAgentCard()

        assertEquals("TestAgent", a2aCard["name"])
        assertEquals("Test agent", a2aCard["description"])
        assertEquals("https://example.com/agent", a2aCard["url"])
        assertEquals("2.0.0", a2aCard["version"])
        assertEquals("1.0.0", a2aCard["protocol_version"])
        assertEquals(provider, a2aCard["provider"])
    }

    @Test
    fun `should convert to MCP Tool format`() {
        val inputSchema = ToolInput(
            type = "object",
            properties = buildJsonObject {
                put("param1", buildJsonObject {
                    put("type", "string")
                    put("description", "First parameter")
                })
            },
            required = listOf("param1")
        )

        val card = ToolAgentCard.fromMCPTool(
            name = "TestTool",
            description = "Test tool",
            url = "https://example.com/tool",
            provider = AgentProvider("TestProvider"),
            title = "Test Tool Title",
            inputSchema = inputSchema
        )

        val mcpTool = card.toMCPTool()

        assertEquals("TestTool", mcpTool.name)
        assertEquals("Test tool", mcpTool.description)
        assertEquals("Test Tool Title", mcpTool.title)
        assertEquals(inputSchema, mcpTool.inputSchema)
    }

    @Test
    fun `should create skill from tool definition`() {
        val card = ToolAgentCard.create(
            name = "SkillAgent",
            description = "Agent with skills",
            url = "https://example.com/skill",
            providerName = "SkillProvider"
        )

        val skill = card.createSkillFromTool()

        assertEquals("SkillAgent", skill.name)
        assertEquals("Agent with skills", skill.description)
        assertEquals(card.defaultInputModes, skill.inputModes)
        assertEquals(card.defaultOutputModes, skill.outputModes)
    }

    @Test
    fun `should serialize and deserialize correctly`() {
        val original = ToolAgentCard.create(
            name = "SerializationTest",
            description = "Test serialization",
            url = "https://example.com/serialize",
            providerName = "SerializeProvider"
        )

        val json = Json.encodeToString(ToolAgentCard.serializer(), original)
        val deserialized = Json.decodeFromString(ToolAgentCard.serializer(), json)

        assertEquals(original.name, deserialized.name)
        assertEquals(original.description, deserialized.description)
        assertEquals(original.url, deserialized.url)
        assertEquals(original.provider.name, deserialized.provider.name)
        assertEquals(original.protocolVersion, deserialized.protocolVersion)
    }

    @Test
    fun `should support complex configurations`() {
        val capabilities = AgentCapabilities(
            supportsStreaming = true,
            supportsAuthentication = true,
            supportsBatching = false
        )

        val skills = listOf(
            AgentSkill(
                name = "skill1",
                description = "First skill",
                inputModes = listOf("text/plain"),
                outputModes = listOf("application/json")
            ),
            AgentSkill(
                name = "skill2",
                description = "Second skill",
                inputModes = listOf("application/json"),
                outputModes = listOf("text/plain")
            )
        )

        val card = ToolAgentCard(
            name = "ComplexAgent",
            description = "Complex agent with multiple features",
            url = "https://example.com/complex",
            provider = AgentProvider("ComplexProvider"),
            capabilities = capabilities,
            skills = skills,
            defaultInputModes = listOf("text/plain", "application/json", "text/markdown"),
            defaultOutputModes = listOf("text/plain", "application/json", "text/html")
        )

        assertEquals(2, card.skills.size)
        assertTrue(card.capabilities.supportsStreaming)
        assertTrue(card.capabilities.supportsAuthentication)
        assertEquals(3, card.defaultInputModes.size)
        assertEquals(3, card.defaultOutputModes.size)
    }
}
