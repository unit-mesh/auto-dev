package cc.unitmesh.devti.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ContextCreator {
    suspend fun collectChatContextList(
        project: Project,
        chatCreationContext: ChatCreationContext,
    ): List<ChatContextItem> {
        val elements = mutableListOf<ChatContextItem>()

        val chatContextProviders = ChatContextProvider.EP_NAME.extensionList
        for (provider in chatContextProviders) {
            val applicable = withContext(Dispatchers.Default) {
                provider.isApplicable(project, chatCreationContext)
            }
            if (applicable) {
                val filteredItems = withContext(Dispatchers.Default) {
                    provider.filterItems(elements, chatCreationContext)
                }.filterNotNull()

                elements.addAll(filteredItems)
            }
        }

        elements.addAll(chatCreationContext.extraItems)

        return elements
    }
}