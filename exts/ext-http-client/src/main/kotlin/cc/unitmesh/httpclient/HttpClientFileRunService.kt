package cc.unitmesh.httpclient

import cc.unitmesh.devti.provider.RunService
import com.intellij.execution.Executor
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.httpClient.http.request.HttpRequestFileType
import com.intellij.httpClient.http.request.run.HttpRequestExecutorExtensionFactory
import com.intellij.httpClient.http.request.run.HttpRequestRunConfigurationExecutor
import com.intellij.httpClient.http.request.run.config.HttpRequestRunConfiguration
import com.intellij.httpClient.http.request.run.config.HttpRequestRunConfigurationType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager

class HttpClientFileRunService : RunService {
    override fun isApplicable(project: Project, file: VirtualFile): Boolean {
        return file.extension == HttpRequestFileType.INSTANCE.defaultExtension
    }

    override fun runConfigurationClass(project: Project): Class<out RunProfile> {
        return HttpRequestRunConfiguration::class.java
    }

    override fun runFile(project: Project, virtualFile: VirtualFile, psiElement: PsiElement?, isFromToolAction: Boolean): String? {
        val runner: RunnerAndConfigurationSettings = runReadAction {
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: return@runReadAction null

            ConfigurationContext(psiFile).configurationsFromContext?.firstOrNull()?.configurationSettings
        } ?: return null

        val factory = HttpRequestRunConfigurationType.getInstance().configurationFactories[0]
        val configuration = HttpRequestRunConfiguration(project, factory, "HttpRequest")

        val runManager: RunManager = RunManager.getInstance(project)
        configuration.settings.filePath = virtualFile.path

        runManager.setUniqueNameIfNeeded(configuration)
        runner.isTemporary = true
        runManager.addConfiguration(runner)

        val selectedRunner = runManager.selectedConfiguration
        if ((selectedRunner == null || selectedRunner.isTemporary) && runManager.shouldSetRunConfigurationFromContext()) {
            runManager.selectedConfiguration = runner
        }

        val executor: Executor = HttpRequestExecutorExtensionFactory.getRunExtension().executor ?: return null
        HttpRequestRunConfigurationExecutor.getInstance().execute(
            project, runner, executor
        )

        return "Run Success"
    }

    override fun createConfiguration(project: Project, virtualFile: VirtualFile): RunConfiguration? {
        val factory = HttpRequestRunConfigurationType.getInstance().configurationFactories[0]
        val configuration = HttpRequestRunConfiguration(project, factory, "HttpRequest")
        configuration.settings.filePath = virtualFile.path

        return configuration
    }
}
