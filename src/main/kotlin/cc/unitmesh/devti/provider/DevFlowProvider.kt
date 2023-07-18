package cc.unitmesh.devti.provider

import cc.unitmesh.devti.flow.kanban.Kanban
import cc.unitmesh.devti.flow.model.TargetEndpoint
import cc.unitmesh.devti.gui.chat.ChatCodingComponent
import cc.unitmesh.devti.models.openai.OpenAIProvider
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface DevFlowProvider {
    fun initContext(kanban: Kanban, aiRunner: OpenAIProvider, component: ChatCodingComponent, project: Project)

    fun getOrCreateStoryDetail(id: String): String
    fun updateOrCreateDtoAndEntity(storyDetail: String)
    fun fetchSuggestEndpoint(storyDetail: String): TargetEndpoint
    fun updateOrCreateEndpointCode(target: TargetEndpoint, storyDetail: String)
    fun updateOrCreateServiceAndRepository()

    companion object {
        private val EP_NAME: ExtensionPointName<DevFlowProvider> =
            ExtensionPointName.create("cc.unitmesh.devFlowProvider")

        fun flowProvider(): DevFlowProvider? = EP_NAME.extensionList.firstOrNull()
    }
}
