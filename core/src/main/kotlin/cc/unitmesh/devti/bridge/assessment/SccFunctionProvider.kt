package cc.unitmesh.devti.bridge.assessment

import cc.unitmesh.devti.bridge.Assessment
import cc.unitmesh.devti.agent.tool.linecount.SccResult
import cc.unitmesh.devti.agent.tool.linecount.SccWrapper
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.NlsSafe

class SccFunctionProvider : ToolchainFunctionProvider {
    override suspend fun isApplicable(project: Project, funcName: String): Boolean = funcName == Assessment.SCC.name

    override suspend fun funcNames(): List<String> = listOf(Assessment.SCC.name)

    override suspend fun execute(
        project: Project,
        prop: String,
        args: List<Any>,
        allVariables: Map<String, Any?>,
        commandName: @NlsSafe String
    ): Any {
        val baseDir = project.guessProjectDir()!!.path
        val path = if (prop.isEmpty()) {
            baseDir
        } else "$baseDir/$prop"

        var items = emptyList<SccResult>()
        val thread = ApplicationManager.getApplication().executeOnPooledThread {
            items = SccWrapper().runSync(path)
        }

        thread.get(30000, java.util.concurrent.TimeUnit.MILLISECONDS)

        if (items.isEmpty()) {
            return "No files found"
        }

        return "Here is project's code summary:\n" + format(items)
    }

    private fun format(items: List<SccResult>): String {
        val table = StringBuilder()
        table.append("| Name | Lines | Code |Complexity |\n")
        table.append("|------|--------|-------|------|\n")
        items.forEach {
            table.append("| ${it.name} | ${it.lines} | ${it.code} | ${it.complexity}  |\n")
        }

        return table.toString()
    }
}
