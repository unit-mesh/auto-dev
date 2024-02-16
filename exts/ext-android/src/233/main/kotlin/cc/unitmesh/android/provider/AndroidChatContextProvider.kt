package cc.unitmesh.android.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.openapi.project.Project
import org.jetbrains.android.util.AndroidUtils

class AndroidChatContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return AndroidUtils.hasAndroidFacets(project)
    }

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val text = "This project is a Mobile Android project."
        // count for versions
        return listOf(ChatContextItem(AndroidChatContextProvider::class, text))
    }

    private fun getProjectAndroidTargetSdkVersion(project: Project): Int {
        return 0
    }
}