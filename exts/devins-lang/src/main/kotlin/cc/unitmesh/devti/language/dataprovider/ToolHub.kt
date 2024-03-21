package cc.unitmesh.devti.language.dataprovider

import cc.unitmesh.devti.agent.model.CustomAgentConfig

enum class ToolHub(val summaryName: String, val type: String, val description: String) {
    AGENT("Agent", CustomAgentConfig::class.simpleName.toString(), "DevIns all agent for AI Agent to call"),
    COMMAND("Command", BuiltinCommand::class.simpleName.toString(), "DevIns all commands for AI Agent to call"),

    ;

    companion object {
        fun all(): List<ToolHub> {
            return values().toList()
        }

        // fun examples from resources
    }
}