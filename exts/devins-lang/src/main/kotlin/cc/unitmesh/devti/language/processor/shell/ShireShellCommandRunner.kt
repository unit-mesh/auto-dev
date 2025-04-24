package cc.unitmesh.devti.language.processor.shell

import cc.unitmesh.devti.envior.ShireEnvReader
import cc.unitmesh.devti.envior.ShireEnvVariableFiller
import cc.unitmesh.devti.util.readText
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.ProjectScope
import java.io.File
import java.nio.charset.StandardCharsets


object ShireShellCommandRunner {
    private const val DEFAULT_TIMEOUT: Int = 30000

    fun fill(project: Project, file: VirtualFile, processVariables: Map<String, String>): String {
        return runReadAction {
            val scope = ProjectScope.getContentScope(project)

            val envName =
                ShireEnvReader.getAllEnvironments(project, scope).firstOrNull() ?: ShireEnvReader.DEFAULT_ENV_NAME
            val envObject = ShireEnvReader.getEnvObject(envName, scope, project)

            val content = file.readText()

            val envVariables: List<Set<String>> = ShireEnvReader.fetchEnvironmentVariables(envName, scope)
            val filledContent = ShireEnvVariableFiller.fillVariables(content, envVariables, envObject, processVariables)

            filledContent
        }
    }

    fun runShellCommand(virtualFile: VirtualFile, myProject: Project, processVariables: Map<String, String>): String {
        val workingDirectory = virtualFile.parent.path

        val fileContent = fill(myProject, virtualFile, processVariables)
        val tempFile = File.createTempFile("tempScript", ".sh");
        tempFile.writeText(fileContent)

        val commandLine: GeneralCommandLine = GeneralCommandLine()
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            .withWorkDirectory(workingDirectory)
            .withCharset(StandardCharsets.UTF_8)
            .withExePath("sh")
            .withParameters(tempFile.path)

        val future = ApplicationManager.getApplication().executeOnPooledThread<String> {
            val processOutput = runCatching {
                CapturingProcessHandler(commandLine).runProcess(DEFAULT_TIMEOUT)
            }.apply { deleteFileOnTermination(commandLine, tempFile) }.getOrThrow()


            val exitCode = processOutput.exitCode
            if (exitCode != 0) {
                throw RuntimeException("Cannot execute ${commandLine}: exit code $exitCode, error output: ${processOutput.stderr}")
            }
            processOutput.stdout
        }

        return try {
            future.get() // 阻塞获取结果，可以选择添加超时控制
        } catch (e: Exception) {
            logger<ShireShellCommandRunner>().error("Command execution failed", e)
            throw RuntimeException("Execution failed: ${e.message}", e)
        }
    }

    /**
     * We need to ensure that the file is deleted after the process is executed.
     * for example,the file also needs to be deleted when [create-process][OSProcessHandler.startProcess] fails.
     */
    private fun deleteFileOnTermination(commandLine: GeneralCommandLine, tempFile: File) {
        OSProcessHandler.deleteFileOnTermination(commandLine, tempFile)  // is Internal API
    }
}
