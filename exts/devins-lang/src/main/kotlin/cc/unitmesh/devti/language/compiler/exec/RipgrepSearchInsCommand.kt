package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.agent.tool.search.RipgrepSearcher
import com.intellij.openapi.project.Project

/// https://github.com/MituuZ/fuzzier
class RipgrepSearchInsCommand(val myProject: Project, private val scope: String, val text: String?) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.RIPGREP_SEARCH

    override fun isApplicable(): Boolean {
        return try {
            RipgrepSearcher.findRipgrepBinary() != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun execute(): String? {
        val searchDirectory = myProject.baseDir!!.path
        val searchContent = text ?: scope
        val result = RipgrepSearcher.searchFiles(myProject, searchDirectory, searchContent, null).get()
        return if (result?.isNotEmpty() == true) {
            "RipGrep Search Result ($searchContent): \n===============================\n$result\n===============================\n"
        } else {
            "No result found for /$commandName:$searchContent"
        }
    }
}