package cc.unitmesh.pycharm.provider

import cc.unitmesh.devti.context.chunks.SimilarChunksWithPaths
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.diagnostic.Logger

class PythonContextPrompter : ContextPrompter() {
    override fun createDisplayPrompt(): String {
        val chunkContext = SimilarChunksWithPaths().similarChunksWithPaths(file!!).toQuery()

        return """$action for the code:
            ```${lang}
            $chunkContext
            $selectedText
            ```
            """.trimIndent()
    }

    override fun createRequestPrompt(): String {
        val chunkContext = SimilarChunksWithPaths().similarChunksWithPaths(file!!).toQuery()

        return """$action for the code:
            ```${lang}
            $chunkContext
            $selectedText
            ```
            """.trimIndent()
    }

    companion object {
        val logger = Logger.getInstance(PythonContextPrompter::class.java)
    }
}
