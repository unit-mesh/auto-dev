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

                    /// java.lang.ClassCastException: class com.intellij.micronaut.jam.http.MnController cannot be cast to class
                    // com.intellij.spring.model.SpringBeanPointer (com.intellij.micronaut.jam.http.MnController is in unnamed module of loader com.intellij.ide.plugins.cl.PluginClassLoader @5d6888bf; com.intellij.spring.model.SpringBeanPointer is in unnamed module of loader com.intellij.ide.plugins.cl.PluginClassLoader @775c694b)
                    val map = collectUrls(project, endpointsProviderList as List<EndpointsProvider<Any, Any>>)
                    val result =
                        "Here is current project web ${map.size} api endpoints: \n```\n" + map.joinToString("\n") { url ->
                            when (url) {
                                is UrlMappingElement -> url.method.joinToString("\n") {
                                    "$it - ${url.urlPath.toStringWithStars()}" +
                                            " (${UrlMappingElement.getContainingFileName(url)})"
                                }
//                                    MnHttpMappingInfo
                                else -> {
                                    url.toString()
                                }
                            }
                        } + "\n```"

                    future.complete(result)
                } catch (e: Exception) {
                    future.completeExceptionally(e)
                }
            }
        }

        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))

        return future.get()
    }
}

