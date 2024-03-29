package cc.unitmesh.devti.language.provider

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.language.compiler.DevInsCompiler
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.provider.devins.LanguagePromptProcessor
import cc.unitmesh.devti.provider.devins.CustomAgentContext
import com.intellij.openapi.project.Project


class DevInsCustomAgentResponse : LanguagePromptProcessor {
    override val name: String = "DevIn"

    override fun execute(project: Project, context: CustomAgentContext): String {
        val devInFile = DevInFile.fromString(project, context.response)
        val devInsCompiler = DevInsCompiler(project, devInFile)

        val result = devInsCompiler.compile()
        AutoDevNotifications.notify(project, result.output)
        return result.output
    }

    override fun compile(project: Project, text: String): String {
        val devInFile = DevInFile.fromString(project, text)
        val devInsCompiler = DevInsCompiler(project, devInFile)

        val result = devInsCompiler.compile()
        return if (result.hasError) {
            text
        } else {
            result.output
        }
    }
}
