package cc.unitmesh.idea.kotlin

import cc.unitmesh.devti.context.chunks.SimilarChunksWithPaths
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.diagnostic.Logger

class KotlinContextPrompter : ContextPrompter() {
    override fun getUIPrompt(): String {
        val chunkContext = getChunks()

        return """$action for the code:
            ```${lang} $chunkContext
            $selectedText
            ```
            """.trimIndent()
    }

    override fun getRequestPrompt(): String {
        val chunkContext = getChunks()

        return """$action for the code:
            ```${lang} $chunkContext
            $selectedText
            ```
            """.trimIndent()
    }

    private fun getChunks(): String {
        val psiElement = file?.findElementAt(offset)
        val chunkContext = SimilarChunksWithPaths.createQuery(psiElement!!) ?: ""
        return chunkContext
    }

    companion object {
        val logger = Logger.getInstance(KotlinContextPrompter::class.java)
    }
}
