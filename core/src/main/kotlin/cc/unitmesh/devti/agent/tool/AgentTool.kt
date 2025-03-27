package cc.unitmesh.devti.agent.tool

import cc.unitmesh.devti.agent.Tool
import kotlinx.serialization.Serializable

@Serializable
data class AgentTool(
    override val name: String, override val description: String,
    val example: String,
    val isMcp: Boolean = false
) : Tool {
    override fun toString(): String {
        val string = if (description.isEmpty()) "" else """desc: $description"""
        val example = if (example.isEmpty()) "" else """example:
<devin>
$example
</devin>"""
        return """<tool>name: ${name}, $string, $example
</tool>"""
    }
}