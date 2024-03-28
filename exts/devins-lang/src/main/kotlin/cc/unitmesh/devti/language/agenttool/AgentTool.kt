package cc.unitmesh.devti.language.agenttool

import com.intellij.openapi.extensions.ExtensionPointName

interface AgentTool {
    val name: String
    val description: String
    fun execute(context: AgentToolContext): AgentToolResult

    // extension point
    companion object {
        val EP_NAME = ExtensionPointName<AgentTool>("devins.agentTool")

        fun allTools(): List<AgentTool> {
            return EP_NAME.extensionList
        }
    }
}
