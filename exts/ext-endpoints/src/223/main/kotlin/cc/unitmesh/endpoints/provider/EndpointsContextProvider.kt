package cc.unitmesh.endpoints.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.openapi.project.Project

class EndpointsContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return false
    }

    override fun collect(
        project: Project,
        creationContext: ChatCreationContext
    ): List<ChatContextItem> {
        return emptyList()
    }
}