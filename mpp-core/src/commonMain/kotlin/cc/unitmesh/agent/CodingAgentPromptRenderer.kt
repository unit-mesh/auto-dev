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
}

