package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.agenttool.browse.BrowseTool
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project

class BrowseInsCommand(val myProject: Project, private val prop: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.BROWSE
    override suspend fun execute(): String? {
        var body: String? = null
        runInEdt {
            val parse = BrowseTool.parse(prop)
            body = parse.body
        }

        return body
    }
}

