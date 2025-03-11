package cc.unitmesh.devti.mcp.provider

import cc.unitmesh.devti.agenttool.AgentTool
import cc.unitmesh.devti.mcp.CustomMcpServerManager
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.NlsSafe
import io.modelcontextprotocol.kotlin.sdk.Tool.Input
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class McpFunctionProvider : ToolchainFunctionProvider {
    override fun funcNames(): List<String> {
        val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return emptyList()
        return CustomMcpServerManager.instance(project).collectServerInfos().map { it.name }
    }

    override fun toolInfos(): List<AgentTool> {
        val manager = CustomMcpServerManager.instance(
            ProjectManager.getInstance().openProjects.firstOrNull() ?: return emptyList()
        )
        return manager.collectServerInfos().map {
            val encodeToString = Json.encodeToString<Input>(it.inputSchema)
            AgentTool(
                it.name,
                it.description ?: "",
                "Here is command and JSON schema\n/${it.name}\n```json\n$encodeToString\n```"
            )
        }
    }

    override fun isApplicable(project: Project, funcName: String): Boolean {
        return CustomMcpServerManager.instance(project).collectServerInfos().any { it.name == funcName }
    }

    override fun execute(
        project: Project,
        prop: String,
        args: List<Any>,
        allVariables: Map<String, Any?>,
        commandName: @NlsSafe String
    ): Any {
        val tool = CustomMcpServerManager.instance(project).collectServerInfos().firstOrNull { it.name == commandName }
        if (tool == null) {
            return "No MCP such tool: $prop"
        }

        return CustomMcpServerManager.instance(project).execute(project, tool, args.firstOrNull().toString())
    }
}
