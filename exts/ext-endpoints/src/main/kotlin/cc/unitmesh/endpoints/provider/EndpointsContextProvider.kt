package cc.unitmesh.endpoints.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.microservices.endpoints.EndpointsProjectModel
import com.intellij.openapi.project.Project

class EndpointsContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        // todo: update to new api
        return EndpointsProjectModel.EP_NAME.extensionList.isNotEmpty()
    }

    override suspend fun collect(
        project: Project,
        creationContext: ChatCreationContext
    ): List<ChatContextItem> {
        val model = EndpointsProjectModel.EP_NAME.extensionList.firstOrNull()
        if (model == null) return emptyList()

        return emptyList()
    }
}