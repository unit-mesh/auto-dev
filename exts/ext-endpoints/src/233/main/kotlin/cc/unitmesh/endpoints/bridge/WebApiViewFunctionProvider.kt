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
import org.jetbrains.kotlin.idea.base.facet.isTestModule

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

//        return availableProviders.joinToString("\n") {
//            it.getEndpointGroups(project, ModuleEndpointsFilter(project))
//            "This project has http endpoints from ${it.presentation.title}"
//        }
        val modules = project.modules
        val groups = modules.map { module ->
            val moduleEndpointsFilter = ModuleEndpointsFilter(module, false, module.isTestModule)
            availableProviders.map { provider ->
                provider.getEndpointGroups(project, moduleEndpointsFilter)
            }.flatten()
        }.flatten()

        val map: List<UrlMappingElement> = groups.map { group ->
            availableProviders.map {
                it.getEndpoints(group)
            }.flatten()
        }.flatten()

        return map.joinToString("\n")
    }
}
