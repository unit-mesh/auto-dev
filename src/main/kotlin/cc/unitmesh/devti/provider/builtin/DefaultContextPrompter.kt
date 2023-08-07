package cc.unitmesh.devti.provider.builtin

import com.intellij.temporary.similar.chunks.SimilarChunksWithPaths
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.psi.PsiFile

class DefaultContextPrompter : ContextPrompter() {
    private var similarChunkCache: MutableMap<PsiFile, String?> = mutableMapOf()
    override fun displayPrompt(): String {
        return getPrompt()
    }

    override fun requestPrompt(): String {
        return getPrompt()
    }

    private fun getPrompt(): String {
        if (file == null) {
            return "$action\n```${lang}\n$selectedText\n```"
        }

        if (file !in similarChunkCache) {
            similarChunkCache[file!!] = SimilarChunksWithPaths.createQuery(file!!)
        }

        return "$action\n```${lang}\n${similarChunkCache[file!!]}\n$selectedText\n```"
    }
}