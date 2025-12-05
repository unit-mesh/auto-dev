package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.sketch.SketchToolchainProvider
import cc.unitmesh.devti.agent.tool.AgentTool

class DevInsSketchToolchainProvider : SketchToolchainProvider {
    override fun collect(): List<AgentTool> {
        /// we need to ignore some bad case for llm
        return BuiltinCommand.all()
            .filter {
                it.enableInSketch
            }
            .map {
            val example = BuiltinCommand.example(it)
            AgentTool(it.commandName, it.description, example)
        }
    }
}
