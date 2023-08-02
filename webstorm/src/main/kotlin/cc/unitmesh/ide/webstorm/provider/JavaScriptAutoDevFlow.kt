package cc.unitmesh.ide.webstorm.provider

import cc.unitmesh.devti.flow.kanban.Kanban
import cc.unitmesh.devti.prompting.code.TargetEndpoint
import cc.unitmesh.devti.gui.chat.ChatCodingComponent
import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.provider.DevFlowProvider
import com.intellij.openapi.project.Project

class JavaScriptAutoDevFlow : DevFlowProvider() {
    override fun initContext(
        kanban: Kanban?,
        aiRunner: LLMProvider,
        component: ChatCodingComponent,
        project: Project
    ) {
        TODO("Not yet implemented")
    }

    override fun getOrCreateStoryDetail(id: String): String {
        TODO("Not yet implemented")
    }

    override fun updateOrCreateDtoAndEntity(storyDetail: String) {
        TODO("Not yet implemented")
    }

    override fun fetchSuggestEndpoint(storyDetail: String): TargetEndpoint {
        TODO("Not yet implemented")
    }

    override fun updateOrCreateEndpointCode(target: TargetEndpoint, storyDetail: String) {
        TODO("Not yet implemented")
    }

    override fun updateOrCreateServiceAndRepository() {
        TODO("Not yet implemented")
    }
}
