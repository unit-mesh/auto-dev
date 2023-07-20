package cc.unitmesh.pycharm.provider

import cc.unitmesh.devti.context.chunks.SimilarChunksWithPaths
import cc.unitmesh.devti.gui.chat.ChatBotActionType
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class PythonContextPrompter : ContextPrompter() {
    private var action: ChatBotActionType? = null
    private var selectedText: String = ""
    private var file: PsiFile? = null
    private var project: Project? = null
    private var lang: String = ""

    override fun initContext(actionType: ChatBotActionType, text: String, file: PsiFile?, project: Project, offset: Int) {
        this.action = actionType
        this.selectedText = text
        this.file = file
        this.project = project
        this.lang = file?.language?.displayName ?: ""
    }

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
