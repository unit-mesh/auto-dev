package cc.unitmesh.devti.language.compiler.service

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.provider.RunService
import cc.unitmesh.devti.sketch.ui.patch.readText
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.sh.psi.ShFile
import com.intellij.sh.run.ShConfigurationType
import com.intellij.sh.run.ShRunConfiguration
import com.intellij.sh.run.ShRunner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.jetbrains.ide.PooledThreadExecutor
import java.io.StringWriter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class ShellRunService : RunService {
    override fun isApplicable(project: Project, file: VirtualFile) = file.extension == "sh" || file.extension == "bash"

    override fun runFile(
        project: Project,
        virtualFile: VirtualFile,
        psiElement: PsiElement?,
        isFromToolAction: Boolean
    ): String? {
        val workingDirectory = project.basePath ?: return "Project base path not found"

        val code = virtualFile.readText()
        if (isFromToolAction) {
            val taskExecutor = PooledThreadExecutor.INSTANCE
            val future: CompletableFuture<String?> = CompletableFuture()
            val task = object : Task.Backgroundable(project, "Running shell command") {
                override fun run(indicator: ProgressIndicator) {
                    runBlocking(taskExecutor.asCoroutineDispatcher()) {
                        val result = ProcessExecutor.executeCodeInIdeaTask(project, code, taskExecutor.asCoroutineDispatcher())
                        future.complete(result ?: "")
                    }
                }
            }

            ProgressManager.getInstance()
                .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))

            return future.get(120, TimeUnit.SECONDS)
        }

        val shRunner = ApplicationManager.getApplication().getService(ShRunner::class.java)
            ?: return "Shell runner not found"

        if (shRunner.isAvailable(project)) {
            shRunner.run(project, virtualFile.path, workingDirectory, "Shell Run Service", true)
        }

        return "Running shell command: ${virtualFile.path}"
    }

    override fun runConfigurationClass(project: Project): Class<out RunProfile> = ShRunConfiguration::class.java
    override fun createConfiguration(project: Project, virtualFile: VirtualFile): RunConfiguration? {
        val psiFile = runReadAction {
            PsiManager.getInstance(project).findFile(virtualFile) as? ShFile
        } ?: return null

        val configurationSetting = RunManager.getInstance(project)
            .createConfiguration(psiFile.name, ShConfigurationType.getInstance())

        val configuration = configurationSetting.configuration as ShRunConfiguration
        configuration.scriptWorkingDirectory = project.basePath!!
        configuration.scriptPath = virtualFile.path
        return configurationSetting.configuration
    }
}
