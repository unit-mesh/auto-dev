package cc.unitmesh.devti.http

import cc.unitmesh.devti.provider.http.HttpClientProvider
import com.intellij.execution.Executor
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.httpClient.http.request.HttpRequestPsiFile
import com.intellij.httpClient.http.request.run.HttpRequestExecutorExtensionFactory
import com.intellij.httpClient.http.request.run.HttpRequestRunConfigurationExecutor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager

class IntellijHttpClientExecutor : HttpClientProvider {
    override fun execute(project: Project, virtualFile: VirtualFile, text: String) {
        val psiFile: HttpRequestPsiFile =
            PsiManager.getInstance(project).findFile(virtualFile) as? HttpRequestPsiFile ?: return

        val runner: RunnerAndConfigurationSettings = ConfigurationContext(psiFile).configurationsFromContext?.firstOrNull {
            val configuration = it.configuration as? RunnerAndConfigurationSettings
            configuration?.configuration?.name == "HttpRequest"
        }?.configurationSettings ?: return

        val executor: Executor = HttpRequestExecutorExtensionFactory.getRunExtension().executor ?: return
        HttpRequestRunConfigurationExecutor.getInstance().execute(
            project, runner, executor
        )
    }
}