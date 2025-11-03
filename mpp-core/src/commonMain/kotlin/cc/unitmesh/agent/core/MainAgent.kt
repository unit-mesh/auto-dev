package cc.unitmesh.agent.core

import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.agent.tool.ExecutableTool
import cc.unitmesh.agent.tool.ToolResult

abstract class MainAgent<TInput : Any, TOutput : ToolResult>(
    definition: AgentDefinition
) : Agent<TInput, TOutput>(definition) {
    protected val tools: MutableList<ExecutableTool<*, *>> = mutableListOf()

    fun registerTool(tool: ExecutableTool<*, *>) {
        tools.add(tool)
        sortToolsByPriority()
    }

    fun unregisterTool(tool: ExecutableTool<*, *>) {
        tools.remove(tool)
    }

    fun getAllTools(): List<ExecutableTool<*, *>> = tools.toList()

    fun getSubAgents(): List<SubAgent<*, *>> {
        return tools.filterIsInstance<SubAgent<*, *>>()
    }

    private fun sortToolsByPriority() {
        tools.sortBy { tool ->
            when (tool) {
                is SubAgent<*, *> -> tool.priority
                else -> Int.MAX_VALUE
            }
        }
    }

    open val maxIterations: Int = 100

    protected var currentIteration: Int = 0
}
