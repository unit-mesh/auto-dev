package cc.unitmesh.devti.devin.dataprovider

import cc.unitmesh.devti.agent.model.CustomAgentConfig
import com.intellij.openapi.project.Project

/**
 * The tool hub provides a list of tools - agents and commands for the AI Agent to decide which one to call
 * For example, you prompt could be:
 * ```devin
 * Here is the tools you can use:
 * $agents
 * ```
 *
 * Or
 *
 * ```devin
 * Here is the tools you can use:
 * $commands
 * ```
 */
enum class ToolHubVariable(val hubName: String, val type: String, val description: String) {
    AGENTS("agents", CustomAgentConfig::class.simpleName.toString(), "DevIns all agent for AI Agent to call"),
    COMMANDS("commands", BuiltinCommand::class.simpleName.toString(), "DevIns all commands for AI Agent to call"),

    ;

    companion object {
        fun all(): List<ToolHubVariable> {
            return values().toList()
        }

        /**
         * @param variableId should be one of the [ToolHubVariable] name
         */
        fun lookup(myProject: Project, variableId: String?): List<String> {
            return when (variableId) {
                AGENTS.hubName -> CustomAgentConfig.loadFromProject(myProject).map {
                    "- " + it.name + ". " + it.description
                }
                COMMANDS.hubName -> BuiltinCommand.all().map {
                    "- " + it.commandName + ". " + it.description
                }
                else -> emptyList()
            }
        }
    }
}
