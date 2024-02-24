package cc.unitmesh.devti.provider.builtin

import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import com.intellij.psi.PsiFile
import kotlinx.coroutines.runBlocking

class DefaultContextPrompter : ContextPrompter() {
    private var similarChunkCache: MutableMap<PsiFile, String?> = mutableMapOf()
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

        if (file == null) {
            return "$action\n$additionContext\n```${lang}\n$selectedText\n```"
        }

//        if (file !in similarChunkCache) {
//            similarChunkCache[file!!] = SimilarChunksWithPaths.createQuery(file!!)
//        }

        return "$action\n```${lang}\n$selectedText\n```"
    }
}