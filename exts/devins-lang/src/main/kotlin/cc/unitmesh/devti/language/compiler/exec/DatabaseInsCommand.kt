package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import com.intellij.openapi.project.Project
import cc.unitmesh.devti.util.parser.CodeFence
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider

class DatabaseInsCommand(val myProject: Project, private val prop: String, private val codeContent: String?) :
    InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.DATABASE

    override suspend fun execute(): String {
        val args = if (codeContent != null) {
            val code = CodeFence.parse(codeContent).text
            listOf(code)
        } else {
            listOf()
        }

        val result = ToolchainFunctionProvider.lookup("DatabaseFunctionProvider")
            ?.execute(myProject, prop, args, emptyMap())

        return result?.toString() ?: "No database provider found"
    }
}
