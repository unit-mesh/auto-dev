package cc.unitmesh.devti.a2a

import cc.unitmesh.devti.agent.tool.AgentTool
import cc.unitmesh.devti.sketch.SketchToolchainProvider
import com.intellij.openapi.project.Project
import io.a2a.spec.AgentCard
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand

class A2ASketchToolchainProvider : SketchToolchainProvider {
    override fun collect(): List<AgentTool> = emptyList()

    companion object {
        fun collectA2ATools(project: Project): List<AgentTool> {
            return try {
                val a2aService = project.getService(A2AService::class.java)
                a2aService.initialize()
                a2aService.getAvailableAgents().map { agentCard ->
                    convertAgentCardToTool(agentCard)
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        private fun convertAgentCardToTool(agentCard: AgentCard): AgentTool {
            val name = agentCard.name() ?: "unknown_agent"
            val fullDescription = agentCard.description() ?: "A2A Agent"

            val example = BuiltinCommand.example("a2a")

            return AgentTool(
                name = name,
                description = fullDescription,
                example = example,
                isMcp = false,
                completion = "",
                mcpGroup = "a2a",
                isDevIns = false,
                devinScriptPath = ""
            )
        }
    }
}
