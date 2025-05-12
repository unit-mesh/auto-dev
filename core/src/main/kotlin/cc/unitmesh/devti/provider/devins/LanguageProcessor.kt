package cc.unitmesh.devti.provider.devins

import cc.unitmesh.devti.agent.custom.model.CustomAgentConfig
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread

data class CustomAgentContext(
    val config: CustomAgentConfig,
    val response: String,
    val filePath: VirtualFile? = null,
    var initVariables: Map<String, String> = mapOf()
)

/**
 * Handle the response of the custom agent, and return the result to the user.
 * Specify for [cc.unitmesh.devti.language.DevInLanguage]
 */
interface LanguageProcessor {
    val name: String

    @RequiresBackgroundThread
    suspend fun execute(project: Project, context: CustomAgentContext): String

    @RequiresBackgroundThread
    suspend fun compile(project: Project, text: String): String

    @RequiresBackgroundThread
    suspend fun transpileCommand(project: Project, psiFile: PsiFile): List<BuiltinCommand>

    companion object {
        val EP_NAME = ExtensionPointName<LanguageProcessor>("cc.unitmesh.languageProcessor")

        fun devin(): LanguageProcessor? {
            return EP_NAME.extensionList.firstOrNull { it.name == "DevIn" }
        }
    }
}