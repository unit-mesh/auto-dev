package cc.unitmesh.httpclient

import cc.unitmesh.devti.provider.http.HttpClientProvider
import com.intellij.execution.Executor
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.httpClient.http.request.run.HttpRequestExecutorExtensionFactory
import com.intellij.httpClient.http.request.run.HttpRequestRunConfigurationExecutor
import com.intellij.httpClient.http.request.run.config.HttpRequestRunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

class IntellijHttpClientExecutor : HttpClientProvider {
    override fun execute(project: Project, virtualFile: VirtualFile, text: String) {
        val psiFile: PsiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return

        val runner: RunnerAndConfigurationSettings = ConfigurationContext(psiFile)
            .configurationsFromContext
            ?.firstOrNull()
            ?.configurationSettings ?: return

//        runner.configuration?.apply {
//            this as HttpRequestRunConfiguration
//        }

        val executor: Executor = HttpRequestExecutorExtensionFactory.getRunExtension().executor ?: return
        HttpRequestRunConfigurationExecutor.getInstance().execute(
            project, runner, executor
        )
    }
}