package cc.unitmesh.devti.command.dataprovider

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.custom.team.TeamPromptsBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

data class CustomCommand(
    val commandName: String,
    val content: String,
    val icon: Icon = AutoDevIcons.COMMAND
) {
    companion object {
        fun all(project: Project): List<CustomCommand> {
            val teamPrompts = TeamPromptsBuilder(project).flows().map { fromFile(it) }
            val specKitCommands = SpecKitCommand.all(project).map { it.toCustomCommand() }
            val claudeSkills = ClaudeSkillCommand.all(project).map { it.toCustomCommand() }
            return teamPrompts + specKitCommands + claudeSkills
        }

        /**
         *  Read the content from the given file and create a CustomCommand object with the file name and content.
         *  @param file the VirtualFile from which the content will be read
         *  @return CustomCommand object containing the name of the file without extension and the content of the file
         */
        private fun fromFile(file: VirtualFile): CustomCommand {
            val content = file.inputStream.readBytes().toString(Charsets.UTF_8)
            return CustomCommand(file.nameWithoutExtension, content)
        }

        fun fromString(project: Project, commandName: String): CustomCommand? {
            return all(project).find { it.commandName == commandName }
        }
    }
}