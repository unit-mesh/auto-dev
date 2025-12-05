package cc.unitmesh.devti.provider.context

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.project.Project

class IdeVersionChatContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext) = false

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val productName = ApplicationNamesInfo.getInstance().fullProductName
        val ideVersion = ApplicationInfo.getInstance().fullVersion
        val ideInfo = "Here is current user's IDE: $productName $ideVersion"

        return listOf(ChatContextItem(IdeVersionChatContextProvider::class, ideInfo))
    }
}
