package cc.unitmesh.devti.provider.devins

import cc.unitmesh.devti.agent.model.CustomAgentConfig
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread

data class CustomAgentContext(
    val config: CustomAgentConfig,
    val response: String
)

interface AgentResponseProvider {
    val name: String

    @RequiresBackgroundThread
    fun execute(project: Project, context: CustomAgentContext): String

    companion object {
        val EP_NAME = ExtensionPointName<AgentResponseProvider>("cc.unitmesh.customAgentResponse")

        fun instance(name: String): List<AgentResponseProvider> {
            return EP_NAME.extensionList.filter { it.name == name }
        }
    }
}