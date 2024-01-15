package cc.unitmesh.ide.javascript.provider

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import kotlinx.coroutines.runBlocking

class JavaScriptContextPrompter : ContextPrompter() {
    private lateinit var creationContext: ChatCreationContext
    private var additionContext = ""

    companion object {
        val log = logger<JavaScriptContextPrompter>()
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
        creationContext = ChatCreationContext(ChatOrigin.ChatAction, action!!, file, listOf(), element)
    }

    override fun displayPrompt(): String {

        return runBlocking {
            additionContext = collectionContext(creationContext)
            return@runBlocking "${action!!.instruction(lang, project)}\n```$lang\n$selectedText\n```"
        }
    }

    override fun requestPrompt(): String {
        return runBlocking {
            additionContext = collectionContext(creationContext)
            val finalPrompt = "${action!!.instruction(lang, project)}:\n$additionContext\n```${lang}\n$selectedText\n```"
            log.info("context: $finalPrompt")
            return@runBlocking finalPrompt
        }
    }
}
