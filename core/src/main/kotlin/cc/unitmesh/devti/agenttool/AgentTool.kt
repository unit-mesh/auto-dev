package cc.unitmesh.devti.agenttool

data class AgentTool(val commandName: String, val description: String, val example: String) {
    override fun toString(): String {
//        val string = """desc: $description"""
        val string = if (description.isEmpty()) "" else """desc: $description"""
        return """<tool>name: ${commandName}, $string
example:
<devin>
$example
</devin>
</tool>"""
    }
}