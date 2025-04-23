package cc.unitmesh.devti.language.startup

import cc.unitmesh.devti.agent.tool.AgentTool
import cc.unitmesh.devti.language.actions.DevInsRunFileAction
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.provider.DevInsAgentToolCollector
import cc.unitmesh.devti.util.relativePath
import com.intellij.openapi.project.Project

class DevInsAgentToolchainProviderCollector : DevInsAgentToolCollector {
    override fun collect(project: Project): List<AgentTool> {
        val actions = DynamicShireActionService.getInstance(project).getAllActions().filter {
            it.hole?.agentic == true
        }

        return actions.map {
            AgentTool(
                it.hole?.name ?: "<Placeholder>",
                it.hole?.description ?: "<No Description>",
                "",
                devinScriptPath = it.devinFile.virtualFile.relativePath(project),
                isDevIns = true
            )
        }
    }

    override suspend fun execute(project: Project, agentName: String, input: String): String? {
        val config = DynamicShireActionService.getInstance(project).getAllActions().firstOrNull {
            it.hole?.agentic == true && it.hole.name == agentName
        } ?: return "$DEVINS_ERROR No action found for agent name: $agentName"

        return DevInsRunFileAction.suspendExecuteFile(project, config.devinFile)
    }
}