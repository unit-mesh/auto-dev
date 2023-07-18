package cc.unitmesh.ide.pycharm.provider

import cc.unitmesh.devti.context.FileContextProvider
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
    private val fileContextProvider = FileContextProvider()

    override fun initContext(actionType: ChatBotActionType, prefixText: String, file: PsiFile?, project: Project) {
        this.action = actionType
        this.selectedText = prefixText
        this.file = file
        this.project = project
        this.lang = file?.language?.displayName ?: ""
    }

    override fun getUIPrompt(): String {
        val classInfo = fileContextProvider.from(file!!).toQuery()
        logger.warn("classInfo: $classInfo")
        val chunkContext = SimilarChunksWithPaths().similarChunksWithPaths(file!!).toQuery()
        logger.warn("chunkContext: $chunkContext")

        return """$action for the code:
            $classInfo
            ```${lang}
            $selectedText
            ```
            """.trimIndent()
    }

    override fun getRequestPrompt(): String {
        val classInfo = fileContextProvider.from(file!!).toQuery()
        logger.warn("classInfo: $classInfo")

        return """$action for the code:
            $classInfo
            ```${lang}
            $selectedText
            ```
            """.trimIndent()
    }

    companion object {
        val logger = Logger.getInstance(PythonContextPrompter::class.java)
    }
}
