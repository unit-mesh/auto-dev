package cc.unitmesh.agent

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.devins.compiler.template.TemplateCompiler

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
        val logger = getLogger("CodingAgentPromptRenderer")

        val template = when (language.uppercase()) {
            "ZH", "CN" -> CodingAgentTemplate.ZH
            else -> CodingAgentTemplate.EN
        }

        val variableTable = context.toVariableTable()
        val toolListVar = variableTable.getVariable("toolList")
        if (toolListVar != null) {
            val toolListContent = toolListVar.value.toString()
            val hasBuiltinTools = listOf("read-file", "write-file", "grep", "glob", "shell")
                .any { toolListContent.contains("<tool name=\"$it\">") }
            logger.debug { "🔍 [CodingAgentPromptRenderer] 包含内置工具: $hasBuiltinTools" }
        } else {
            logger.warn { "❌ [CodingAgentPromptRenderer] 工具列表变量为空" }
        }

        val compiler = TemplateCompiler(variableTable)
        return compiler.compile(template)
    }
}
