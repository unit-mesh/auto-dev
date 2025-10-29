package cc.unitmesh.agent

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToolTest {

    private class TestTool : Tool {
        override val name: String = "test-tool"
        override val description: String = "A test tool for multiplatform testing"
    }

    @Test
    fun testToolInterface() {
        val tool = TestTool()
        assertEquals("test-tool", tool.name)
        assertEquals("A test tool for multiplatform testing", tool.description)
    }

    @Test
    fun testExampleTool() {
        val tool = ExampleTool()
        assertEquals("example-tool", tool.name)

        val config = tool.createConfig()
        assertEquals("MultiPlatform Agent", config.name)
        assertEquals("1.0.0", config.version)
        assertTrue(config.capabilities.contains("serialization"))

        // Test serialization
        val serialized = tool.serializeConfig(config)
        assertTrue(serialized.contains("MultiPlatform Agent"))

        // Test deserialization
        val deserialized = Json.decodeFromString<AgentConfig>(serialized)
        assertEquals(config, deserialized)
    }

    @Test
    fun testPlatformDetection() {
        val demo = demonstratePlatformCapabilities()
        assertTrue(demo.contains("Multiplatform Demo"))
        assertTrue(demo.contains("Running on"))

        // At least one platform should be true
        assertTrue(Platform.isJvm || Platform.isJs || Platform.isWasm)
    }
}
