package cc.unitmesh.devti.command.dataprovider

import cc.unitmesh.devti.AutoDevIcons
import com.intellij.openapi.project.Project
import java.nio.file.Path
import javax.swing.Icon
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText

/**
 * SpecKitCommand represents a GitHub Spec-Kit command loaded from .github/prompts/ directory.
 * 
 * Each command corresponds to a prompt file like `speckit.clarify.prompt.md` and can be used
 * as `/speckit.clarify <arguments>` in DevIns scripts.
 */
data class SpecKitCommand(
    val subcommand: String,
    val description: String,
    val template: String,
    val icon: Icon = AutoDevIcons.IDEA
) {
    val fullCommandName: String
        get() = "speckit.$subcommand"

    /**
     * Execute the command by replacing $ARGUMENTS placeholder with actual arguments
     */
    fun execute(arguments: String): String {
        return template.replace("\$ARGUMENTS", arguments)
    }

    /**
     * Convert to CustomCommand for compatibility with existing DevIns infrastructure
     */
    fun toCustomCommand(): CustomCommand {
        return CustomCommand(
            commandName = fullCommandName,
            content = description,
            icon = icon
        )
    }

    companion object {
        private const val PROMPTS_DIR = ".github/prompts"
        private const val SPECKIT_PREFIX = "speckit."
        private const val PROMPT_SUFFIX = ".prompt.md"

        /**
         * Load all SpecKit commands from .github/prompts/ directory
         */
        fun all(project: Project): List<SpecKitCommand> {
            val projectPath = project.basePath ?: return emptyList()
            val promptsDir = Path.of(projectPath, PROMPTS_DIR)
            
            if (!promptsDir.exists()) {
                return emptyList()
            }

            return try {
                promptsDir.listDirectoryEntries("$SPECKIT_PREFIX*$PROMPT_SUFFIX")
                    .mapNotNull { promptFile ->
                        try {
                            val fileName = promptFile.fileName.toString()
                            // Extract subcommand from "speckit.clarify.prompt.md" -> "clarify"
                            val subcommand = fileName
                                .removePrefix(SPECKIT_PREFIX)
                                .removeSuffix(PROMPT_SUFFIX)

                            if (subcommand.isEmpty()) return@mapNotNull null

                            val template = promptFile.readText()
                            val description = extractDescription(template, subcommand)

                            SpecKitCommand(
                                subcommand = subcommand,
                                description = description,
                                template = template
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
            } catch (e: Exception) {
                emptyList()
            }
        }

        /**
         * Find a specific SpecKit command by subcommand name
         */
        fun fromSubcommand(project: Project, subcommand: String): SpecKitCommand? {
            return all(project).find { it.subcommand == subcommand }
        }

        /**
         * Extract description from prompt template.
         * Looks for the first paragraph or heading in the markdown file.
         */
        private fun extractDescription(template: String, subcommand: String): String {
            val lines = template.lines()
            
            // Try to find first heading or paragraph
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("---")) continue
                
                // Extract from markdown heading
                if (trimmed.startsWith("#")) {
                    return trimmed.removePrefix("#").trim()
                }
                
                // Use first non-empty line as description
                if (trimmed.isNotEmpty()) {
                    return trimmed.take(100) // Limit to 100 chars
                }
            }
            
            // Fallback to formatted subcommand name
            return "Spec-Kit ${subcommand.replaceFirstChar { it.uppercase() }}"
        }

        /**
         * Check if SpecKit is available in the project
         */
        fun isAvailable(project: Project): Boolean {
            val projectPath = project.basePath ?: return false
            val promptsDir = Path.of(projectPath, PROMPTS_DIR)
            return promptsDir.exists()
        }
    }
}

