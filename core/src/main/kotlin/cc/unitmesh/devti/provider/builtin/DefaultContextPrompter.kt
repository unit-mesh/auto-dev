package cc.unitmesh.devti.provider.builtin

import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import kotlinx.coroutines.runBlocking

class DefaultContextPrompter : ContextPrompter() {
    override fun displayPrompt(): String {
        return getPrompt()
    }

    override fun requestPrompt(): String {
        return getPrompt()
    }

    private fun getPrompt(): String {
        var additionContext: String
        runBlocking {
            val creationContext = ChatCreationContext(ChatOrigin.ChatAction, action!!, file, emptyList(), null)
            additionContext = collectionContext(creationContext)
        }

        val prompt = action!!.instruction(lang, project).requestText
        if (file == null) {
            return "$prompt\n$additionContext\n```${lang}\n$selectedText\n```"
        }

        return "$prompt\n```${lang}\n$selectedText\n```"
    }
}