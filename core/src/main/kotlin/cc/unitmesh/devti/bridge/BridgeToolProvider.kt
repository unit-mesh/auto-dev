package cc.unitmesh.devti.bridge

import cc.unitmesh.devti.agenttool.AgentTool
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand.*
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.project.Project

object BridgeToolProvider {
    val Tools =
        setOf(RELATED, STRUCTURE, LOCAL_SEARCH, RIPGREP_SEARCH, DATABASE, DIR, REV)

    fun collect(project: Project): List<AgentTool> {
        val commonTools = Tools
            .map {
                val example = BuiltinCommand.example(it)
                AgentTool(it.commandName, it.description, example)
            }.toMutableList()

        val functions = ToolchainFunctionProvider.all()

        commonTools += functions.map {
            if (it.toolInfo() != null) {
                return@map listOf(it.toolInfo()!!)
            }

            val funcNames = it.funcNames()
            funcNames.map { name ->
                val example = BuiltinCommand.example(name)
                AgentTool(name, "", example)
            }
        }.flatten()

        return commonTools
    }
}