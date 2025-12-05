package cc.unitmesh.devti.sketch.run

import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.sh.ShLanguage
import com.intellij.sh.psi.ShCommand

object ShellPsiSyntaxSafetyCheck {
    private val checkerRegistry = ShellCommandCheckerRegistry()
    private val commandCheckerChain: ShellCommandChecker by lazy { checkerRegistry.buildCheckerChain() }

    init {
        checkerRegistry.register(RmCommandChecker())
        checkerRegistry.register(SudoCommandChecker())
        checkerRegistry.register(GenericCommandChecker("mkfs", "Filesystem formatting command"))
        checkerRegistry.register(GenericCommandChecker("dd", "Low-level disk operation"))
        checkerRegistry.register(ChmodCommandChecker())
        checkerRegistry.register(RootDirectoryOperationChecker())
        checkerRegistry.register(PatternCommandChecker())
    }

    /**
     * Check if shell command contains dangerous operations
     * @return Pair<Boolean, String> - first: is dangerous, second: reason message
     */
    fun checkDangerousCommand(project: Project, command: String): Pair<Boolean, String> {
        val createScratchFile = ScratchRootType.getInstance()
            .createScratchFile(project, "devin-shell-ins.sh", ShLanguage.INSTANCE, command)
            ?: return Pair(true, "Could not parse command")

        val psiFile = PsiManager.getInstance(project)
            .findFile(createScratchFile) ?: return Pair(true, "Could not parse command")

        val commandElements = PsiTreeUtil.findChildrenOfType(psiFile, ShCommand::class.java)

        for (cmd in commandElements) {
            val result = commandCheckerChain.check(cmd, command)
            if (result != null && result.first) {
                return result
            }
        }

        createScratchFile.delete(this)
        return Pair(false, "")
    }
}

class ShellCommandCheckerRegistry {
    private val checkers = mutableListOf<ShellCommandChecker>()

    fun register(checker: ShellCommandChecker) {
        checkers.add(checker)
    }

    fun buildCheckerChain(): ShellCommandChecker {
        if (checkers.isEmpty()) {
            throw IllegalStateException("No shell command checkers registered")
        }

        // Build the chain
        for (i in 0 until checkers.size - 1) {
            checkers[i].setNext(checkers[i + 1])
        }

        return checkers.first()
    }
}

interface ShellCommandChecker {
    fun check(command: ShCommand, fullCommandText: String): Pair<Boolean, String>?
    fun setNext(checker: ShellCommandChecker): ShellCommandChecker
}

abstract class BaseShellCommandChecker : ShellCommandChecker {
    protected var nextChecker: ShellCommandChecker? = null

    override fun setNext(checker: ShellCommandChecker): ShellCommandChecker {
        nextChecker = checker
        return checker
    }

    protected fun checkNext(command: ShCommand, fullCommandText: String): Pair<Boolean, String>? {
        return nextChecker?.check(command, fullCommandText)
    }
}

class RmCommandChecker : BaseShellCommandChecker() {
    override fun check(command: ShCommand, fullCommandText: String): Pair<Boolean, String>? {
        if (!ShellCommandUtils.isCommandWithName(command, "rm")) return checkNext(command, fullCommandText)

        val options = ShellCommandUtils.getCommandOptions(command)
        if (options.containsAll(listOf("-r", "-f")) ||
            options.containsAll(listOf("-rf")) ||
            options.containsAll(listOf("-fr")) ||
            (options.contains("-f") && !options.contains("-i"))
        ) {
            return Pair(true, "Dangerous rm command detected")
        }

        return checkNext(command, fullCommandText)
    }
}

