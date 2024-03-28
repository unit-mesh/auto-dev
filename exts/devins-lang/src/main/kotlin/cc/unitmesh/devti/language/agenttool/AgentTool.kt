package cc.unitmesh.devti.language.agenttool

interface AgentTool {
    fun execute(context: AgentToolContext) : AgentToolResult
}

