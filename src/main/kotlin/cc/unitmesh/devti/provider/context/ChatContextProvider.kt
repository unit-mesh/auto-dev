package cc.unitmesh.devti.provider.context

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock
import kotlin.coroutines.Continuation

interface ChatContextProvider {
    @RequiresReadLock
    fun isApplicable(project: Project, chatCreationContext: ChatCreationContext): Boolean

    @RequiresBackgroundThread
    fun collect(
        project: Project,
        chatCreationContext: ChatCreationContext,
        continuation: Continuation<MutableList<ChatContextItem?>?>
    ): Any?

    fun filterItems(list: List<ChatContextItem?>, creationContext: ChatCreationContext): List<ChatContextItem?> {
        return list
    }

    companion object {
        val EP_NAME = ExtensionPointName<ChatContextProvider>("cc.unitmesh.chatContextProvider")
    }
}
