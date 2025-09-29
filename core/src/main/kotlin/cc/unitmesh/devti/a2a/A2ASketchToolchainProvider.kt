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

                // Initialize A2A service from configuration
                a2aService.initialize()

                val agentCards = a2aService.getAvailableAgents()

                agentCards.map { agentCard ->
                    convertAgentCardToTool(agentCard)
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        private fun convertAgentCardToTool(agentCard: AgentCard): AgentTool {
            val name = agentCard.name() ?: "unknown_agent"

            val description = agentCard.description() ?: "A2A Agent"

            val skills = agentCard.skills()?.joinToString(", ") { it.name } ?: ""

            val fullDescription = if (skills.isNotEmpty()) {
                "$description. Available skills: $skills"
            } else {
                description
            }

            val example = generateExampleUsage(name)
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

        private fun generateExampleUsage(agentName: String): String {
            // Prefer the canonical example stored at /agent/toolExamples/a2a.devin
            val base = BuiltinCommand.example("a2a")

            // If we know the concrete agent name, provide a quick tailored example as well.
            if (agentName.isBlank() || agentName == "unknown_agent") return base

            val tailored = """
                
                Quick example for agent \"$agentName\":
                /a2a:anyString
                ```json
                {
                  "agent": "$agentName",
                  "message": "Please help me with my task"
                }
                ```
            """.trimIndent()

            return listOf(base.trimEnd(), tailored).joinToString("\n\n")
        }
    }
}
