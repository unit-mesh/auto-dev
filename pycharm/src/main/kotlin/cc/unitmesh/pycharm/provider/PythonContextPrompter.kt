package cc.unitmesh.pycharm.provider

import cc.unitmesh.devti.context.chunks.SimilarChunksWithPaths
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class PythonContextPrompter : ContextPrompter() {
    private var additionContext: String = ""

    companion object {
        val log = logger<PythonContextPrompter>()
    }

    override fun initContext(
        actionType: ChatActionType,
        selectedText: String,
        file: PsiFile?,
        project: Project,
        offset: Int,
        element: PsiElement?
    ) {
        super.initContext(actionType, selectedText, file, project, offset, element)
        additionContext = SimilarChunksWithPaths.createQuery(file!!) ?: ""
    }

    override fun displayPrompt(): String {
        return "$action\n```${lang}\n$selectedText\n```"
    }

    override fun requestPrompt(): String {
        val prompt = "$action\n```${lang}\n$additionContext\n$selectedText\n```"
        log.info("final prompt: $prompt")
        return prompt
    }
}
