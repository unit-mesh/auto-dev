package cc.unitmesh.idea.kotlin

import cc.unitmesh.devti.context.chunks.SimilarChunksWithPaths
import cc.unitmesh.devti.gui.chat.ChatBotActionType
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class KotlinContextPrompter : ContextPrompter() {
    private var action: ChatBotActionType? = null
    private var selectedText: String = ""
    private var file: PsiFile? = null
    private var project: Project? = null
    private var lang: String = ""

    override fun initContext(actionType: ChatBotActionType, prefixText: String, file: PsiFile?, project: Project) {
        this.action = actionType
        this.selectedText = prefixText
        this.file = file
        this.project = project
        this.lang = file?.language?.displayName ?: ""
    }

    override fun getUIPrompt(): String {
        val chunkContext = SimilarChunksWithPaths.createQuery(file!!) ?: ""

        return """$action for the code:
            ```${lang} $chunkContext
            $selectedText
            ```
            """.trimIndent()
    }

    override fun getRequestPrompt(): String {
        val chunkContext = SimilarChunksWithPaths.createQuery(file!!) ?: ""

        return """$action for the code:
            ```${lang} $chunkContext
            $selectedText
            ```
            """.trimIndent()
    }

    companion object {
        val logger = Logger.getInstance(KotlinContextPrompter::class.java)
    }
}
