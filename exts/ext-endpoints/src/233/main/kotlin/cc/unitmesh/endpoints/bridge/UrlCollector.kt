package cc.unitmesh.endpoints.bridge

import com.intellij.microservices.endpoints.EndpointsProvider
import com.intellij.microservices.endpoints.EndpointsUrlTargetProvider
import com.intellij.microservices.endpoints.ModuleEndpointsFilter
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.spring.model.SpringBeanPointer
import com.intellij.spring.mvc.mapping.UrlMappingElement

fun collectUrls(project: Project, model: List<EndpointsProvider<*, *>>): List<UrlMappingElement> = runReadAction {
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

    return@runReadAction map
}