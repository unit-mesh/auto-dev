package cc.unitmesh.devti.language.agent

interface AgentTool {
    fun execute(context: AgentContext) : ToolResult
}

data class ToolResult(
    val isSuccess: Boolean,
    val output: String? = null
) {

}
