package cc.unitmesh.devti.flow.base

import cc.unitmesh.devti.flow.model.TargetEndpoint
import com.intellij.psi.PsiJavaFile

interface DevtiFlowAction {
    fun getOrCreateStoryDetail(id: String): String

    fun updateOrCreateDtoAndEntity(storyDetail: String)

    fun fetchSuggestEndpoint(storyDetail: String): TargetEndpoint

    fun updateOrCreateEndpointCode(target: TargetEndpoint, storyDetail: String)

    fun updateOrCreateServiceAndRepository()
}