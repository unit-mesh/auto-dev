package cc.unitmesh.devti.mcp.client

import cc.unitmesh.devti.agent.tool.AgentTool
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.Tool.Input
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class McpFunctionProvider : ToolchainFunctionProvider {
    override suspend fun funcNames(): List<String> {
        val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return emptyList()
        return CustomMcpServerManager.instance(project).collectServerInfos().values
            .flatMap { it }
            .map { it.name }
            .distinct()
    }

    override suspend fun toolInfos(project: Project): List<AgentTool> {
        val manager = CustomMcpServerManager.instance(project)
        val toolsMap = manager.collectServerInfos()
        
        val agentTools = mutableListOf<AgentTool>()
        for ((serverName, tools) in toolsMap) {
            for (tool in tools) {
                val agentTool = toAgentTool(tool, serverName)

                agentTools.add(agentTool)
            }
        }
        
        return agentTools
    }

    override suspend fun isApplicable(project: Project, funcName: String): Boolean {
        val toolsMap = CustomMcpServerManager.instance(project).collectServerInfos()
        return toolsMap.any { (_, tools) -> tools.any { it.name == funcName } }
    }

    override suspend fun execute(
        project: Project,
        prop: String,
        args: List<Any>,
        allVariables: Map<String, Any?>,
        commandName: String
    ): Any {
        val toolsMap = CustomMcpServerManager.instance(project).collectServerInfos()
        val tool = toolsMap.values.flatMap { it }
            .firstOrNull { it.name == commandName }
            
        if (tool == null) {
            return "No MCP such tool: $prop"
        }

        val arg = args.firstOrNull().toString()
        return CustomMcpServerManager.instance(project).execute(project, tool, arg)
    }
}

fun toAgentTool(
    tool: Tool,
    serverName: String
): AgentTool {
    val schemaJson = Json.encodeToString<Input>(tool.inputSchema)
    val mockData = Json.encodeToString(MockDataGenerator.generateMockData(tool.inputSchema))
    val agentTool = AgentTool(
        tool.name,
        tool.description ?: "",
        "Here is command and JSON schema\n/${tool.name}\n```json\n$schemaJson\n```",
        isMcp = true,
        mcpGroup = serverName,
        completion = mockData
    )
    return agentTool
}
