package cc.unitmesh.pycharm.provider

import cc.unitmesh.devti.context.chunks.SimilarChunksWithPaths
import cc.unitmesh.devti.gui.chat.ChatBotActionType
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class PythonContextPrompter : ContextPrompter() {
    override fun getUIPrompt(): String {
        val chunkContext = SimilarChunksWithPaths().similarChunksWithPaths(file!!).toQuery()

        return """$action for the code:
            ```${lang}
            $chunkContext
            $selectedText
            ```
            """.trimIndent()
    }

    override fun getRequestPrompt(): String {
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
