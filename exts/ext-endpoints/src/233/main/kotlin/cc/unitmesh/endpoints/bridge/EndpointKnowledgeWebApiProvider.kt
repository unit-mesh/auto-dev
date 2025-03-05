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
            runReadAction {
                RelatedClassesProvider.provide(it.language)?.lookupCallee(project, it)
            }
        }.flatten()

        val initialElements = (decls + relatedCode + callees).distinct().toMutableList()
        
        // 定义最大元素数量和最大递归深度
        val maxElements = 20
        /// Controller-Application-Service-Repository
        /// Controller-Service-Repository/Controller-Service-Service-Repository
        val maxDepth = 4
        
        // 使用递归方式收集元素
        val allElements = collectElementsRecursively(
            project = project,
            elements = initialElements,
            collected = mutableSetOf<PsiElement>().apply { addAll(initialElements) },
            maxElements = maxElements,
            currentDepth = 1,
            maxDepth = maxDepth
        )
        
        return allElements
    }
    
    /**
     * 递归收集元素，直到达到指定数量或最大深度
     */
    private fun collectElementsRecursively(
        project: Project,
        elements: List<PsiElement>,
        collected: MutableSet<PsiElement>,
        maxElements: Int,
        currentDepth: Int,
        maxDepth: Int
    ): MutableList<PsiElement> {
        // 如果已经达到目标数量或最大深度，返回当前收集的元素
        if (collected.size >= maxElements || currentDepth > maxDepth) {
            return collected.toMutableList()
        }
        
        // 寻找下一层的调用关系
        val nextLevelElements = elements.mapNotNull {
            runReadAction {
                RelatedClassesProvider.provide(it.language)?.lookupCallee(project, it)
            }
        }.flatten()
            .filter { it !in collected }
            .distinct()
            
        // 如果没有新的元素，返回当前收集的元素
        if (nextLevelElements.isEmpty()) {
            return collected.toMutableList()
        }
        
        // 添加新的元素，不超过最大数量
        val elementsToAdd = nextLevelElements.take(maxElements - collected.size)
        collected.addAll(elementsToAdd)
        
        // 如果达到目标数量，返回当前收集的元素
        if (collected.size >= maxElements) {
            return collected.toMutableList()
        }
        
        // 递归收集下一层元素
        return collectElementsRecursively(
            project = project,
            elements = elementsToAdd,
            collected = collected,
            maxElements = maxElements,
            currentDepth = currentDepth + 1,
            maxDepth = maxDepth
        )
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
