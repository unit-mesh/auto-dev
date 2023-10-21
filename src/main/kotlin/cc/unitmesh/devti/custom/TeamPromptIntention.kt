package cc.unitmesh.devti.custom

import cc.unitmesh.devti.custom.team.TeamPromptAction
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.intentions.action.base.AbstractChatIntention
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

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
        val withRange = elementWithRange(editor, file, project) ?: return

        val selectedText = withRange.first
        val psiElement = withRange.second
        // compile template here ?
        val msgs = intentionConfig.actionPrompt.msgs

        // TODO: handle by interaction type
        sendToChatPanel(project) { panel, service ->
            service.handleMsgsAndResponse(panel, msgs)
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
