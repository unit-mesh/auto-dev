package cc.unitmesh.devti.language.compiler.exec.agents

import cc.unitmesh.devti.a2a.A2AService
import cc.unitmesh.devti.a2a.AgentRequest
import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * A2A (Agent-to-Agent) command implementation for sending messages to A2A protocol agents.
 *
 * Example:
 * <devin>
 * /a2a
 * ```json
 * {
 *   "agent": "code-reviewer",
 *   "message": "Please review this code for potential issues"
 * }
 * ```
 * <devin>
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

    fun parseRequest(prop: String, codeContent: String): AgentRequest? {
        if (codeContent.isNotBlank()) {
            try {
                return Json.Default.decodeFromString<AgentRequest>(codeContent)
            } catch (e: SerializationException) {
                logger<A2AInsCommand>().warn("Failed to parse JSON request: $e")
            }
        }

        return null
    }
}