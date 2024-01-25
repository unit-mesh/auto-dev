package cc.unitmesh.python.provider

import com.intellij.temporary.similar.chunks.SimilarChunksWithPaths
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import kotlinx.coroutines.runBlocking

class PythonContextPrompter : ContextPrompter() {
    private lateinit var creationContext: ChatCreationContext
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
        element: PsiElement?,
    ) {
        super.initContext(actionType, selectedText, file, project, offset, element)
        additionContext = SimilarChunksWithPaths.createQuery(file!!) ?: ""
        creationContext = ChatCreationContext(ChatOrigin.ChatAction, action!!, file, listOf(), element)
    }

    override fun displayPrompt(): String {
        return runBlocking {
            additionContext = collectionContext(creationContext)
            return@runBlocking "$action\n```${lang}\n$selectedText\n```"
        }
    }

    override fun requestPrompt(): String {
        return runBlocking {
            additionContext = collectionContext(creationContext)
            val prompt = "$action\n```${lang}\n$additionContext\n$selectedText\n```"
            log.info("final prompt: $prompt")
            return@runBlocking prompt
        }
    }
}
