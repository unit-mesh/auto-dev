package cc.unitmesh.devti.language.compiler.exec.agents

import cc.unitmesh.devti.a2a.A2AService
import cc.unitmesh.devti.a2a.A2ASketchToolchainProvider
import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.provider.DevInsAgentToolCollector
import com.intellij.openapi.project.Project
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Request format for agents command
 */
@Serializable
data class AgentRequest(
    val agent: String,
    val message: String
)

/**
 * Agents command implementation for listing and invoking AI agents.
 *
 * Example:
 * ```devin
 * /agents
 * ```
 *
 * Or invoke an agent with JSON:
 * ```devin
 * /agents
 * ```json
 * {
 *   "agent": "code-reviewer",
 *   "message": "Please review this code"
 * }
 * ```
 * ```
 */
class AgentsInsCommand(
    private val project: Project,
    private val prop: String,
    private val codeContent: String
) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.AGENTS

    override fun isApplicable(): Boolean = true

    override suspend fun execute(): String? {
        // If no parameter and no code content, list all agents
        if (prop.isBlank() && codeContent.isBlank()) {
            return listAllAgents()
        }

        // Parse request from JSON or legacy format
        val request = parseRequest(prop, codeContent)
        if (request == null) {
            return "${DEVINS_ERROR} Invalid request format. Use JSON: {\"agent\": \"agent-name\", \"message\": \"your message\"}"
        }

        if (request.agent.isEmpty()) {
            return "${DEVINS_ERROR} Agent name is required."
        }

        if (request.message.isEmpty()) {
            return "${DEVINS_ERROR} Message is required."
        }

        // Invoke the specific agent
        return invokeAgent(request.agent, request.message)
    }

    /**
     * List all available agents including A2A agents and DevIns agents
     */
    private fun listAllAgents(): String {
        val result = StringBuilder()
        result.append("Available AI Agents:\n\n")

        // Collect A2A agents
        val a2aAgents = try {
            A2ASketchToolchainProvider.collectA2ATools(project)
        } catch (e: Exception) {
            emptyList()
        }

        // Collect DevIns agents
        val devInsAgents = try {
            DevInsAgentToolCollector.all(project)
        } catch (e: Exception) {
            emptyList()
        }

        if (a2aAgents.isEmpty() && devInsAgents.isEmpty()) {
            result.append("No agents available. Please configure A2A agents or create DevIns agents.\n")
            return result.toString()
        }

        // Show usage examples first
        appendUsageExamples(result)

        // List A2A agents with examples
        if (a2aAgents.isNotEmpty()) {
            result.append("## A2A Agents\n\n")
            a2aAgents.forEachIndexed { index, agent ->
                appendAgentInfo(result, index + 1, agent.name, agent.description)
            }
        }

        // List DevIns agents with examples
        if (devInsAgents.isNotEmpty()) {
            result.append("## DevIns Agents\n\n")
            devInsAgents.forEachIndexed { index, agent ->
                appendAgentInfo(
                    result,
                    index + 1,
                    agent.name,
                    agent.description,
                    scriptPath = agent.devinScriptPath
                )
            }
        }

        result.append("---\n")
        result.append("Total: ${a2aAgents.size + devInsAgents.size} agent(s) available\n")

        return result.toString()
    }

    /**
     * Append usage examples section
     */
    private fun appendUsageExamples(result: StringBuilder) {
        result.append("## Usage Examples\n\n")
        result.append("JSON format:\n")
        result.append(formatAgentExample("agent-name", "your message here"))
    }

    /**
     * Append agent information with example
     */
    private fun appendAgentInfo(
        result: StringBuilder,
        index: Int,
        name: String,
        description: String,
        scriptPath: String? = null
    ) {
        result.append("### $index. $name\n")

        if (description.isNotEmpty()) {
            result.append("**Description**: $description\n\n")
        }

        if (!scriptPath.isNullOrEmpty()) {
            result.append("**Script**: $scriptPath\n\n")
        }

        result.append("**Example**:\n")
        result.append(formatAgentExample(name, "Please help with this task"))
    }

    /**
     * Format agent invocation example
     */
    private fun formatAgentExample(agentName: String, message: String): String {
        return buildString {
            append("<devin>\n")
            append("/agents\n")
            append("```json\n")
            append("{\n")
            append("  \"agent\": \"$agentName\",\n")
            append("  \"message\": \"$message\"\n")
            append("}\n")
            append("```\n")
            append("</devin>\n\n")
        }
    }

    /**
     * Parse request from JSON or legacy format
     */
    private fun parseRequest(prop: String, codeContent: String): AgentRequest? {
        // Try JSON format first
        if (codeContent.isNotBlank()) {
            try {
                return Json.Default.decodeFromString<AgentRequest>(codeContent)
            } catch (e: Exception) {
                // Fallback to legacy format if JSON parsing fails
            }
        }

        // Legacy string format: "agent-name \"message\"" or "agent-name message"
        if (prop.isBlank()) return null

        val (agentName, message) = parseCommand(prop)
        return if (agentName.isNotEmpty()) {
            AgentRequest(agentName, message)
        } else {
            null
        }
    }

    /**
     * Parse the command string to extract agent name and message.
     * Expected format: "<agent_name> \"<message>\"" or "<agent_name> <message>"
     */
    private fun parseCommand(input: String): Pair<String, String> {
        val trimmed = input.trim()

        if (trimmed.isEmpty()) {
            return Pair("", "")
        }

        // Try to parse quoted message first
        val quotedMessageRegex = """^(\S+)\s+"(.+)"$""".toRegex()
        val quotedMatch = quotedMessageRegex.find(trimmed)
        if (quotedMatch != null) {
            val agentName = quotedMatch.groupValues[1]
            val message = quotedMatch.groupValues[2]
            return Pair(agentName, message)
        }

        // Try to parse single quoted message
        val singleQuotedRegex = """^(\S+)\s+'(.+)'$""".toRegex()
        val singleQuotedMatch = singleQuotedRegex.find(trimmed)
        if (singleQuotedMatch != null) {
            val agentName = singleQuotedMatch.groupValues[1]
            val message = singleQuotedMatch.groupValues[2]
            return Pair(agentName, message)
        }

        // Fallback: split by first space
        val parts = trimmed.split(" ", limit = 2)
        if (parts.size >= 2) {
            return Pair(parts[0], parts[1])
        } else if (parts.size == 1) {
            return Pair(parts[0], "")
        }

        return Pair("", "")
    }

    /**
     * Invoke a specific agent by name
     */
    private suspend fun invokeAgent(agentName: String, input: String): String? {
        // Try to find and invoke A2A agent first
        val a2aService = project.getService(A2AService::class.java)
        a2aService.initialize()

        if (a2aService.isAvailable()) {
            // Check if the agent exists in A2A agents by trying to send a message
            try {
                val response = a2aService.sendMessage(agentName, input)
                if (response != null) {
                    return "A2A Agent '$agentName' response:\n$response"
                }
                // If response is null, continue to try DevIns agents
            } catch (e: Exception) {
                // If error occurs, continue to try DevIns agents
            }
        }

        // Try to find and invoke DevIns agent
        val devInsAgents = DevInsAgentToolCollector.all(project)
        val devInsAgent = devInsAgents.find { it.name == agentName }

        if (devInsAgent != null) {
            return try {
                val collectors = com.intellij.openapi.extensions.ExtensionPointName
                    .create<DevInsAgentToolCollector>("cc.unitmesh.devInsAgentTool")
                    .extensionList

                for (collector in collectors) {
                    val result = collector.execute(project, agentName, input)
                    if (result != null) {
                        return "DevIns Agent '$agentName' response:\n$result"
                    }
                }

                "${DEVINS_ERROR} Failed to execute DevIns agent '$agentName'"
            } catch (e: Exception) {
                "${DEVINS_ERROR} Error executing DevIns agent '$agentName': ${e.message}"
            }
        }

        // Agent not found
        return "${DEVINS_ERROR} Agent '$agentName' not found. Use /agents to list all available agents."
    }
}
