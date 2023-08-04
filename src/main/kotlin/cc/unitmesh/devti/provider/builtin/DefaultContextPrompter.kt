package cc.unitmesh.devti.provider.builtin

import cc.unitmesh.devti.context.chunks.SimilarChunksWithPaths
import cc.unitmesh.devti.provider.ContextPrompter

class DefaultContextPrompter : ContextPrompter() {
    override fun displayPrompt(): String {
        if (file == null) {
            return "$action\n```${lang}\n$selectedText\n```"
        }

        val chunkContext = SimilarChunksWithPaths.createQuery(file!!)
        return "$action\n```${lang}\n$chunkContext\n$selectedText\n```"
    }

    override fun requestPrompt(): String {
        if (file == null) {
            return "$action\n```${lang}\n$selectedText\n```"
        }

        val chunkContext = SimilarChunksWithPaths.createQuery(file!!)
        return "$action\n```${lang}\n$chunkContext\n$selectedText\n```"
    }
}