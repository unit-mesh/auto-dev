package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.agenttool.browse.Browse
import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import com.intellij.openapi.project.Project

class BrowseInsCommand(val myProject: Project, private val prop: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.BROWSE

    override suspend fun execute(): String? {
        val parse = Browse.parse(prop)
        return parse.body
    }
}

