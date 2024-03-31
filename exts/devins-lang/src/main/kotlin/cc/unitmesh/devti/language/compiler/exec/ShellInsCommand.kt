package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.language.compiler.service.ShellRunService
import cc.unitmesh.devti.language.utils.lookupFile
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.sh.psi.ShFile
import com.intellij.sh.run.ShRunner

/**
 * A class that implements the `InsCommand` interface to execute a shell command within the IntelliJ IDEA environment.
 *
 * This class is designed to run a shell command specified by a given `prop` string, which is assumed to be the path to a file within the project.
 * The command is executed in a shell runner service provided by IntelliJ IDEA, using the specified file's path and its parent directory as the working directory.
 *
 * @param myProject The current project context.
 * @param argument The path to the file within the project whose content should be executed as a shell command.
 */
class ShellInsCommand(val myProject: Project, private val argument: String) : InsCommand {
    override suspend fun execute(): String? {
        val virtualFile = myProject.lookupFile(argument.trim()) ?: return "$DEVINS_ERROR: File not found: $argument"
        val psiFile = PsiManager.getInstance(myProject).findFile(virtualFile) as? ShFile
        val settings: RunnerAndConfigurationSettings? = ShellRunService().createRunSettings(myProject, virtualFile, psiFile)

        if (settings != null) {
            ShellRunService().runFile(myProject, virtualFile, psiFile)
            return "Running shell file: $argument"
        }

        val workingDirectory = virtualFile.parent.path
        val shRunner = ApplicationManager.getApplication().getService(ShRunner::class.java)
            ?: return "$DEVINS_ERROR: Shell runner not found"

        if (shRunner.isAvailable(myProject)) {
            shRunner.run(myProject, virtualFile.path, workingDirectory, "RunDevInsShell", true)
        }

        return "Running shell command: $argument"
    }
}
