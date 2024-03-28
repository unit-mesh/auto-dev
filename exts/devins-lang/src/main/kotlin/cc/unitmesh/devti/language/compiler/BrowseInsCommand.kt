package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.language.agenttool.browse.BrowseTool
import cc.unitmesh.devti.language.compiler.exec.InsCommand
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project

class BrowseInsCommand(val myProject: Project, val prop: String) : InsCommand {
    override suspend fun execute(): String? {
        var body: String? = null
        runInEdt {
            val parse = BrowseTool.parse(prop)
            body = parse.body
        }

        return body
    }
}

