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
interface LanguagePromptProcessor {
    val name: String

    @RequiresBackgroundThread
    fun execute(project: Project, context: CustomAgentContext): String

    @RequiresBackgroundThread
    fun compile(project: Project, text: String): String

    companion object {
        val EP_NAME = ExtensionPointName<LanguagePromptProcessor>("cc.unitmesh.languageProcessor")

        fun instance(languageName: String): List<LanguagePromptProcessor> {
            return EP_NAME.extensionList.filter { it.name == languageName }
        }
    }
}