package cc.unitmesh.ide.webstorm.provider

import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import kotlinx.coroutines.runBlocking

class JavaScriptContextPrompter : ContextPrompter() {
    private var additionContext = ""

    override fun displayPrompt(): String {
        val creationContext = ChatCreationContext(ChatOrigin.ChatAction, action!!, file)

        return runBlocking {
            val contextItems = ChatContextProvider.collectChatContextList(project!!, creationContext)
            contextItems.forEach {
                additionContext += it.text
            }

            return@runBlocking "$action for the code:\n```${lang}$additionContext\n$selectedText\n```"
        }
    }

    override fun requestPrompt(): String {
        val creationContext = ChatCreationContext(ChatOrigin.ChatAction, action!!, file)

        return runBlocking {
            val contextItems = ChatContextProvider.collectChatContextList(project!!, creationContext)
            contextItems.forEach {
                additionContext += it.text
            }

            return@runBlocking "$action for the code:\n```${lang}$additionContext\n$selectedText\n```"
        }
    }
}
