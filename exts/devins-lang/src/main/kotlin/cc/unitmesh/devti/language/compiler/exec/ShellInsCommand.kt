package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.language.compiler.service.ShellRunService
import cc.unitmesh.devti.language.utils.lookupFile
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.sh.psi.ShFile
import com.intellij.sh.run.ShRunner
import java.io.IOException

/**
 * A class that implements the `InsCommand` interface to execute a shell command within the IntelliJ IDEA environment.
 *
 * This class is designed to run a shell command specified by a given `prop` string, which is assumed to be the path to a file within the project.
 * The command is executed in a shell runner service provided by IntelliJ IDEA, using the specified file's path and its parent directory as the working directory.
 *
 * @param myProject The current project context.
 * @param shellFile The path to the file within the project whose content should be executed as a shell command.
 */
class ShellInsCommand(val myProject: Project, private val shellFile: String?, val shellcoNTENT: String?) : InsCommand {
    override suspend fun execute(): String? {
        val shRunner = ApplicationManager.getApplication().getService(ShRunner::class.java)
            ?: return "$DEVINS_ERROR: Shell runner not found"

        val virtualFile: VirtualFile = if (shellFile != null) {
            myProject.lookupFile(shellFile.trim()) ?: return "$DEVINS_ERROR: File not found: $shellFile"
        } else {
            val compute = WriteAction.compute<VirtualFile, Throwable> {
                val file = createFile(shellcoNTENT!!)
                VfsUtil.saveText(file, shellcoNTENT)
                return@compute file
            }

            compute
        }

        val psiFile = PsiManager.getInstance(myProject).findFile(virtualFile) as? ShFile
        val settings: RunnerAndConfigurationSettings? =
            ShellRunService().createRunSettings(myProject, virtualFile, psiFile)

        if (settings != null) {
            ShellRunService().runFile(myProject, virtualFile, psiFile)
            return "Running shell file: $shellFile"
        }

        val workingDirectory = virtualFile.parent.path
        if (shRunner.isAvailable(myProject)) {
            shRunner.run(myProject, virtualFile.path, workingDirectory, "RunDevInsShell", true)
        }

        return "Running shell command: $shellFile"
    }

    @Throws(IOException::class)
    fun createFile(filePath: String): VirtualFile {
        val file = LocalFileSystem.getInstance().refreshAndFindFileByPath(filePath)
        return file ?: VfsUtil.createDirectories(filePath)
    }
}
