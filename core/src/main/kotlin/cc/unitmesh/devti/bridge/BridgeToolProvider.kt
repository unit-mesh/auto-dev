package cc.unitmesh.devti.bridge

import cc.unitmesh.devti.agenttool.AgentTool
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand.*
import com.intellij.openapi.project.Project

object BridgeToolProvider {
    val Tools =
        setOf(RELATED, STRUCTURE, LOCAL_SEARCH, RIPGREP_SEARCH, DATABASE, DIR, REV)

    fun collect(project: Project): List<AgentTool> {
        val commonTools = Tools
            .map {
                val example = BuiltinCommand.example(it)
                AgentTool(it.commandName, it.description, example)
            }

        /// collect function tools in Bridge.kt

        return commonTools
    }
}