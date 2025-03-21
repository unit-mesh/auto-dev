package cc.unitmesh.terminal.sketch

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.sh.ShLanguage
import com.intellij.sh.psi.ShCommand
import com.intellij.sh.psi.ShFile

object ShellSyntaxSafetyCheck {
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
        val psiFile = PsiFileFactory.getInstance(project)
            .createFileFromText("temp.sh", ShLanguage.INSTANCE, command) as? ShFile
            ?: return Pair(true, "Could not parse command")

        val commandElements = PsiTreeUtil.findChildrenOfType(psiFile, ShCommand::class.java)

        for (cmd in commandElements) {
            val result = commandCheckerChain.check(cmd, command)
            if (result != null && result.first) {
                return result
            }
        }

        return Pair(false, "")
    }

    // Public API to register custom checkers
    fun registerChecker(checker: ShellCommandChecker) {
        checkerRegistry.register(checker)
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
        if (options.contains("-rf") || options.contains("-fr") ||
            options.contains("-r") && options.contains("-f") ||
            options.contains("-f") && !options.contains("-i")
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
}

class ChmodCommandChecker : BaseShellCommandChecker() {
    override fun check(command: ShCommand, fullCommandText: String): Pair<Boolean, String>? {
        if (!ShellCommandUtils.isCommandWithName(command, "chmod")) return checkNext(command, fullCommandText)

        if (ShellCommandUtils.hasRecursiveFlag(command) && ShellCommandUtils.hasInsecurePermissions(command)) {
            return Pair(true, "Recursive chmod with insecure permissions")
        }

        return checkNext(command, fullCommandText)
    }
}

class RootDirectoryOperationChecker : BaseShellCommandChecker() {
    override fun check(command: ShCommand, fullCommandText: String): Pair<Boolean, String>? {
        if (ShellCommandUtils.operatesOnRootDirectory(command)) {
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
        val tokens = cmd.text.trim().split("\\s+".toRegex())
        return tokens.firstOrNull() == name
    }

    fun hasRecursiveFlag(cmd: ShCommand): Boolean {
        return getCommandOptions(cmd).any { it.contains("r", ignoreCase = true) }
    }

    fun hasInsecurePermissions(cmd: ShCommand): Boolean {
        return cmd.text.contains("777")
    }

    fun operatesOnRootDirectory(cmd: ShCommand): Boolean {
        val tokens = cmd.text.trim().split("\\s+".toRegex())
        return tokens.any { it == "/" }
    }
}
