package cc.unitmesh.devti.language.compiler.exec.agents

import cc.unitmesh.devti.a2a.A2ARequest
import cc.unitmesh.devti.a2a.A2AService
import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import com.intellij.openapi.project.Project
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * A2A (Agent-to-Agent) command implementation for sending messages to A2A protocol agents.
 *
 * Example:
 * ```json
 * {
 *   "agent": "code-reviewer",
 *   "message": "Please review this code for potential issues"
 * }
 * ```
 */
class A2AInsCommand(
    private val project: Project,
    private val prop: String,
    private val codeContent: String
) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.A2A

    override fun isApplicable(): Boolean {
        val a2aService = project.getService(A2AService::class.java)
        return a2aService.isAvailable()
    }

    override suspend fun execute(): String? {
        val a2aService = project.getService(A2AService::class.java)
        a2aService.initialize()

        if (!a2aService.isAvailable()) {
            return "A2A service is not available. Please check your A2A configuration."
        }

        // Try to parse as JSON first, fallback to legacy string format
        val request = parseRequest(prop, codeContent)
        if (request == null) {
            return "${DEVINS_ERROR} Invalid request format. Use JSON: {\"agent\": \"code-reviewer\", \"message\": \"review this\"} or legacy format: agent-name \"message\""
        }

        if (request.agent.isEmpty()) {
            return "${DEVINS_ERROR} Agent name is required."
        }

        if (request.message.isEmpty()) {
            return "${DEVINS_ERROR} Message is required."
        }

        return try {
            val response = a2aService.sendMessage(request.agent, request.message)
            if (response != null) {
                "A2A Agent '${request.agent}' response:\n$response"
            } else {
                "${DEVINS_ERROR} Failed to get response from agent '${request.agent}'"
            }
        } catch (e: Exception) {
            "${DEVINS_ERROR} Error communicating with agent '${request.agent}': ${e.message}"
        }
    }

    private fun parseRequest(prop: String, codeContent: String): A2ARequest? {
        // Try JSON format first
        if (codeContent.isNotBlank()) {
            try {
                return Json.Default.decodeFromString<A2ARequest>(codeContent)
            } catch (e: SerializationException) {
                // Fallback to legacy format
            }
        }

        // Legacy string format: "agent-name \"message\"" or "agent-name message"
        if (prop.isBlank()) return null

        val (agentName, message) = parseCommand(prop)
        return if (agentName.isNotEmpty()) {
            A2ARequest(agentName, message)
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
}