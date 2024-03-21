package cc.unitmesh.devti.language.completion.dataprovider

import cc.unitmesh.devti.agent.model.CustomAgentConfig

/**
 * The tool hub provides a list of tools - agents and commands for the AI Agent to decide which one to call
 * For example, you prompt could be:
 * ```devin
 * Here is the tools you can use:
 * $agent
 * ```
 */
enum class ToolHubVariable(val summaryName: String, val type: String, val description: String) {
    AGENT("agent", CustomAgentConfig::class.simpleName.toString(), "DevIns all agent for AI Agent to call"),
    COMMAND("command", BuiltinCommand::class.simpleName.toString(), "DevIns all commands for AI Agent to call"),

    ;

    companion object {
        fun all(): List<ToolHubVariable> {
            return values().toList()
        }

        // fun examples from resources
    }
}