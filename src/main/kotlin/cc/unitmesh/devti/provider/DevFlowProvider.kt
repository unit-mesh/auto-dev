package cc.unitmesh.devti.provider

import cc.unitmesh.devti.flow.TaskFlow
import cc.unitmesh.devti.flow.kanban.Kanban
import cc.unitmesh.devti.prompting.code.TargetEndpoint
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.llms.LLMProvider
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.LazyExtensionInstance
import com.intellij.util.xmlb.annotations.Attribute

abstract class DevFlowProvider : LazyExtensionInstance<ContextPrompter>(), TaskFlow<String> {
    @Attribute("language")
    var language: String? = null

    @Attribute("implementationClass")
    var implementationClass: String? = null

    override fun getImplementationClassName(): String? = implementationClass

    override fun clarify(): String {
        return ""
    }

    abstract fun initContext(kanban: Kanban?, aiRunner: LLMProvider, component: ChatCodingPanel, project: Project)
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
