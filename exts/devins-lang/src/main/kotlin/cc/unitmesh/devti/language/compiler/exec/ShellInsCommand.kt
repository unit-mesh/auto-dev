package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.sketch.run.ProcessExecutor
import com.intellij.openapi.project.Project

/**
 * A class that implements the `InsCommand` interface to execute a shell command within the IntelliJ IDEA environment.
 *
 * This class is designed to run a shell command specified by a given `prop` string, which is assumed to be the path to a file within the project.
 * The command is executed in a shell runner service provided by IntelliJ IDEA, using the specified file's path and its parent directory as the working directory.
 *
 * @param myProject The current project context.
 * @param shellFile The path to the file within the project whose content should be executed as a shell command.
 */
class ShellInsCommand(val myProject: Project, private val shellFile: String?, val shellContent: String?) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.SHELL

    override suspend fun execute(): String? {
        return ProcessExecutor(myProject).executeCode(shellContent ?: "echo 'No content'").let { result ->
            return if (result.exitCode != 0) {
                "Error: ${result.errOutput}"
            } else {
                "Output: ${result.stdOutput}"
            }
        }
    }
}