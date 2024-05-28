package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.provider.RefactoringTool
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

open class RefactorThisAction : ChatBaseAction() {
    init {
        getTemplatePresentation().text = AutoDevBundle.message("settings.autodev.rightClick.refactor")
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
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

        val staticCodeResults = collectProblems(project, editor, element)?.let {
            "\n\n$commentSymbol relative static analysis result:\n$it"
        } ?: ""

        val refactoringTool = RefactoringTool.forLanguage(element.language)
        refactoringTool ?: return staticCodeResults

        val devinRefactorPrompt: String =
            """```
                |- You should summary in the end with `DevIn` language in markdown fence-code block, I will handle it.
                |- the DevIn language current only support rename method.
                |- If you had rename method name or class name, return follow format:
                |```DevIn
                |/refactor:rename <sourceMethodName> to <targetMethodName> // method and class only
            """.trimMargin()

        return staticCodeResults + devinRefactorPrompt
    }

    open fun commentPrefix(element: PsiElement): String {
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
    open fun collectProblems(project: Project, editor: Editor, element: PsiElement): String? {
        val range = element.textRange
        val document = editor.document
        var errors: MutableList<String> = mutableListOf()
        DaemonCodeAnalyzerEx.processHighlights(document, project, null, range.startOffset, range.endOffset) {
            if (it.description != null) {
                errors.add(it.description)
            }

            true
        }

        val commentSymbol = commentPrefix(element)
        // remove dupcliated descriptions
        errors = errors.distinct().toMutableList()
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
        // get random key from refactorIntentionsKeys
        val key = refactorIntentionsKeys.random()
        val msg = AutoDevBundle.message(key)

        return {
            panel.showSuggestion(msg)
        }
    }
}
