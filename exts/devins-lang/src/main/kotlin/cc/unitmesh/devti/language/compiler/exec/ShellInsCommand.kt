package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.gui.chat.ui.AutoInputService
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.language.compiler.service.ShellRunService
import cc.unitmesh.devti.language.utils.lookupFile
import cc.unitmesh.devti.sketch.run.ShellUtil
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiManager
import com.intellij.sh.psi.ShFile
import com.intellij.sh.run.ShRunner
import java.awt.Toolkit.getDefaultToolkit
import java.io.IOException
import java.util.concurrent.CompletableFuture

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
        val shRunner = ApplicationManager.getApplication().getService(ShRunner::class.java)
            ?: return "$DEVINS_ERROR: Shell runner not found"

        if (shellContent != null) {
            val commandLine = createCommandLineForScript(myProject, shellContent)
            val processBuilder = commandLine.toProcessBuilder()
            val process = processBuilder.start()
            val processHandler = KillableProcessHandler(process, commandLine.commandLineString)
            processHandler.startNotify()

            processHandler.addProcessListener(object : ProcessAdapter() {
                override fun processTerminated(event: ProcessEvent) {
                    super.processTerminated(event)
                    val allOutput = process.outputStream.buffered()
                    val hasToolwindow = ToolWindowManager.getInstance(myProject).getToolWindow("AutoDev")
                    if (hasToolwindow != null) {
                        AutoInputService.getInstance(myProject).putText(allOutput.toString())
                    }

                    // copy to clipboard
                    val selection = allOutput.toString()
                    if (selection.isNotEmpty()) {
                        val selectionTransferable = java.awt.datatransfer.StringSelection(selection)
                        getDefaultToolkit().systemClipboard.setContents(selectionTransferable, null)
                    }

                    if (event.exitCode != 0) {
                        AutoDevNotifications.notify(myProject, "Process terminated with exit code ${event.exitCode}")
                    }

                    processHandler.destroyProcess()
                }
            })

            return ""
        }

        val virtualFile: VirtualFile = if (shellFile != null) {
            myProject.lookupFile(shellFile.trim()) ?: return "$DEVINS_ERROR: File not found: $shellFile"
        } else {
            null
        } ?: return "$DEVINS_ERROR: File not found"


        val psiFile = PsiManager.getInstance(myProject).findFile(virtualFile) as? ShFile
        val settings: RunnerAndConfigurationSettings? =
            ShellRunService().createRunSettings(myProject, virtualFile, psiFile)

        if (settings != null) {
            ShellRunService().runFile(myProject, virtualFile, psiFile, true)
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

    fun createCommandLineForScript(project: Project, scriptText: String): GeneralCommandLine {
        val workingDirectory = project.basePath
        val commandLine = PtyCommandLine()
        commandLine.withConsoleMode(false)
        commandLine.withInitialColumns(120)
        commandLine.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
        commandLine.setWorkDirectory(workingDirectory!!)
        commandLine.withExePath(ShellUtil.detectShells().first())
        commandLine.withParameters("-c")
        commandLine.withParameters(scriptText)
        return commandLine
    }
}
