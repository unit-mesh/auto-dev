package cc.unitmesh.terminal.sketch

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.sh.ShLanguage
import com.intellij.sh.psi.ShCommand
import com.intellij.sh.psi.ShFile

object ShellSyntaxSafetyCheck {
    /**
     * Check if shell command contains dangerous operations
     * @return Pair<Boolean, String> - first: is dangerous, second: reason message
     */
    fun checkDangerousCommand(project: Project, command: String): Pair<Boolean, String> {
        val psiFile = PsiFileFactory.getInstance(project)
            .createFileFromText("temp.sh", ShLanguage.INSTANCE, command) as? ShFile
            ?: return Pair(true, "Could not parse command")

        val commandElements = PsiTreeUtil.findChildrenOfType(psiFile, ShCommand::class.java)

        for (cmd in commandElements) {
            if (isDangerousRmCommand(cmd)) {
                return Pair(true, "Dangerous rm command detected")
            }

            if (isSudoCommand(cmd)) {
                val sudoArgs = getSudoArgs(cmd)
                if (sudoArgs.contains("rm")) {
                    return Pair(true, "Removing files with elevated privileges")
                }
            }

            if (isCommandWithName(cmd, "mkfs")) {
                return Pair(true, "Filesystem formatting command")
            }

            if (isCommandWithName(cmd, "dd")) {
                return Pair(true, "Low-level disk operation")
            }

            if (isCommandWithName(cmd, "chmod") && hasRecursiveFlag(cmd) && hasInsecurePermissions(cmd)) {
                return Pair(true, "Recursive chmod with insecure permissions")
            }

            if (operatesOnRootDirectory(cmd)) {
                return Pair(true, "Operation targeting root directory")
            }
        }

        if (command.contains(":(){ :|:& };:") || command.matches(".*:[(][)][{]\\s*:|:&\\s*[}];:.*".toRegex())) {
            return Pair(true, "Potential fork bomb")
        }

        return Pair(false, "")
    }

    private fun isDangerousRmCommand(command: ShCommand): Boolean {
        if (!isCommandWithName(command, "rm")) return false

        val options = getCommandOptions(command)
        return options.contains("-rf") || options.contains("-fr") ||
                options.contains("-r") && options.contains("-f") ||
                options.contains("-f") && !options.contains("-i")
    }

    private fun isSudoCommand(command: ShCommand): Boolean {
        return isCommandWithName(command, "sudo")
    }

    private fun getSudoArgs(cmd: ShCommand): List<String> {
        return cmd.text.trim().split("\\s+".toRegex()).drop(1)
    }

    private fun getCommandOptions(cmd: ShCommand): List<String> {
        return cmd.text.trim().split("\\s+".toRegex()).filter { it.startsWith("-") }
    }

    private fun isCommandWithName(cmd: ShCommand, name: String): Boolean {
        val tokens = cmd.text.trim().split("\\s+".toRegex())
        return tokens.firstOrNull() == name
    }

    private fun hasRecursiveFlag(cmd: ShCommand): Boolean {
        return getCommandOptions(cmd).any { it.matches("-[rR]+".toRegex()) }
    }

    private fun hasInsecurePermissions(cmd: ShCommand): Boolean {
        return cmd.text.contains("777")
    }

    private fun operatesOnRootDirectory(cmd: ShCommand): Boolean {
        val tokens = cmd.text.trim().split("\\s+".toRegex())
        return tokens.any { it == "/" }
    }
}