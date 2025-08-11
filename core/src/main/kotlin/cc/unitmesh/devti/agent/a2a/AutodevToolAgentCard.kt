package cc.unitmesh.devti.agent.a2a

import cc.unitmesh.devti.agent.Tool
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A2A Protocol AgentCard data structures
 */
@Serializable
data class AgentProvider(
    val name: String,
    val url: String? = null,
    val description: String? = null
)

@Serializable
data class AgentInterface(
    val transport: String,
    val url: String
)

@Serializable
data class AgentCapabilities(
    val supportsStreaming: Boolean = false,
    val supportsAuthentication: Boolean = false,
    val supportsBatching: Boolean = false
)

@Serializable
data class SecurityScheme(
    val type: String,
    val scheme: String? = null,
    val bearerFormat: String? = null,
    val description: String? = null
)

@Serializable
data class Security(
    val schemes: Map<String, List<String>> = emptyMap()
)

@Serializable
data class AgentSkill(
    val name: String,
    val description: String,
    val inputModes: List<String> = emptyList(),
    val outputModes: List<String> = emptyList(),
    val parameters: JsonObject? = null
)

/**
 * MCP Protocol Tool data structures
 */
@Serializable
data class ToolInput(
    val type: String = "object",
    val properties: JsonObject = JsonObject(emptyMap()),
    val required: List<String> = emptyList()
)

@Serializable
data class ToolOutput(
    val type: String = "object",
    val properties: JsonObject = JsonObject(emptyMap())
)

@Serializable
data class ToolAnnotations(
    val audience: List<String> = emptyList(),
    val tags: List<String> = emptyList()
)

/**
 * MCP Tool definition compatible with MCP protocol
 */
@Serializable
data class MCPTool(
    val name: String,
    val title: String? = null,
    val description: String? = null,
    val inputSchema: ToolInput,
    val outputSchema: ToolOutput? = null,
    val annotations: ToolAnnotations? = null
)

/**
 * A unified ToolAgentCard that combines A2A protocol's AgentCard and MCP protocol's Tool
 * while inheriting from the existing Tool interface for backward compatibility.
 *
 * This class provides downward compatibility with both A2A and MCP protocols:
 * - A2A compatibility through AgentCard structure
 * - MCP compatibility through Tool structure
 * - Legacy compatibility through Tool interface inheritance
 */
@Serializable
data class ToolAgentCard(
    // A2A AgentCard fields
    val protocolVersion: String = "1.0.0",
    override val name: String,
    override val description: String,
    val url: String,
    val preferredTransport: String = "JSONRPC",
    val additionalInterfaces: List<AgentInterface> = emptyList(),
    val provider: AgentProvider,
    val version: String = "1.0.0",
    val documentationUrl: String? = null,
    val capabilities: AgentCapabilities = AgentCapabilities(),
    val securitySchemes: Map<String, SecurityScheme> = emptyMap(),
    val security: List<Security> = emptyList(),
    val defaultInputModes: List<String> = listOf("text/plain", "application/json"),
    val defaultOutputModes: List<String> = listOf("text/plain", "application/json"),
    val skills: List<AgentSkill> = emptyList(),
    val supportsAuthenticatedExtendedCard: Boolean = false,

    // MCP Tool fields
    val title: String? = null,
    val inputSchema: ToolInput = ToolInput(),
    val outputSchema: ToolOutput? = null,
    val annotations: ToolAnnotations? = null
) : Tool {

    /**
     * Convert this ToolAgentCard to A2A AgentCard format
     */
    fun toA2AAgentCard(): Map<String, Any?> = mapOf(
        "protocol_version" to protocolVersion,
        "name" to name,
        "description" to description,
        "url" to url,
        "preferred_transport" to preferredTransport,
        "additional_interfaces" to additionalInterfaces,
        "provider" to provider,
        "version" to version,
        "documentation_url" to documentationUrl,
        "capabilities" to capabilities,
        "security_schemes" to securitySchemes,
        "security" to security,
        "default_input_modes" to defaultInputModes,
        "default_output_modes" to defaultOutputModes,
        "skills" to skills,
        "supports_authenticated_extended_card" to supportsAuthenticatedExtendedCard
    )

    /**
     * Convert this ToolAgentCard to MCP Tool format
     */
    fun toMCPTool(): MCPTool = MCPTool(
        name = name,
        title = title,
        description = description,
        inputSchema = inputSchema,
        outputSchema = outputSchema,
        annotations = annotations
    )

    /**
     * Create a skill from MCP tool definition
     */
    fun createSkillFromTool(): AgentSkill = AgentSkill(
        name = name,
        description = description,
        inputModes = defaultInputModes,
        outputModes = defaultOutputModes,
        parameters = inputSchema.properties
    )

    companion object {
        /**
         * Create a ToolAgentCard from A2A AgentCard data
         */
        fun fromA2AAgentCard(
            name: String,
            description: String,
            url: String,
            provider: AgentProvider,
            version: String = "1.0.0",
            protocolVersion: String = "1.0.0",
            skills: List<AgentSkill> = emptyList()
        ): ToolAgentCard = ToolAgentCard(
            protocolVersion = protocolVersion,
            name = name,
            description = description,
            url = url,
            provider = provider,
            version = version,
            skills = skills
        )

        /**
         * Create a ToolAgentCard from MCP Tool data
         */
        fun fromMCPTool(
            name: String,
            description: String,
            url: String,
            provider: AgentProvider,
            title: String? = null,
            inputSchema: ToolInput = ToolInput(),
            outputSchema: ToolOutput? = null,
            annotations: ToolAnnotations? = null
        ): ToolAgentCard = ToolAgentCard(
            name = name,
            description = description,
            url = url,
            provider = provider,
            title = title,
            inputSchema = inputSchema,
            outputSchema = outputSchema,
            annotations = annotations
        )

        /**
         * Create a simple ToolAgentCard with minimal required fields
         */
        fun create(
            name: String,
            description: String,
            url: String,
            providerName: String,
            providerUrl: String? = null
        ): ToolAgentCard = ToolAgentCard(
            name = name,
            description = description,
            url = url,
            provider = AgentProvider(
                name = providerName,
                url = providerUrl,
                description = "Provider for $name"
            )
        )
    }
}