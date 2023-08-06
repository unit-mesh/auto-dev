package cc.unitmesh.clion.provider

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.coroutines.runBlocking

class RustContextPrompter : ContextPrompter() {
    private lateinit var creationContext: ChatCreationContext
    private var additionContext = ""

    companion object {
        val log = logger<RustContextPrompter>()
    }

    override fun initContext(
        actionType: ChatActionType,
        selectedText: String,
        file: PsiFile?,
        project: Project,
        offset: Int
    ) {
        super.initContext(actionType, selectedText, file, project, offset)
        creationContext = ChatCreationContext(ChatOrigin.ChatAction, action!!, file)
    }

    override fun displayPrompt(): String {

        return runBlocking {
            additionContext = collectionContext(creationContext)
            return@runBlocking "${action!!.instruction(lang)}\n```$lang\n$selectedText\n```"
        }
    }

    override fun requestPrompt(): String {
        return runBlocking {
            additionContext = collectionContext(creationContext)
            val finalPrompt = "${action!!.instruction(lang)}:\n$additionContext\n```${lang}\n$selectedText\n```"
            log.info("context: $finalPrompt")
            return@runBlocking finalPrompt
        }
    }
}