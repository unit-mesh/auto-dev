package cc.unitmesh.pycharm.provider

import cc.unitmesh.devti.context.chunks.SimilarChunksWithPaths
import cc.unitmesh.devti.provider.ContextPrompter

class PythonContextPrompter : ContextPrompter() {
    override fun displayPrompt(): String {
        val chunkContext = SimilarChunksWithPaths().similarChunksWithPaths(file!!).toQuery()
        return """$action for the code:
            ```${lang}
            $chunkContext
            $selectedText
            ```
            """.trimIndent()
    }

    override fun requestPrompt(): String {
        val chunkContext = SimilarChunksWithPaths().similarChunksWithPaths(file!!).toQuery()
        return """$action for the code:
            ```${lang}
            $chunkContext
            $selectedText
            ```
            """.trimIndent()
    }
}
