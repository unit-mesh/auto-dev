package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import com.intellij.openapi.project.Project
import cc.unitmesh.devti.util.parser.CodeFence
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider

class DatabaseInsCommand(val myProject: Project, private val prop: String, private val codeContent: String?) :
    InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.DATABASE

    override fun isApplicable(): Boolean {
        return  ToolchainFunctionProvider.lookup("DatabaseFunctionProvider") != null
    }

    override suspend fun execute(): String {
        val args = if (codeContent != null) {
            val code = CodeFence.parse(codeContent).text
            listOf(code)
        } else {
            listOf()
        }

        val result = try {
            ToolchainFunctionProvider.lookup("DatabaseFunctionProvider")
                ?.execute(myProject, prop, args, emptyMap())
        } catch (e: Exception) {
            AutoDevNotifications.notify(myProject, "Error: ${e.message}")
            return "Error: ${e.message}"
        }

        return result?.toString() ?: "No database provider found"
    }
}