class SudoCommandChecker : BaseShellCommandChecker() {
    override fun check(command: ShCommand, fullCommandText: String): Pair<Boolean, String>? {
        if (!ShellCommandUtils.isCommandWithName(command, "sudo")) return checkNext(command, fullCommandText)

        val sudoArgs = ShellCommandUtils.getCommandArgs(command).drop(1)
        if (sudoArgs.contains("rm")) {
            return Pair(true, "Removing files with elevated privileges")
        }
        // Consider more advanced parsing of the command after sudo if needed
        // Example: if (sudoArgs.isNotEmpty()) {
        //     val commandAfterSudo = sudoArgs.joinToString(" ")
        //     // Potentially recursively call checkDangerousCommand on commandAfterSudo
        // }

        return checkNext(command, fullCommandText)
    }
}

class GenericCommandChecker(private val commandName: String, private val dangerMessage: String) :
    BaseShellCommandChecker() {
    override fun check(command: ShCommand, fullCommandText: String): Pair<Boolean, String>? {
        if (ShellCommandUtils.isCommandWithName(command, commandName)) {
            return Pair(true, dangerMessage)
        }
        return checkNext(command, fullCommandText)
    }
    // Consider adding checks based on arguments for specific generic commands if needed
}

class ChmodCommandChecker : BaseShellCommandChecker() {
    override fun check(command: ShCommand, fullCommandText: String): Pair<Boolean, String>? {
        if (!ShellCommandUtils.isCommandWithName(command, "chmod")) return checkNext(command, fullCommandText)

        val commandText = command.text
        if (ShellCommandUtils.hasRecursiveFlag(command) && ShellCommandUtils.hasInsecurePermissions(command)) {
            return Pair(true, "Recursive chmod with insecure permissions (e.g., 777)")
        }

        // Example of checking for other potentially dangerous permission changes:
        // Giving execute permission to 'others' without a clear reason might be risky.
        // This is a simplified check and might need refinement based on your security policy.
        if (commandText.contains("o+x") && !commandText.contains("a+x")) {
            return Pair(true, "Granting execute permission to others (o+x) might be dangerous")
        }
        // You can add more checks here for other insecure permission patterns,
        // like removing write permissions from group or others unexpectedly.

        return checkNext(command, fullCommandText)
    }
}

class RootDirectoryOperationChecker : BaseShellCommandChecker() {
    override fun check(command: ShCommand, fullCommandText: String): Pair<Boolean, String>? {
        if (ShellCommandUtils.operatesOnRootDirectory(command)) {
            return Pair(true, "Operation targeting root directory")
        }
        // Consider more precise checks for operations directly on the root directory
        // For example, commands like `rm -rf /` or `mv /some/file /`
        val tokens = ShellCommandUtils.getCommandArgs(command)
        if (tokens.size > 1 && tokens.drop(1).any { it == "/" }) {
            return Pair(true, "Operation targeting root directory")
        }
        return checkNext(command, fullCommandText)
    }
}

class PatternCommandChecker : BaseShellCommandChecker() {
    override fun check(command: ShCommand, fullCommandText: String): Pair<Boolean, String>? {
        if (fullCommandText.contains(":(){ :|:& };:") ||
            fullCommandText.matches(".*:[(][)][{]\\s*:|:&\\s*[}];:.*".toRegex())
        ) {
            return Pair(true, "Potential fork bomb")
        }
        return checkNext(command, fullCommandText)
    }
}

object ShellCommandUtils {
    fun getCommandOptions(cmd: ShCommand): List<String> {
        return cmd.text.trim().split("\\s+".toRegex()).filter { it.startsWith("-") }
    }

    fun getCommandArgs(cmd: ShCommand): List<String> {
        return cmd.text.trim().split("\\s+".toRegex())
    }

    fun isCommandWithName(cmd: ShCommand, name: String): Boolean {
        return getCommandArgs(cmd).firstOrNull()?.equals(name) ?: false
    }

    fun hasRecursiveFlag(cmd: ShCommand): Boolean {
        return getCommandOptions(cmd).any { it.contains("r", ignoreCase = true) }
    }

    fun hasInsecurePermissions(cmd: ShCommand): Boolean {
        return cmd.text.contains("777")
    }

    fun operatesOnRootDirectory(cmd: ShCommand): Boolean {
        val tokens = getCommandArgs(cmd)
        return tokens.any { it == "/" }
    }
}