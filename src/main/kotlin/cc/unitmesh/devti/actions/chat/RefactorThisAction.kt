package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement


class RefactorThisAction : ChatBaseAction() {
    init{
        val presentation = getTemplatePresentation()
        presentation.text = AutoDevBundle.message("settings.autodev.rightClick.refactor")
    }
    override fun getActionType(): ChatActionType = ChatActionType.REFACTOR
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.PSI_FILE)
        val project = e.getData(CommonDataKeys.PROJECT)

        if (editor == null || file == null || project == null) {
            e.presentation.isEnabled = false
            return
        }

        if (file.isWritable) {
            e.presentation.isEnabled = true
            return
        }

        e.presentation.isEnabled = false
    }

    override fun addAdditionPrompt(project: Project, editor: Editor, element: PsiElement): String {
        val commentSymbol = commentPrefix(element)

        //todo: prompts
        return collectProblems(project, editor, element)?.let {
            "\n\n$commentSymbol relative static analysis result: $it"
        } ?: ""
    }

    private fun commentPrefix(element: PsiElement): String {
        return LanguageCommenters.INSTANCE.forLanguage(element.language)?.lineCommentPrefix ?: "//"
    }

    /**
     * Collects all the problems found in the given `project`, within the specified `editor` and `element`.
     *
     * @param project The project in which the problems are to be collected.
     * @param editor The editor that is associated with the element.
     * @param element The PsiElement for which the problems are to be collected.
     * @return A string containing all the problems found, separated by new lines, or `null` if no problems were found.
     */
    private fun collectProblems(project: Project, editor: Editor, element: PsiElement): String? {
        val range = element.textRange
        val document = editor.document
        val errors: MutableList<String> = mutableListOf()
        DaemonCodeAnalyzerEx.processHighlights(
            document,
            project,
            null,
            range.startOffset,
            range.endOffset
        ) { info ->
            if (info.description != null) {
                errors.add(info.description)
            }

            true
        }

        val commentSymbol = commentPrefix(element)
        return errors.joinToString("\n") {
            "$commentSymbol - $it"
        }
    }

    private val refactorIntentionsKeys = arrayOf(
        "intentions.refactor.readability",
        "intentions.refactor.usability",
        "intentions.refactor.performance",
        "intentions.refactor.maintainability",
        "intentions.refactor.flexibility",
        "intentions.refactor.reusability",
        "intentions.refactor.accessibility"
    )

    override fun chatCompletedPostAction(event: AnActionEvent, panel: ChatCodingPanel): (response: String) -> Unit {
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val project = event.getRequiredData(CommonDataKeys.PROJECT)
        val document = editor.document

        val primaryCaret = editor.caretModel.primaryCaret;
        val start = primaryCaret.selectionStart;
        val end = primaryCaret.selectionEnd

        return { response ->
            WriteCommandAction.runWriteCommandAction(project) {
                document.replaceString(start, end, response)
            }
        }
    }
}
