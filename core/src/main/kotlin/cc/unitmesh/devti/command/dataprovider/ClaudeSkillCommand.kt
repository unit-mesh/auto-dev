package cc.unitmesh.devti.command.dataprovider

import cc.unitmesh.devti.AutoDevIcons
import com.intellij.openapi.project.Project
import java.nio.file.Path
import javax.swing.Icon
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

/**
 * ClaudeSkillCommand represents a Claude Skill loaded from directories containing SKILL.md files.
 * 
 * Claude Skills are organized folders of instructions, scripts, and resources that agents can
 * discover and load dynamically. Each skill is a directory containing a SKILL.md file with
 * YAML frontmatter (name, description) and markdown content.
 * 
 * Skills can be located in:
 * - Project root directories containing SKILL.md
 * - ~/.claude/skills/ directory (user-level skills)
 * 
 * Example usage:
 * ```
 * /skill.pdf <arguments>
 * ```
 */
data class ClaudeSkillCommand(
    val skillName: String,
    val description: String,
    val template: String,
    val skillPath: Path,
    val icon: Icon = AutoDevIcons.IDEA
) {
    val fullCommandName: String
        get() = "skill.$skillName"

    /**
     * Execute the command using SkillTemplateCompiler for proper variable resolution.
     * This method:
     * 1. Parses frontmatter to extract variable definitions
     * 2. Resolves variables (e.g., loading file contents)
     * 3. Uses Velocity template engine for compilation
     *
     * @param project The current project
     * @param arguments User-provided arguments
     * @return Compiled template with all variables resolved
     */
    fun executeWithCompiler(project: Project, arguments: String): String {
        val compiler = SpecKitTemplateCompiler(project, template, arguments)
        return compiler.compile()
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
        private const val SKILL_FILE = "SKILL.md"
        private const val USER_SKILLS_DIR = ".claude/skills"

        /**
         * Load all Claude Skills from available locations:
         * 1. Project root directories containing SKILL.md
         * 2. User home ~/.claude/skills/ directory
         */
        fun all(project: Project): List<ClaudeSkillCommand> {
            val skills = mutableListOf<ClaudeSkillCommand>()
            
            // Load from project root
            skills.addAll(loadFromProjectRoot(project))
            
            // Load from user skills directory
            skills.addAll(loadFromUserSkillsDir())
            
            return skills
        }

        /**
         * Load skills from project root directories
         */
        private fun loadFromProjectRoot(project: Project): List<ClaudeSkillCommand> {
            val projectPath = project.basePath ?: return emptyList()
            val projectRoot = Path.of(projectPath)
            
            if (!projectRoot.exists()) {
                return emptyList()
            }

            return try {
                projectRoot.listDirectoryEntries()
                    .filter { it.isDirectory() }
                    .mapNotNull { dir ->
                        val skillFile = dir.resolve(SKILL_FILE)
                        if (skillFile.exists()) {
                            loadSkillFromFile(skillFile, dir)
                        } else {
                            null
                        }
                    }
            } catch (e: Exception) {
                emptyList()
            }
        }

        /**
         * Load skills from user's ~/.claude/skills/ directory
         */
        private fun loadFromUserSkillsDir(): List<ClaudeSkillCommand> {
            val userHome = System.getProperty("user.home") ?: return emptyList()
            val skillsDir = Path.of(userHome, USER_SKILLS_DIR)
            
            if (!skillsDir.exists()) {
                return emptyList()
            }

            return try {
                skillsDir.listDirectoryEntries()
                    .filter { it.isDirectory() }
                    .mapNotNull { dir ->
                        val skillFile = dir.resolve(SKILL_FILE)
                        if (skillFile.exists()) {
                            loadSkillFromFile(skillFile, dir)
                        } else {
                            null
                        }
                    }
            } catch (e: Exception) {
                emptyList()
            }
        }

        /**
         * Load a skill from a SKILL.md file
         */
        private fun loadSkillFromFile(skillFile: Path, skillDir: Path): ClaudeSkillCommand? {
            return try {
                val template = skillFile.readText()
                val (frontmatter, _) = SkillFrontmatter.parse(template)
                
                // Extract skill name from frontmatter or directory name
                val skillName = frontmatter?.name?.ifEmpty { null } 
                    ?: skillDir.fileName.toString()
                
                // Extract description from frontmatter
                val description = frontmatter?.description?.ifEmpty { null }
                    ?: "Claude Skill: $skillName"

                ClaudeSkillCommand(
                    skillName = skillName,
                    description = description,
                    template = template,
                    skillPath = skillDir
                )
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Find a specific Claude Skill by skill name
         */
        fun fromSkillName(project: Project, skillName: String): ClaudeSkillCommand? {
            return all(project).find { it.skillName == skillName }
        }

        /**
         * Find a specific Claude Skill by full command name (e.g., "skill.pdf")
         */
        fun fromFullName(project: Project, commandName: String): ClaudeSkillCommand? {
            return all(project).find { it.fullCommandName == commandName }
        }

        /**
         * Check if Claude Skills are available in the project
         */
        fun isAvailable(project: Project): Boolean {
            return all(project).isNotEmpty()
        }
    }
}

