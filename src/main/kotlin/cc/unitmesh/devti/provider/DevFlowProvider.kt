package cc.unitmesh.devti.provider

import cc.unitmesh.devti.flow.kanban.Kanban
import cc.unitmesh.devti.flow.model.TargetEndpoint
import cc.unitmesh.devti.gui.chat.ChatCodingComponent
import cc.unitmesh.devti.models.LLMProvider
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.LazyExtensionInstance
import com.intellij.util.xmlb.annotations.Attribute

abstract class DevFlowProvider : LazyExtensionInstance<ContextPrompter>() {
    @Attribute("language")
    var language: String? = null

    @Attribute("implementationClass")
    var implementationClass: String? = null

    override fun getImplementationClassName(): String? {
        return implementationClass
    }

    abstract fun initContext(kanban: Kanban, aiRunner: LLMProvider, component: ChatCodingComponent, project: Project)
    abstract fun getOrCreateStoryDetail(id: String): String
    abstract fun updateOrCreateDtoAndEntity(storyDetail: String)
    abstract fun fetchSuggestEndpoint(storyDetail: String): TargetEndpoint
    abstract fun updateOrCreateEndpointCode(target: TargetEndpoint, storyDetail: String)
    abstract fun updateOrCreateServiceAndRepository()

    companion object {
        private val EP_NAME: ExtensionPointName<DevFlowProvider> =
            ExtensionPointName.create("cc.unitmesh.devFlowProvider")

        fun flowProvider(lang: String): DevFlowProvider? {
            val extensionList = EP_NAME.extensionList
            val contextPrompter = extensionList.filter {
                it.language?.lowercase() == lang.lowercase()
            }

            return if (contextPrompter.isEmpty()) {
                extensionList.first()
            } else {
                contextPrompter.first()
            }
        }
    }
}
