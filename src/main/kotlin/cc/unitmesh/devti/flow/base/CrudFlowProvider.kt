package cc.unitmesh.devti.flow.base

import cc.unitmesh.devti.flow.model.TargetEndpoint
import com.intellij.openapi.extensions.ExtensionPointName

interface CrudFlowProvider {
    fun getOrCreateStoryDetail(id: String): String
    fun updateOrCreateDtoAndEntity(storyDetail: String)
    fun fetchSuggestEndpoint(storyDetail: String): TargetEndpoint
    fun updateOrCreateEndpointCode(target: TargetEndpoint, storyDetail: String)
    fun updateOrCreateServiceAndRepository()

    companion object {
        private val EP_NAME: ExtensionPointName<CrudFlowProvider> =
            ExtensionPointName.create("cc.unitmesh.prompterFormatterProvider")

        fun flowProvider(): CrudFlowProvider? = EP_NAME.extensionList.asSequence().firstOrNull()
    }
}
