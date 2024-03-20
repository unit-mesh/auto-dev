package cc.unitmesh.devti.language.provider

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.language.DevInLanguage
import cc.unitmesh.devti.language.compiler.DevInsCompiler
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.provider.devins.AgentResponseProvider
import cc.unitmesh.devti.provider.devins.CustomAgentContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import java.util.*


class DevInsCustomAgentResponse : AgentResponseProvider {
    override val name: String = "DevIn"

    override fun execute(project: Project, context: CustomAgentContext): String {
        val filename = "DevIns-${UUID.randomUUID()}.devin"

        val devInFile = PsiFileFactory.getInstance(project)
            .createFileFromText(filename, DevInLanguage, context.response) as DevInFile

        val devInsCompiler = DevInsCompiler(project, devInFile)
        val result = devInsCompiler.compile()

        AutoDevNotifications.notify(project, result.output)
        return result.output
    }
}
