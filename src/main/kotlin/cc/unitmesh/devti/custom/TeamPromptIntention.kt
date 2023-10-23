package cc.unitmesh.devti.custom

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.devti.custom.team.InteractionType
import cc.unitmesh.devti.custom.team.TeamPromptAction
import cc.unitmesh.devti.custom.team.TeamPromptTemplateCompiler
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.intentions.action.base.AbstractChatIntention
import cc.unitmesh.devti.intentions.action.task.BaseCompletionTask
import cc.unitmesh.devti.intentions.action.task.CodeCompletionRequest
import cc.unitmesh.devti.intentions.action.task.CodeCompletionTask
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.temporary.calculateFrontendElementToExplain
import io.kotest.mpp.file

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
        val textRange = getCurrentSelectionAsRange(editor)
        val element = calculateFrontendElementToExplain(project, file, textRange)

        val offset = editor.caretModel.offset

        val templateCompiler = TeamPromptTemplateCompiler(language, file, element, editor)
        templateCompiler.set("selection", range.first)
        templateCompiler.set("beforeCursor", file.text.substring(0, editor.caretModel.offset))
        templateCompiler.set("afterCursor", file.text.substring(editor.caretModel.offset))

        val msgs = intentionConfig.actionPrompt.msgs.map {
            it.copy(content = templateCompiler.compile(it.content))
        }

        val userPrompt = msgs.filter { it.role == LlmMsg.ChatRole.User }.joinToString("\n") { it.content }
        val systemPrompt = msgs.filter { it.role == LlmMsg.ChatRole.System }.joinToString("\n") { it.content }

        when (intentionConfig.actionPrompt.interaction) {
            InteractionType.ChatPanel -> {
                sendToChatPanel(project) { panel, service ->
                    service.handleMsgsAndResponse(panel, msgs)
                }
            }

            InteractionType.AppendCursor,
            InteractionType.AppendCursorStream,
            -> {
                val msgString = systemPrompt + "\n" + userPrompt
                val request = CodeCompletionRequest.create(editor, offset, element!!, msgString, "") ?: return
                val task = object : BaseCompletionTask(request) {
                    override fun keepHistory(): Boolean = false
                    override fun promptText(): String = msgString
                }

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
