package cc.unitmesh.devti.provider

import cc.unitmesh.devti.context.chunks.SimilarChunksWithPaths

class DefaultContextPrompter : ContextPrompter() {
    override fun displayPrompt(): String {
        val chunkContext = SimilarChunksWithPaths.createQuery(file!!)
        return "$action\n```${lang}\n$chunkContext\n$selectedText\n```"
    }

    override fun requestPrompt(): String {
        val chunkContext = SimilarChunksWithPaths.createQuery(file!!)
        return "$action\n```${lang}\n$chunkContext\n$selectedText\n```"
    }
}