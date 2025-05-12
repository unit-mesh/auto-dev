package cc.unitmesh.devti.provider

import cc.unitmesh.devti.agent.tool.AgentTool
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface DevInsAgentToolCollector {
    fun collect(project: Project): List<AgentTool>

    suspend fun execute(project: Project, agentName: String, input: String): String?

    companion object {
        private val EP_NAME: ExtensionPointName<DevInsAgentToolCollector> =
            ExtensionPointName("cc.unitmesh.devInsAgentTool")

        fun all(project: Project): List<AgentTool> {
            return EP_NAME.extensionList.flatMap { it.collect(project) }
        }
    }
}
