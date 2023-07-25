package cc.unitmesh.devti.provider.context

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock

interface ChatContextProvider {
    @RequiresReadLock
    fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean

    @RequiresBackgroundThread
    fun collect(
        project: Project,
        creationContext: ChatCreationContext,
    ): List<ChatContextItem>

    fun filterItems(list: List<ChatContextItem?>, creationContext: ChatCreationContext): List<ChatContextItem?> {
        return list
    }

    companion object {
        val EP_NAME = ExtensionPointName<ChatContextProvider>("cc.unitmesh.chatContextProvider")
    }
}
