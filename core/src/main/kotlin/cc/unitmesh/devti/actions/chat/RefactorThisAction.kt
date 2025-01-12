package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.actions.chat.base.ChatCheckForUpdateAction
import cc.unitmesh.devti.actions.chat.base.collectProblems
import cc.unitmesh.devti.actions.chat.base.commentPrefix
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.provider.RefactoringTool
import cc.unitmesh.devti.settings.LanguageChangedCallback.presentationText
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

open class RefactorThisAction : ChatCheckForUpdateAction() {
    init {
        presentationText("settings.autodev.rightClick.refactor", templatePresentation)
    }

    override fun getActionType(): ChatActionType = ChatActionType.REFACTOR
    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE)
        if (file != null && file.isWritable) {
            super.update(e)
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
        val key = refactorIntentionsKeys.random()
        val msg = AutoDevBundle.message(key)

        return {
            panel.showSuggestion(msg)
        }
    }
}
