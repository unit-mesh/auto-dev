package cc.unitmesh.devti.language.provider

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

class AgentToolContext(
    val project: Project,
    val argument: String
) {

}

data class AgentToolResult(
    val isSuccess: Boolean,
    val output: String? = null
)

interface AgentTool {
    val name: String
    val description: String
    fun execute(context: AgentToolContext): AgentToolResult

    // extension point
    companion object {
        private val EP_NAME = ExtensionPointName<AgentTool>("cc.unitmesh.shireAgentTool")

        fun allTools(): List<AgentTool> {
            return EP_NAME.extensionList
        }
    }
}
