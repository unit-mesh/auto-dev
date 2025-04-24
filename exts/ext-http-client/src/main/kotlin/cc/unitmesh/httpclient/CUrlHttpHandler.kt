package cc.unitmesh.httpclient

import cc.unitmesh.devti.language.envior.ShireEnvReader
import cc.unitmesh.devti.language.envior.ShireEnvVariableFiller
import cc.unitmesh.devti.language.provider.http.HttpHandler
import cc.unitmesh.devti.language.provider.http.HttpHandlerType
import cc.unitmesh.httpclient.converter.CUrlConverter
import com.intellij.execution.console.ConsoleViewWrapperBase
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentManager
import com.intellij.httpClient.converters.curl.parser.CurlParser
import com.intellij.httpClient.execution.RestClientRequest
import com.intellij.httpClient.http.request.HttpRequestCollectionProvider
import com.intellij.httpClient.http.request.notification.HttpClientWhatsNewContentService
import com.intellij.ide.scratch.ScratchUtil
import com.intellij.ide.scratch.ScratchesSearchScope
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiUtilCore
import okhttp3.OkHttpClient
import com.intellij.openapi.application.ApplicationManager
import okhttp3.Request

class CUrlHttpHandler : HttpHandler {
    override fun isApplicable(type: HttpHandlerType): Boolean = type == HttpHandlerType.CURL

    override fun execute(
        project: Project,
        content: String,
        variablesName: Array<String>,
        variableTable: MutableMap<String, Any?>,
    ): String? {
        val processVariables: Map<String, String> = variableTable.mapValues { it.value.toString() }

        var filledShell: String = content
        val client = OkHttpClient()
        var restClientRequest: RestClientRequest? = null
        val request = ApplicationManager.getApplication().executeOnPooledThread<Request?> {
            runReadAction {
                val scope = getSearchScope(project)

                val envName =
                    ShireEnvReader.getAllEnvironments(project, scope).firstOrNull() ?: ShireEnvReader.DEFAULT_ENV_NAME
                val envObject = ShireEnvReader.getEnvObject(envName, scope, project)

                val enVariables: List<Set<String>> = ShireEnvReader.fetchEnvironmentVariables(envName, scope)
                filledShell = ShireEnvVariableFiller.fillVariables(content, enVariables, envObject, processVariables)
                restClientRequest = CurlParser().parseToRestClientRequest(filledShell)

                CUrlConverter.convert(restClientRequest!!)
            }
        }.get()

        if (restClientRequest == null || request == null) {
            return null
        }

        restClientRequest?.let {
            showLogInConsole(project, filledShell, it)
        }

        val response = client.newCall(request).execute()
        return response.body?.string()
    }

    private fun showLogInConsole(project: Project, content: String, request: RestClientRequest) {
        val contentManager = RunContentManager.getInstance(project)
        val console = contentManager.selectedContent?.executionConsole as? ConsoleViewWrapperBase ?: return

        ///-----
        console.print("--------------------\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
        /// original content
        console.print(content, ConsoleViewContentType.LOG_INFO_OUTPUT)
        /// new line
        console.print("\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
        /// converted content
        console.print(request.httpMethod + " " + request.url, ConsoleViewContentType.LOG_INFO_OUTPUT)
        /// headers
        request.headers.forEach {
            console.print("\n${it.key}: ${it.value}", ConsoleViewContentType.LOG_INFO_OUTPUT)
        }
        /// request.body
        console.print("\n" + request.textToSend.toString(), ConsoleViewContentType.LOG_INFO_OUTPUT)
        console.print("\n--------------------\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
    }

    private fun getSearchScope(project: Project, contextFile: PsiFile? = null): GlobalSearchScope {
        val projectScope = ProjectScope.getContentScope(project)
        if (contextFile == null) return projectScope

        val context = PsiUtilCore.getVirtualFile(contextFile)
        val whatsNewFile = HttpClientWhatsNewContentService.getInstance().getWhatsNewFileIfCreated()

        if (contextFile.virtualFile == whatsNewFile) {
            HttpRequestCollectionProvider.getCollectionFolder()?.let { folder ->
                return GlobalSearchScopesCore.directoryScope(project, folder, true)
            }
        }

        if (context != null && !ScratchUtil.isScratch(context) && !projectScope.contains(context)) {
            contextFile.parent?.let { parent ->
                return GlobalSearchScopesCore.directoryScope(parent, true)
            }
        }

        if (ScratchUtil.isScratch(context)) {
            return projectScope.uniteWith(ScratchesSearchScope.getScratchesScope(project))
        }

        return projectScope
    }
}
