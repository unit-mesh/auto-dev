package cc.unitmesh.devti.agenttool

data class AgentTool(val commandName: String, val description: String, val example: String) {
    override fun toString(): String {
        val string = if (description.isEmpty()) "" else """desc: $description"""
        val example = if (example.isEmpty()) "" else """example:
<devin>
$example
</devin>"""
        return """<tool>name: ${commandName}, $string, $example
</tool>"""
    }
}