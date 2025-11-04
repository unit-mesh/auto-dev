package cc.unitmesh.agent

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
        val template = when (language.uppercase()) {
            "ZH", "CN" -> CodingAgentTemplate.ZH
            else -> CodingAgentTemplate.EN
        }

        val variableTable = context.toVariableTable()

        // 🔍 调试：检查工具列表变量
        val toolListVar = variableTable.getVariable("toolList")
        if (toolListVar != null) {
            val toolListContent = toolListVar.value.toString()
            println("🔍 [CodingAgentPromptRenderer] 工具列表长度: ${toolListContent.length}")
            val toolCount = toolListContent.split("<tool name=").size - 1
            println("🔍 [CodingAgentPromptRenderer] 工具数量: $toolCount")

            // 检查是否包含内置工具
            val hasBuiltinTools = listOf("read-file", "write-file", "grep", "glob", "shell")
                .any { toolListContent.contains("<tool name=\"$it\">") }
            println("🔍 [CodingAgentPromptRenderer] 包含内置工具: $hasBuiltinTools")

            // 检查是否包含 SubAgent
            val hasSubAgents = listOf("error-recovery", "log-summary", "codebase-investigator")
                .any { toolListContent.contains("<tool name=\"$it\">") }
            println("🔍 [CodingAgentPromptRenderer] 包含 SubAgent: $hasSubAgents")
        } else {
            println("❌ [CodingAgentPromptRenderer] 工具列表变量为空")
        }

        val compiler = TemplateCompiler(variableTable)
        return compiler.compile(template)
    }
}
