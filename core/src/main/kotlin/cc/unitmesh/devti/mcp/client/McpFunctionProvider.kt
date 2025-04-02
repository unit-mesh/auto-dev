package cc.unitmesh.devti.mcp.client

import cc.unitmesh.devti.agent.tool.AgentTool
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import io.modelcontextprotocol.kotlin.sdk.Tool.Input
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class McpFunctionProvider : ToolchainFunctionProvider {
    override suspend fun funcNames(): List<String> {
        val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return emptyList()
        return CustomMcpServerManager.instance(project).collectServerInfos().map { it.name }
    }

    override suspend fun toolInfos(project: Project): List<AgentTool> {
        val manager = CustomMcpServerManager.instance(project)
        return manager.collectServerInfos().map {
            val schemaJson = Json.encodeToString<Input>(it.inputSchema)
            val mockData = Json.encodeToString(MockDataGenerator.generateMockData(it.inputSchema))
            AgentTool(
                it.name,
                it.description ?: "",
                "Here is command and JSON schema\n/${it.name}\n```json\n$schemaJson\n```",
                isMcp = true,
                mcpGroup = it.name,
                completion = mockData
            )
        }
    }

    override suspend fun isApplicable(project: Project, funcName: String): Boolean {
        return CustomMcpServerManager.instance(project).collectServerInfos().any { it.name == funcName }
    }

    override suspend fun execute(
        project: Project,
        prop: String,
        args: List<Any>,
        allVariables: Map<String, Any?>,
        commandName: String
    ): Any {
        val tool = CustomMcpServerManager.instance(project).collectServerInfos().firstOrNull { it.name == commandName }
        if (tool == null) {
            return "No MCP such tool: $prop"
        }

        val arg = args.firstOrNull().toString()
        return CustomMcpServerManager.instance(project).execute(project, tool, arg)
    }
}
