package cc.unitmesh.endpoints.bridge

import cc.unitmesh.devti.bridge.provider.KnowledgeWebApiProvider
import cc.unitmesh.devti.provider.RelatedClassesProvider
import com.intellij.microservices.endpoints.EndpointsProvider
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.spring.mvc.jam.RequestMethod
import com.intellij.spring.mvc.mapping.UrlMappingElement
import java.util.concurrent.CompletableFuture

class EndpointKnowledgeWebApiProvider : KnowledgeWebApiProvider() {
    override fun isApplicable(project: Project): Boolean = EndpointsProvider.hasAnyProviders()

    override fun lookupApiCallTree(
        project: Project,
        httpMethod: String,
        httpUrl: String
    ): List<PsiElement> {
        val future = CompletableFuture<List<PsiElement>>()
        val task = object : Task.Backgroundable(project, "Processing context", false) {
            override fun run(indicator: ProgressIndicator) {
                var allElements = collectWebApiDecl(project, httpMethod, httpUrl)

                future.complete(allElements)
            }
        }

        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))

        return future.get()
    }

    private fun collectWebApiDecl(project: Project, httpMethod: String, httpUrl: String): MutableList<PsiElement> {
        val endpointsProviderList = runReadAction { EndpointsProvider.getAvailableProviders(project).toList() }
        val decls = collectApiDeclElements(project, endpointsProviderList, httpMethod, httpUrl)

        val relatedCode = decls.mapNotNull {
            runReadAction {
                RelatedClassesProvider.provide(it.language)?.lookupIO(it)
            }
        }.flatten()

        val callees = decls.mapNotNull {
            val classesProvider = runReadAction { RelatedClassesProvider.provide(it.language) }
            classesProvider?.lookupCallee(project, it)
        }.flatten()

        var allElements = (decls + relatedCode + callees).distinct().toMutableList()
        /// find better number then 10, and keep 10 as a default
        if (allElements.size <= 10) {
            val secondLevels =
                callees.mapNotNull {
                    runReadAction {
                        RelatedClassesProvider.provide(it.language)?.lookupCallee(project, it)
                    }
                }.flatten().take(10)
            allElements.addAll(secondLevels)
        }

        return allElements
    }

    private fun collectApiDeclElements(
        project: Project,
        model: List<EndpointsProvider<*, *>>,
        httpMethod: String,
        httpUrl: String
    ): List<PsiElement> = runReadAction {
        val collectUrls = collectUrls(project, model)
        val requestMethod: RequestMethod = httpMethod.toRequestMethod()
        val navElement = collectUrls
            .filter {
                it.method.contains(requestMethod) && compareUrl(it, httpUrl)
            }
            .mapNotNull { it.navigationTarget }
            .distinct()

        return@runReadAction navElement
    }

    private fun compareUrl(element: UrlMappingElement, httpUrl: String): Boolean {
        val queriedRequestUrl = httpUrl.trimStart('/')
        val projectUrls = element.urlPath.toStringWithStars().trimStart('/')
        return projectUrls == queriedRequestUrl
    }
}

private fun String.toRequestMethod(): RequestMethod {
    return when (this) {
        "GET" -> RequestMethod.GET
        "POST" -> RequestMethod.POST
        "PUT" -> RequestMethod.PUT
        "DELETE" -> RequestMethod.DELETE
        "PATCH" -> RequestMethod.PATCH
        "HEAD" -> RequestMethod.HEAD
        "OPTIONS" -> RequestMethod.OPTIONS
        else -> RequestMethod.GET
    }
}
