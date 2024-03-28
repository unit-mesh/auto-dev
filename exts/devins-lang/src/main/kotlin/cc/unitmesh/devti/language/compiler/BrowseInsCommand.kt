package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.language.agenttool.browse.BrowseTool
import cc.unitmesh.devti.language.compiler.exec.InsCommand
import com.intellij.openapi.project.Project

class BrowseInsCommand(val myProject: Project, val prop: String) : InsCommand {
    override suspend fun execute(): String? {
        val documentContent = BrowseTool.parse(prop)
        return documentContent.body
    }
}
