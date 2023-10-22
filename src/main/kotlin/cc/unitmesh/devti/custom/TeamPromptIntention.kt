package cc.unitmesh.devti.custom

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.LLMCoroutineScope
import cc.unitmesh.devti.custom.team.InteractionType
import cc.unitmesh.devti.custom.team.TeamPromptAction
import cc.unitmesh.devti.custom.team.TeamPromptTemplateCompiler
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.intentions.action.base.AbstractChatIntention
import cc.unitmesh.devti.intentions.action.task.CodeCompletionRequest
import cc.unitmesh.devti.intentions.action.task.CodeCompletionTask
import cc.unitmesh.devti.llms.LlmProviderFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch

class TeamPromptIntention(private val intentionConfig: TeamPromptAction) : AbstractChatIntention() {
    override fun getActionType(): ChatActionType {
        return ChatActionType.CUSTOM_ACTION
    }

    companion object {
        fun create(intentionConfig: TeamPromptAction): TeamPromptIntention {
            return TeamPromptIntention(intentionConfig)
        }
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val range = elementWithRange(editor, file, project) ?: return

        val language = file.language
        val element = file.findElementAt(editor.caretModel.offset)
        val offset = editor.caretModel.offset

        val templateCompiler = TeamPromptTemplateCompiler(language, file, element, editor)
        templateCompiler.set("selection", range.first)
        templateCompiler.set("beforeCursor", file.text.substring(0, editor.caretModel.offset))
        templateCompiler.set("afterCursor", file.text.substring(editor.caretModel.offset))

        val msgs = intentionConfig.actionPrompt.msgs.map {
            it.copy(content = templateCompiler.compile(it.content))
        }

        when (intentionConfig.actionPrompt.interaction) {
            InteractionType.ChatPanel -> {
                sendToChatPanel(project) { panel, service ->
                    service.handleMsgsAndResponse(panel, msgs)
                }
            }

            InteractionType.AppendCursor,
            InteractionType.AppendCursorStream -> {
                val msgString = msgs.joinToString("\n") { it.content }
                val request = CodeCompletionRequest.create(editor, offset, element!!, msgString, "") ?: return
                val task = CodeCompletionTask(request)
                ProgressManager.getInstance()
                    .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
            }
        }
    }

    override fun priority(): Int {
        return intentionConfig.actionPrompt.priority
    }

    override fun getText(): String {
        return intentionConfig.actionName
    }

    override fun getFamilyName(): String {
        return intentionConfig.actionName
    }
}
