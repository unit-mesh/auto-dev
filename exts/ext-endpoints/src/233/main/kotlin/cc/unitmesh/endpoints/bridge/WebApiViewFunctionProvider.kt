package cc.unitmesh.endpoints.bridge

import cc.unitmesh.devti.bridge.ArchViewCommand
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.facet.FacetManager
import com.intellij.microservices.endpoints.EndpointsProvider
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.spring.SpringManager
import com.intellij.spring.contexts.model.CombinedSpringModel
import com.intellij.spring.contexts.model.CombinedSpringModelImpl
import com.intellij.spring.contexts.model.SpringModel
import com.intellij.spring.facet.SpringFacet
import com.intellij.spring.mvc.mapping.UrlMappingElement
import java.util.concurrent.CompletableFuture

class WebApiViewFunctionProvider : ToolchainFunctionProvider {
    override suspend fun funcNames(): List<String> = listOf(ArchViewCommand.WebApiView.name)

    override suspend fun isApplicable(project: Project, funcName: String): Boolean =
        funcName == ArchViewCommand.WebApiView.name

    override suspend fun execute(
        project: Project,
        prop: String,
        args: List<Any>,
        allVariables: Map<String, Any?>,
        commandName: @NlsSafe String
    ): Any {
        val future = CompletableFuture<String>()
        val task = object : Task.Backgroundable(project, "Processing context", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val endpointsProviderList = runReadAction {
                        EndpointsProvider.getAvailableProviders(project).toList()
                    }
                    if (endpointsProviderList.isEmpty()) {
                        future.complete("Cannot find any endpoints")
                        return
                    }

                    val urls = collectUrls(project, endpointsProviderList as List<EndpointsProvider<Any, Any>>)
                    val formatUrls = urls
                        .map(::formatUrl)
                        .filter(String::isNotBlank)
                        .joinToString("\n")

                    val result = "Here is current ${urls.size} api endpoints: \n```\n" + formatUrls + "\n```" +
                            "\nYou can use `/knowledge` command to get more details about the endpoints."
                    future.complete(result)
                } catch (e: Exception) {
                    future.completeExceptionally(e)
                }
            }
        }

//        runReadAction {
//            ModuleManager.getInstance(project).modules.forEach { module ->
//                SpringCommonUtils.withProgress {
//                    val context = SpringModelsCreationContext.create(module).loadModelsFromDependentModules()
//                        .loadModelsFromModuleDependencies()
//                    val allModels = SpringManagerImpl.getAllModels(context)
//                }
//            }
//        }

        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))

        return future.get()
    }

    private fun getCombinedModelForProject(project: Project): CombinedSpringModel {
        val allCombinedModels: MutableSet<SpringModel> = LinkedHashSet<SpringModel>()
        for (module in ModuleManager.getInstance(project).modules) {
            val allModels = SpringManager.getInstance(project).getAllModels(module)
            allCombinedModels.addAll(allModels)
        }

        return CombinedSpringModelImpl(allCombinedModels, null)
    }

    fun getAllSpringFacets(project: Project): List<SpringFacet> {
        return ModuleManager.getInstance(project).modules.mapNotNull { module ->
            FacetManager.getInstance(module).getFacetByType(SpringFacet.FACET_TYPE_ID)
        }
    }

    private fun formatUrl(url: Any): String = when (url) {
        is UrlMappingElement -> url.method.joinToString("\n") {
            "$it - ${url.urlPath.toStringWithStars()}" +
                    " (${UrlMappingElement.getContainingFileName(url)})"
        }

        else -> {
            url.toString()
        }
    }
}

