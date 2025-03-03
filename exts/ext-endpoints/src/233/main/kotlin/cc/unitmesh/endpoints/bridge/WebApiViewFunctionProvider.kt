package cc.unitmesh.endpoints.bridge

import cc.unitmesh.devti.bridge.ArchViewCommand
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.microservices.endpoints.EndpointsProvider
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.spring.mvc.mapping.UrlMappingElement
import java.util.concurrent.CompletableFuture

class WebApiViewFunctionProvider : ToolchainFunctionProvider {
    override fun funcNames(): List<String> = listOf(ArchViewCommand.WebApiView.name)

    override fun isApplicable(project: Project, funcName: String): Boolean = funcName == ArchViewCommand.WebApiView.name

    override fun execute(
        project: Project,
        prop: String,
        args: List<Any>,
        allVariables: Map<String, Any?>
    ): Any {
        val endpointsProviderList = runReadAction { EndpointsProvider.getAvailableProviders(project).toList() }
        if (endpointsProviderList.isEmpty()) return "Cannot find any endpoints"

        val future = CompletableFuture<String>()
        val task = object : Task.Backgroundable(project, "Processing context", false) {
            override fun run(indicator: ProgressIndicator) {
                val map = collectUrls(project, endpointsProviderList)
                val result =
                    """Here is current project web api endpoints, ${map.size}:""" + map.joinToString("\n") { url ->
                        url.method.joinToString("\n") {
                            "$it - ${url.urlPath.toStringWithStars()}" + " (${
                                UrlMappingElement.getContainingFileName(
                                    url
                                )
                            })"
                        }
                    }

                future.complete(result)
            }
        }

        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))

        return future.get()
    }
}

