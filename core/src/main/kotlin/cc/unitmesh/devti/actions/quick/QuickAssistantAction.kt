package cc.unitmesh.devti.actions.quick

import cc.unitmesh.devti.custom.TeamPromptBaseIntention
import cc.unitmesh.devti.custom.team.TeamPromptAction
import cc.unitmesh.devti.custom.team.TeamPromptsBuilder
import cc.unitmesh.devti.gui.quick.QuickPromptField
import cc.unitmesh.devti.gui.quick.QuickPromptField.Companion.QUICK_ASSISTANT_CANCEL_ACTION
import cc.unitmesh.devti.gui.quick.QuickPromptField.Companion.QUICK_ASSISTANT_SUBMIT_ACTION
import cc.unitmesh.devti.intentions.action.task.BaseCompletionTask
import cc.unitmesh.devti.intentions.action.task.CodeCompletionRequest
import cc.unitmesh.devti.settings.LanguageChangedCallback.presentationText
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.temporary.inlay.InlayPanel
import java.awt.event.ActionEvent
import javax.swing.AbstractAction

/**
 * A quick insight action is an action that can be triggered by a user,
 * user can input custom text to call with LLM.
 */
open class QuickAssistantAction : AnAction() {
    init{
        presentationText("settings.autodev.others.quickAssistant", templatePresentation)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.getData(CommonDataKeys.PSI_FILE)?.isWritable ?: false
    }


    override fun actionPerformed(e: AnActionEvent) {
        val dataContext = e.dataContext
        val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return
        val offset = editor.caretModel.offset
        val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return
        val element = e.getData(CommonDataKeys.PSI_ELEMENT)
        val sourceFile = dataContext.getData(CommonDataKeys.PSI_FILE)

        val quickPrompts = project.service<TeamPromptsBuilder>().quickPrompts()

        if (quickPrompts.isEmpty()) {
            useInlayMode(editor, offset, project, element)
        } else {
            useQuickMode(editor, quickPrompts, project, sourceFile)
        }
    }

    private fun useQuickMode(
        editor: Editor,
        quickPrompts: List<TeamPromptAction>,
        project: Project,
        sourceFile: PsiFile?
    ) {
        val cursorPosition = editor.visualPositionToXY(editor.caretModel.visualPosition)

        val promptIntentions: List<TeamPromptBaseIntention> = quickPrompts.map {
            TeamPromptBaseIntention.create(it, trySelectElement = false)
        }

        var awareActions: Array<AnAction> = arrayOf()
        val categoryMap = promptIntentions.groupBy {
            it.intentionConfig.actionPrompt.other["category"] ?: ""
        }

        categoryMap.forEach { (category, intentions) ->
            val intentionList = intentions.map { action ->
                val actionName: String? = action.intentionConfig.actionPrompt.other["name"] as? String
                val actionText: String = actionName ?: action.text
                DumbAwareAction.create(actionText) {
                    action.invoke(project, editor, sourceFile)
                }
            }.toTypedArray()

            awareActions += intentionList
            awareActions += Separator.getInstance()
        }

        val popupMenu = ActionManager.getInstance()
            .createActionPopupMenu("QuickAction", object : ActionGroup("QuickAction", true),
                DumbAware {
                override fun getChildren(e: AnActionEvent?): Array<AnAction> = awareActions
            })

        popupMenu.component.show(editor.contentComponent, cursorPosition.x, cursorPosition.y)
    }

    private fun useInlayMode(editor: Editor, offset: Int, project: Project, element: PsiElement?) {
        InlayPanel.add(editor as EditorEx, offset, QuickPromptField())?.let {
            doExecute(it, project, editor, element)
        }
    }

    private fun doExecute(
        inlay: InlayPanel<QuickPromptField>,
        project: Project,
        editor: EditorEx,
        element: PsiElement?,
    ) {
        val component = inlay.component

        val actionMap = component.actionMap

        val language = element?.language?.displayName ?: ""

        actionMap.put(QUICK_ASSISTANT_SUBMIT_ACTION, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                val text =
                    """Generate a concise $language code snippet with no extra text, description, or comments. 
                        | The code should achieve the following task: ${component.getText()}"""
                        .trimMargin()

                val offset = editor.caretModel.offset

                val request = runReadAction {
                    CodeCompletionRequest.create(editor, offset, element, null, text)
                } ?: return

                val task = object : BaseCompletionTask(request) {
                    override fun keepHistory(): Boolean = false
                    override fun promptText(): String = text
                }

                ProgressManager.getInstance()
                    .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))

                Disposer.dispose(inlay.inlay!!)
            }
        })

        actionMap.put(QUICK_ASSISTANT_CANCEL_ACTION, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                Disposer.dispose(inlay.inlay!!)
            }
        })

        IdeFocusManager.getInstance(project).requestFocus(component, false)
        EditorUtil.disposeWithEditor(editor, inlay.inlay!!)
    }
}

