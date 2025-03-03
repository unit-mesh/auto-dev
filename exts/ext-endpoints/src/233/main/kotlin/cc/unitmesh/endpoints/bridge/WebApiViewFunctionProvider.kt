package cc.unitmesh.endpoints.bridge

import cc.unitmesh.devti.bridge.ArchViewCommand
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.microservices.endpoints.EndpointsProvider
import com.intellij.microservices.endpoints.EndpointsUrlTargetProvider
import com.intellij.microservices.endpoints.ModuleEndpointsFilter
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.spring.model.SpringBeanPointer
import com.intellij.spring.mvc.mapping.UrlMappingElement

class WebApiViewFunctionProvider : ToolchainFunctionProvider {
    override fun funcNames(): List<String> = listOf(ArchViewCommand.WebApiView.name)

    override fun isApplicable(project: Project, funcName: String): Boolean = funcName == ArchViewCommand.WebApiView.name

    override fun execute(
        project: Project,
        prop: String,
        args: List<Any>,
        allVariables: Map<String, Any?>
    ): Any {
        val model = runReadAction { EndpointsProvider.getAvailableProviders(project).toList() }
        if (model.isEmpty()) return "Cannot find any endpoints"

        val availableProviders = model
            .filter { it.getStatus(project) == EndpointsProvider.Status.HAS_ENDPOINTS }
            .filterIsInstance<EndpointsUrlTargetProvider<SpringBeanPointer<*>, UrlMappingElement>>()

        val modules = project.modules
        val groups = modules.map { module ->
            val moduleEndpointsFilter = ModuleEndpointsFilter(module, false, false)
            availableProviders.map { provider ->
                provider.getEndpointGroups(project, moduleEndpointsFilter)
            }.flatten()
        }.flatten()

        val map: List<UrlMappingElement> = groups.map { group ->
            availableProviders.map {
                it.getEndpoints(group)
            }.flatten()
        }.flatten()

        return """Here is current project web api endpoints, ${map.size}:""" + map.joinToString("\n") {
            "endpoint: ${it.method} - ${it.urlPath}"
        }
    }
}
