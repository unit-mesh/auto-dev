package cc.unitmesh.devti.agenttool

data class AgentTool(val commandName: String, val description: String, val example: String) {
    override fun toString(): String =
        """<tool>name: ${commandName}, desc: $description, example:
<devin>
$example
</devin>
</tool>"""
}