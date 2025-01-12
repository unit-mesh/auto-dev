package cc.unitmesh.devti.custom

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.custom.team.TeamPromptAction
import cc.unitmesh.devti.custom.team.TeamPromptExecTask
import cc.unitmesh.devti.custom.compile.VariableTemplateCompiler
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.intentions.action.base.ChatBaseIntention
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElement
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
class TeamPromptBaseIntention(val intentionConfig: TeamPromptAction, val trySelectElement: Boolean) :
    ChatBaseIntention() {
    override fun priority(): Int = intentionConfig.actionPrompt.priority
    override fun getText(): String = intentionConfig.actionName
    override fun getFamilyName(): String = intentionConfig.actionName
    override fun getActionType(): ChatActionType = ChatActionType.CUSTOM_ACTION

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val elementPair = elementWithRange(editor, file, project)
        val textRange = getCurrentSelectionAsRange(editor)

        val language = file.language
        val element = if (trySelectElement) {
            calculateFrontendElementToExplain(project, file, textRange)
        } else {
            elementPair?.second
        }

        val compiler = VariableTemplateCompiler(language, file, element, editor, elementPair?.first ?: "")

        val actionPrompt = intentionConfig.actionPrompt
        val chatMessages = actionPrompt.msgs


        if (actionPrompt.batchFileRegex != "") {
            val files = actionPrompt.batchFiles(project)
            if (files.isNotEmpty()) {
                val length = files.size
                files.forEachIndexed { index, vfile ->
                    compiler.set("all", VfsUtilCore.loadText(vfile))
                    val msgs = chatMessages.map {
                        it.copy(content = compiler.compile(it.content))
                    }

                    // display progress like 1/2 in the title
                    val taskName = "${intentionConfig.actionName} ${index + 1}/$length "
                    val task: Task.Backgroundable =
                        TeamPromptExecTask(project, msgs, editor, intentionConfig, element, vfile, taskName)
                    ProgressManager.getInstance()
                        .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
                }
            } else {
                AutoDevNotifications.error(
                    project,
                    "No files found for batch processing, please check the regex. " + "Regex: ${actionPrompt.batchFileRegex}"
                )
            }
        } else {
            val msgs = chatMessages.map {
                it.copy(content = compiler.compile(it.content))
            }

            executeSingleJob(project, msgs, editor, element)
        }
    }

    private fun executeSingleJob(
        project: Project,
        msgs: List<LlmMsg.ChatMessage>,
        editor: Editor,
        element: PsiElement?
    ) {
        val task: Task.Backgroundable = TeamPromptExecTask(project, msgs, editor, intentionConfig, element, null)
        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    companion object {
        fun create(intentionConfig: TeamPromptAction, trySelectElement: Boolean = true): TeamPromptBaseIntention =
            TeamPromptBaseIntention(intentionConfig, trySelectElement)
    }
}
