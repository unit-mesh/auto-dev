package cc.unitmesh.devti.custom

import cc.unitmesh.devti.custom.team.TeamPromptAction
import cc.unitmesh.devti.custom.team.TeamPromptTemplateCompiler
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.intentions.action.base.AbstractChatIntention
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
        val range = elementWithRange(editor, file, project) ?: return

        val language = file.language
        val element = file.findElementAt(editor.caretModel.offset)

        val templateCompiler = TeamPromptTemplateCompiler(language, file, element, editor)
        templateCompiler.set("selection", range.first)
        templateCompiler.set("beforeCursor", file.text.substring(0, editor.caretModel.offset))
        templateCompiler.set("afterCursor", file.text.substring(editor.caretModel.offset))

        val msgs = intentionConfig.actionPrompt.msgs.map {
            it.copy(content = templateCompiler.compile(it.content))
        }

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
