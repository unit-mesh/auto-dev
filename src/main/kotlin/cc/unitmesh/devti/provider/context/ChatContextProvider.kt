package cc.unitmesh.devti.provider.context

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ChatContextProvider {
    @RequiresReadLock
    fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean

    @RequiresBackgroundThread
    fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem>

    fun filterItems(
        list: List<ChatContextItem?>,
        creationContext: ChatCreationContext
    ): List<ChatContextItem?> {
        return list
    }

    companion object {
        val EP_NAME = ExtensionPointName<ChatContextProvider>("cc.unitmesh.chatContextProvider")

        suspend fun collectChatContextList(
            project: Project,
            chatCreationContext: ChatCreationContext,
        ): List<ChatContextItem> {
            val elements = mutableListOf<ChatContextItem>()

            val chatContextProviders = EP_NAME.extensionList
            for (provider in chatContextProviders) {
                try {
                    val applicable = withContext(Dispatchers.Default) {
                        provider.isApplicable(project, chatCreationContext)
                    }

                    if (applicable) {
                        val filteredItems = withContext(Dispatchers.Default) {
                            provider.filterItems(provider.collect(project, chatCreationContext), chatCreationContext)
                        }.filterNotNull()

                        elements.addAll(filteredItems)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            elements.addAll(chatCreationContext.extraItems)

            return elements
        }

        suspend fun collectChatContext(
            project: Project,
            chatCreationContext: ChatCreationContext
        ): String {
            val itemList = collectChatContextList(project, chatCreationContext)
            return itemList.joinToString(separator = "\n") { it.text }
        }
    }
}
