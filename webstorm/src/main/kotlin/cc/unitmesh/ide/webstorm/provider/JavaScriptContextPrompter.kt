package cc.unitmesh.ide.webstorm.provider

import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.runBlocking

class JavaScriptContextPrompter : ContextPrompter() {
    private var additionContext = ""

    companion object {
        val log = logger<JavaScriptContextPrompter>()
    }

    override fun displayPrompt(): String {
        val creationContext = ChatCreationContext(ChatOrigin.ChatAction, action!!, file)
        additionContext = ""

        return runBlocking {
            val contextItems = ChatContextProvider.collectChatContextList(project!!, creationContext)
            contextItems.forEach {
                additionContext += it.text + "\n"
            }

            return@runBlocking "${action!!.instruction(lang)}:\n```markdown\n$additionContext```\n```${lang}\n$selectedText\n```"
        }
    }

    override fun requestPrompt(): String {
        val creationContext = ChatCreationContext(ChatOrigin.ChatAction, action!!, file)
        additionContext = ""

        return runBlocking {
            val contextItems = ChatContextProvider.collectChatContextList(project!!, creationContext)
            contextItems.forEach {
                additionContext += it.text + "\n"
            }

            return@runBlocking "${action!!.instruction(lang)}:\n$additionContext\n```${lang}\n$selectedText\n```"
        }
    }
}
