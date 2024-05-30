package cc.unitmesh.httpclient

import cc.unitmesh.devti.provider.http.HttpClientProvider
import com.intellij.execution.Executor
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.httpClient.http.request.run.HttpRequestExecutorExtensionFactory
import com.intellij.httpClient.http.request.run.HttpRequestRunConfigurationExecutor
import com.intellij.httpClient.http.request.run.config.HttpRequestRunConfiguration
import com.intellij.httpClient.http.request.run.config.HttpRequestRunConfigurationType
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

class IntellijHttpClientExecutor : HttpClientProvider {
    override fun execute(project: Project, virtualFile: VirtualFile, text: String) {
        val originFile: PsiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return

        // create temporary file for http request
        ScratchFileService.getInstance()
        val scratchFile = ScratchRootType.getInstance()
            .createScratchFile(project, "autodev-http-request.http", originFile.language, text) ?: return

        val psiFile = PsiManager.getInstance(project).findFile(scratchFile) ?: return

        val runner: RunnerAndConfigurationSettings = ConfigurationContext(psiFile)
            .configurationsFromContext
            ?.firstOrNull()
            ?.configurationSettings ?: return


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

        val executor: Executor = HttpRequestExecutorExtensionFactory.getRunExtension().executor ?: return
        HttpRequestRunConfigurationExecutor.getInstance().execute(
            project, runner, executor
        )
    }
}