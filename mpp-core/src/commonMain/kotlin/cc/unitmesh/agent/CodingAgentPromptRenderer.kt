package cc.unitmesh.agent

import cc.unitmesh.agent.tool.Tool
import cc.unitmesh.agent.Platform
import cc.unitmesh.devins.compiler.template.TemplateCompiler
import cc.unitmesh.devins.compiler.variable.VariableTable
import cc.unitmesh.devins.compiler.variable.VariableType
import cc.unitmesh.devins.compiler.variable.VariableScope
import kotlinx.datetime.Clock

/**
 * Renders system prompts for the coding agent using templates and context
 * 
 * This class bridges CodingAgentContext with the template system,
 * similar to how SketchRunContext is used with sketch.vm in the JetBrains plugin
 */
class CodingAgentPromptRenderer {
    
    /**
     * Render system prompt from context
     *
     * @param context The coding agent context
     * @param language Language for the prompt (EN or ZH)
     * @return The rendered system prompt
     */
    fun render(context: CodingAgentContext, language: String = "EN"): String {
        // Get template based on language
        val template = when (language.uppercase()) {
            "ZH", "CN" -> CodingAgentTemplate.ZH
            else -> CodingAgentTemplate.EN
        }

        // Convert context to variable table
        val variableTable = context.toVariableTable()

        // Compile template
        val compiler = TemplateCompiler(variableTable)
        return compiler.compile(template)
    }

    /**
     * Render system prompt with dynamic tool list
     *
     * @param tools Available tools for the agent
     * @param language Language for the prompt (EN or ZH)
     * @return The rendered system prompt
     */
    fun renderSystemPrompt(tools: List<Tool>, language: String = "EN"): String {
        // Get template based on language
        val template = when (language.uppercase()) {
            "ZH", "CN" -> CodingAgentTemplate.ZH
            else -> CodingAgentTemplate.EN
        }

        // Create variable table with tools and default values
        val variableTable = VariableTable()

        // Add tool information for ${toolList}
        val toolDescriptions = tools.map { tool ->
            "- **${tool.name}**: ${tool.description}"
        }.joinToString("\n")

        variableTable.addVariable("toolList", VariableType.STRING, toolDescriptions, VariableScope.USER_DEFINED)
        variableTable.addVariable("toolNames", VariableType.ARRAY, tools.map { it.name }, VariableScope.USER_DEFINED)

        // Add default values for other template variables
        variableTable.addVariable("osInfo", VariableType.STRING, Platform.getOSName(), VariableScope.BUILTIN)
        variableTable.addVariable("projectPath", VariableType.STRING, "/project", VariableScope.BUILTIN)
        variableTable.addVariable("timestamp", VariableType.STRING, Clock.System.now().toString(), VariableScope.BUILTIN)
        variableTable.addVariable("currentFile", VariableType.STRING, "", VariableScope.BUILTIN)
        variableTable.addVariable("buildTool", VariableType.STRING, "Auto-detected", VariableScope.BUILTIN)
        variableTable.addVariable("shell", VariableType.STRING, Platform.getDefaultShell(), VariableScope.BUILTIN)
        variableTable.addVariable("projectStructure", VariableType.STRING, "Will be analyzed during execution", VariableScope.BUILTIN)

        // Compile template
        val compiler = TemplateCompiler(variableTable)
        return compiler.compile(template)
    }
}
