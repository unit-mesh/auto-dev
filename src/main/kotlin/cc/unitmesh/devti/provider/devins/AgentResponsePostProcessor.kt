package cc.unitmesh.devti.provider.devins

import cc.unitmesh.devti.agent.model.CustomAgentConfig
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread

data class CustomAgentContext(
    val config: CustomAgentConfig,
    val response: String
)

/**
 * Handle the response of the custom agent, and return the result to the user.
 * Specify for [cc.unitmesh.devti.language.DevInLanguage]
 */
interface AgentResponsePostProcessor {
    val name: String

    @RequiresBackgroundThread
    fun execute(project: Project, context: CustomAgentContext): String

    companion object {
        val EP_NAME = ExtensionPointName<AgentResponsePostProcessor>("cc.unitmesh.customAgentResponse")

        fun instance(languageName: String): List<AgentResponsePostProcessor> {
            return EP_NAME.extensionList.filter { it.name == languageName }
        }
    }
}