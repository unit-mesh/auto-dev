package cc.unitmesh.devti.custom

import cc.unitmesh.devti.custom.team.TeamPromptAction
import cc.unitmesh.devti.custom.team.TeamPromptExecTask
import cc.unitmesh.devti.custom.team.TeamPromptTemplateCompiler
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.intentions.action.base.AbstractChatIntention
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.temporary.calculateFrontendElementToExplain

/**
 * The `TeamPromptIntention` class represents an intention for team prompts in a chat application.
 * It extends the `AbstractChatIntention` class and provides functionality to handle team prompt actions.
 *
 * @property intentionConfig The configuration for the team prompt action.
 *
 * @constructor Creates a `TeamPromptIntention` with the specified intention configuration.
 *
 * @param intentionConfig The configuration for the team prompt action.
 *
 */
class TeamPromptIntention(private val intentionConfig: TeamPromptAction) : AbstractChatIntention() {

    override fun priority(): Int = intentionConfig.actionPrompt.priority

    override fun getText(): String = intentionConfig.actionName

    override fun getFamilyName(): String = intentionConfig.actionName

    override fun getActionType(): ChatActionType = ChatActionType.CUSTOM_ACTION

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

        val templateCompiler = TeamPromptTemplateCompiler(language, file, element, editor)
        templateCompiler.set("selection", range.first)
        templateCompiler.set("beforeCursor", file.text.substring(0, editor.caretModel.offset))
        templateCompiler.set("afterCursor", file.text.substring(editor.caretModel.offset))

        val msgs = intentionConfig.actionPrompt.msgs.map {
            it.copy(content = templateCompiler.compile(it.content))
        }

        val task: Task.Backgroundable = TeamPromptExecTask(project, msgs, editor, intentionConfig, element)
        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }
}
