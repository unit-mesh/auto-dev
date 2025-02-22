package cc.unitmesh.endpoints.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.microservices.endpoints.EndpointsProvider
import com.intellij.microservices.endpoints.EndpointsUrlTargetProvider
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.spring.model.SpringBeanPointer
import com.intellij.spring.mvc.mapping.UrlMappingElement

/**
 * Since it's very slow to load all endpoints, we don't want to show this context provider in the chat.
 */
class EndpointsContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return EndpointsProvider.hasAnyProviders()
    }

    override fun collect(
        project: Project,
        creationContext: ChatCreationContext
    ): List<ChatContextItem> {
        val model = runReadAction { EndpointsProvider.getAvailableProviders(project).toList() }
        if (model.isEmpty()) return emptyList()

        val availableProviders = model
            .filter { it.getStatus(project) == EndpointsProvider.Status.HAS_ENDPOINTS }
            .filterIsInstance<EndpointsUrlTargetProvider<SpringBeanPointer<*>, UrlMappingElement>>()

        return availableProviders.map {
            val text = "\n- This project has http endpoints from ${it.presentation.title}"
            ChatContextItem(EndpointsContextProvider::class, text)
        }
    }
}