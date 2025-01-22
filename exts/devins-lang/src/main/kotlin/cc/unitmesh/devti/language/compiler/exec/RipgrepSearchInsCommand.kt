package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.agenttool.RipgrepSearcher
import com.intellij.openapi.project.Project

/// https://github.com/MituuZ/fuzzier
class RipgrepSearchInsCommand(
    val myProject: Project, private val scope: String, val text: String?,
) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.RIPGREP_SEARCH

    override fun isApplicable(): Boolean {
        return RipgrepSearcher.findRipgrepBinary() != null
    }

    override suspend fun execute(): String? {
        val searchDirectory = myProject.baseDir!!.path
        return RipgrepSearcher.searchFiles(myProject, searchDirectory, text ?: scope, null).get()
    }
}