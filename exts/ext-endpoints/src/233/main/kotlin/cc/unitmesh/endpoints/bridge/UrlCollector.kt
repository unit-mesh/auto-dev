package cc.unitmesh.endpoints.bridge

import com.intellij.microservices.endpoints.EndpointsProvider
import com.intellij.microservices.endpoints.ModuleEndpointsFilter
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.modules

fun collectUrls(project: Project, model: List<EndpointsProvider<Any, Any>>): List<Any> = runReadAction {
    val endpointsProviders = model
        .filter { it.getStatus(project) == EndpointsProvider.Status.HAS_ENDPOINTS }

    val modules = project.modules
    val groups = modules.map { module ->
        collectGroup(project, module, endpointsProviders)
    }.flatten()

    return@runReadAction groups
}

private fun collectGroup(
    project: Project,
    module: Module,
    endpointsProviders: List<EndpointsProvider<Any, Any>>
): List<Any> {
    val moduleEndpointsFilter = ModuleEndpointsFilter(module, false, false)
    return endpointsProviders.map { provider ->
        provider.getEndpointGroups(project, moduleEndpointsFilter).map { group ->
            provider.getEndpoints(group)
        }.flatten().toList()
    }.flatten()
}