package cc.unitmesh.agent

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Example serializable data class that can be used across all platforms
 */
@Serializable
data class AgentConfig(
    val name: String,
    val version: String,
    val capabilities: List<String>,
    val platform: String = Platform.name
)

/**
 * Example tool implementation that works on all platforms
 */
class ExampleTool : Tool {
    override val name: String = "example-tool"
    override val description: String = "An example tool that demonstrates multiplatform capabilities"
    
    fun createConfig(): AgentConfig {
        return AgentConfig(
            name = "MultiPlatform Agent",
            version = "1.0.0",
            capabilities = listOf("serialization", "cross-platform", "json-processing")
        )
    }
    
    fun serializeConfig(config: AgentConfig): String {
        return Json.encodeToString(config)
    }
}

/**
 * Utility function to demonstrate platform detection
 */
fun demonstratePlatformCapabilities(): String {
    val config = ExampleTool().createConfig()
    val serialized = ExampleTool().serializeConfig(config)
    
    return buildString {
        appendLine("=== Multiplatform Demo ===")
        appendLine(getPlatformInfo())
        appendLine("JVM: ${Platform.isJvm}")
        appendLine("JS: ${Platform.isJs}")
        appendLine("WASM: ${Platform.isWasm}")
        appendLine("Config: $serialized")
    }
}
