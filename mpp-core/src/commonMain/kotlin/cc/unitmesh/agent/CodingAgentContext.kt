package cc.unitmesh.agent

import cc.unitmesh.agent.context.AgentContextDiscovery
import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.tool.schema.AgentToolFormatter
import cc.unitmesh.agent.tool.ExecutableTool
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
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
 * @property agentRules Project-specific agent rules from AGENTS.md (auto-loaded if available)
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
        private val logger = getLogger("CodingAgentContext")
        
        /**
         * Create CodingAgentContext from AgentTask
         * 
         * @param task The agent task
         * @param toolList List of available tools
         * @param fileSystem Optional file system for AGENTS.md discovery (default: DefaultToolFileSystem)
         * @param loadAgentRules Whether to auto-load AGENTS.md files (default: true)
         * @param fallbackFilenames Additional filenames to search for (e.g., CLAUDE.md)
         * @param maxBytes Maximum bytes to load from context files (default: 32KB)
         * @return CodingAgentContext with formatted tool list and optional agent rules
         */
        suspend fun fromTask(
            task: AgentTask,
            toolList: List<ExecutableTool<*, *>>,
            fileSystem: ToolFileSystem? = null,
            loadAgentRules: Boolean = true,
            fallbackFilenames: List<String> = AgentContextDiscovery.DEFAULT_FALLBACK_FILENAMES,
            maxBytes: Int = AgentContextDiscovery.DEFAULT_MAX_BYTES
        ): CodingAgentContext {
            val agentRules = if (loadAgentRules) {
                try {
                    val fs = fileSystem ?: DefaultToolFileSystem(projectPath = task.projectPath)
                    val discovery = AgentContextDiscovery(fs, maxBytes)
                    val rules = discovery.loadAgentContext(task.projectPath, fallbackFilenames)
                    
                    if (rules.isNotEmpty()) {
                        logger.info { "Loaded agent rules from AGENTS.md (${rules.length} bytes)" }
                    } else {
                        logger.debug { "No AGENTS.md files found" }
                    }
                    
                    rules
                } catch (e: Exception) {
                    logger.warn { "Failed to load agent rules: ${e.message}" }
                    ""
                }
            } else {
                ""
            }
            
            return CodingAgentContext(
                projectPath = task.projectPath,
                osInfo = Platform.getOSInfo(),
                timestamp = Platform.getCurrentTimestamp().toString(),
                shell = Platform.getDefaultShell(),
                toolList = AgentToolFormatter.formatToolListForAI(toolList),
                agentRules = agentRules
            )
        }

        interface Builder {
            suspend fun build(projectPath: String, requirement: String): CodingAgentContext
        }
    }
}

