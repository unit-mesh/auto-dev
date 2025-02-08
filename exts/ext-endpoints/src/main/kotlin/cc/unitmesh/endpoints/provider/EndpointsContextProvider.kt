package cc.unitmesh.endpoints.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.microservices.endpoints.EndpointsProvider
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project

class EndpointsContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return runReadAction { EndpointsProvider.getAvailableProviders(project).toList() }.isNotEmpty()
    }

    override suspend fun collect(
        project: Project,
        creationContext: ChatCreationContext
    ): List<ChatContextItem> {
        val model = runReadAction { EndpointsProvider.getAvailableProviders(project).toList() }
        if (model.isEmpty()) return emptyList()

        val availableProviders = model.filter { it.getStatus(project) == EndpointsProvider.Status.HAS_ENDPOINTS }

        if (availableProviders.isNotEmpty()) {
            val infos = availableProviders.mapNotNull {
                val text = "This project has endpoints from ${it.javaClass.simpleName}"
                return@mapNotNull ChatContextItem(EndpointsContextProvider::class, text)
            }.toList()

            return infos
        }

        return emptyList()
    }
}