package cc.unitmesh.devti.agent.tool

import cc.unitmesh.devti.agent.tool.Tool
import kotlinx.serialization.Serializable

@Serializable
data class AgentTool(
    override val name: String, override val description: String,
    val example: String,
    val isMcp: Boolean = false,
    val completion: String = "",
    val mcpGroup: String = "",
    val isDevIns: Boolean = false,
    val devinScriptPath: String = "",
) : Tool {
    override fun toString(): String {
        val descAttr = if (description.isNotEmpty()) " description=\"$description\"" else ""
        val exampleContent = if (example.isNotEmpty()) """
    <example>
        <devin>$example</devin>
    </example>""" else ""
        
        return """<tool name="$name"$descAttr>$exampleContent
</tool>"""
    }
}