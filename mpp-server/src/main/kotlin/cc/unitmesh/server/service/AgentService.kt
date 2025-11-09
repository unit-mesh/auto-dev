package cc.unitmesh.server.service

import cc.unitmesh.server.config.LLMConfig
import cc.unitmesh.server.model.AgentRequest
import cc.unitmesh.server.model.AgentResponse

class AgentService(private val defaultLLMConfig: LLMConfig) {

    suspend fun executeAgent(
        projectPath: String,
        request: AgentRequest
    ): AgentResponse {
        // TODO: Implement actual agent execution
        // For MVP, return a simple response
        return AgentResponse(
            success = true,
            message = "Task '${request.task}' received for project at $projectPath",
            output = "This is a placeholder response. Agent execution will be implemented in the next phase."
        )
    }
}

