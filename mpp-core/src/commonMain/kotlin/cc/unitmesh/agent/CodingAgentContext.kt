package cc.unitmesh.agent

import cc.unitmesh.agent.tool.ExecutableTool
import cc.unitmesh.devins.compiler.variable.VariableTable

/**
 * Coding Agent Context - provides context information for autonomous coding agent
 * Similar to SketchRunContext in JetBrains plugin
 * 
 * This context is used to render system prompts for the coding agent
 * 
 * @property currentFile Current file being edited
 * @property projectPath Absolute path to the project workspace
 * @property projectStructure Summary of project structure
 * @property osInfo Operating system information
 * @property timestamp Current timestamp
 * @property toolList Available tools for the agent (as formatted string)
 * @property agentRules Project-specific agent rules from AGENTS.md
 * @property buildTool Build tool information (e.g., "gradle + kotlin")
 * @property shell Shell path (e.g., "/bin/bash")
 */
data class CodingAgentContext(
    val currentFile: String? = null,
    val projectPath: String,
    val projectStructure: String = "",
    val osInfo: String,
    val timestamp: String,
    val toolList: String = "",
    val agentRules: String = "",
    val buildTool: String = "",
    val shell: String = "/bin/bash",
    val moduleInfo: String = "",
    val frameworkContext: String = "",
) : AgentContext {
    /**
     * Convert context to variable table for template compilation
     */
    override fun toVariableTable(): VariableTable {
        val table = VariableTable()
        table.addVariable("currentFile", cc.unitmesh.devins.compiler.variable.VariableType.STRING, currentFile ?: "")
        table.addVariable("projectPath", cc.unitmesh.devins.compiler.variable.VariableType.STRING, projectPath)
        table.addVariable("projectStructure", cc.unitmesh.devins.compiler.variable.VariableType.STRING, projectStructure)
        table.addVariable("osInfo", cc.unitmesh.devins.compiler.variable.VariableType.STRING, osInfo)
        table.addVariable("timestamp", cc.unitmesh.devins.compiler.variable.VariableType.STRING, timestamp)
        table.addVariable("toolList", cc.unitmesh.devins.compiler.variable.VariableType.STRING, toolList)
        table.addVariable("agentRules", cc.unitmesh.devins.compiler.variable.VariableType.STRING, agentRules)
        table.addVariable("buildTool", cc.unitmesh.devins.compiler.variable.VariableType.STRING, buildTool)
        table.addVariable("shell", cc.unitmesh.devins.compiler.variable.VariableType.STRING, shell)
        table.addVariable("moduleInfo", cc.unitmesh.devins.compiler.variable.VariableType.STRING, moduleInfo)
        table.addVariable("frameworkContext", cc.unitmesh.devins.compiler.variable.VariableType.STRING, frameworkContext)
        return table
    }
    
    companion object {
        /**
         * Create CodingAgentContext from AgentTask
         * 
         * @param task The agent task
         * @param toolList List of available tools
         * @return CodingAgentContext with formatted tool list
         */
        fun fromTask(task: AgentTask, toolList: List<ExecutableTool<*, *>>): CodingAgentContext {
            return CodingAgentContext(
                projectPath = task.projectPath,
                osInfo = Platform.getOSInfo(),
                timestamp = Platform.getCurrentTimestamp().toString(),
                shell = Platform.getDefaultShell(),
                toolList = AgentToolFormatter.formatToolListForAI(toolList)
            )
        }

        interface Builder {
            suspend fun build(projectPath: String, requirement: String): CodingAgentContext
        }
    }
}

