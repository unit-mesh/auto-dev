package cc.unitmesh.devti.language.compiler.exec.claudeskill

import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.command.dataprovider.ClaudeSkillCommand
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager

/**
 * Claude Skill command implementation for Agent Skills.
 *
 * Claude Skills are organized folders of instructions, scripts, and resources that agents
 * can discover and load dynamically to perform better at specific tasks.
 *
 * Supports subcommands like:
 * - /skill.pdf <arguments>
 * - /skill.algorithmic-art <arguments>
 * - /skill.artifacts-builder <arguments>
 *
 * Example:
 * <devin>
 * /skill.pdf Fill out the form in document.pdf
 * </devin>
 *
 * <devin>
 * /skill.algorithmic-art Create a generative art piece
 * </devin>
 */
class ClaudeSkillInsCommand(
    private val project: Project,
    private val prop: String,
    private val arguments: String
) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.CLAUDE_SKILL

    private val logger = logger<ClaudeSkillInsCommand>()

    override fun isApplicable(): Boolean {
        return ClaudeSkillCommand.isAvailable(project)
    }

    override suspend fun execute(): String? {
        // Parse skill name from prop (e.g., "skill.pdf" -> "pdf")
        val skillName = parseSkillName(prop)
        if (skillName.isEmpty()) {
            return "$DEVINS_ERROR Invalid skill command format. Use /skill.<skillname> <arguments>"
        }

        // Load the Claude Skill command
        val claudeSkill = ClaudeSkillCommand.fromSkillName(project, skillName)
        if (claudeSkill == null) {
            val availableSkills = ClaudeSkillCommand.all(project)
                .joinToString(", ") { it.skillName }
            return "$DEVINS_ERROR Skill not found: $skillName\n" +
                    "Available skills: $availableSkills"
        }

        try {
            // Execute the command with the compiler for proper variable resolution
            val result = claudeSkill.executeWithCompiler(project, arguments, prop)

            // Refresh VFS to ensure file changes are visible
            VirtualFileManager.getInstance().refreshWithoutFileWatcher(false)

            return result
        } catch (e: Exception) {
            logger.error("Error executing Claude skill: $skillName", e)
            return "$DEVINS_ERROR Error executing skill.$skillName: ${e.message}"
        }
    }

    /**
     * Parse skill name from prop string.
     * Examples:
     * - "skill.pdf" -> "pdf"
     * - "pdf" -> "pdf"
     * - ".pdf" -> "pdf"
     */
    private fun parseSkillName(prop: String): String {
        val trimmed = prop.trim()
        
        // Handle "skill.pdf" format
        if (trimmed.startsWith("skill.")) {
            return trimmed.removePrefix("skill.")
        }
        
        // Handle ".pdf" format
        if (trimmed.startsWith(".")) {
            return trimmed.removePrefix(".")
        }
        
        // Handle "pdf" format directly
        return trimmed
    }
}

