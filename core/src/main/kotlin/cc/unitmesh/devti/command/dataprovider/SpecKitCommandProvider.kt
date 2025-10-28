package cc.unitmesh.devti.command.dataprovider

import cc.unitmesh.devti.AutoDevIcons
import com.intellij.openapi.project.Project
import java.nio.file.Path
import javax.swing.Icon
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
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
    val fullCommandName: String get() = "speckit.$subcommand"


    fun executeWithCompiler(project: Project, command: String, input: String): String {
        val compiler = SpecKitTemplateCompiler(project, template, command, input)
        return compiler.compile()
    }

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

        fun fromSubcommand(project: Project, subcommand: String): SpecKitCommand? {
            return all(project).find { it.subcommand == subcommand }
        }

        fun fromFullName(project: Project, commandName: String): SpecKitCommand? {
            return all(project).find { it.fullCommandName == commandName }
        }


        private fun extractDescription(template: String, subcommand: String): String {
            SkillFrontmatter.parse(template).let { (frontmatter, _) ->
                return frontmatter?.description ?: "SpecKit: $subcommand"
            }
        }

        fun isAvailable(project: Project): Boolean {
            val projectPath = project.basePath ?: return false
            val promptsDir = Path.of(projectPath, PROMPTS_DIR)
            return promptsDir.exists()
        }
    }
}

