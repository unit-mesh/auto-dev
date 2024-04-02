package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.ChatActionType
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement


class RefactorThisAction : ChatBaseAction() {
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

    override fun addAdditionInfo(project: Project, editor: Editor, element: PsiElement): String {
        return collectProblems(project, editor, element)?.let {
            "\n\n// relative static analysis result: $it"
        } ?: ""
    }

    fun collectProblems(project: Project, editor: Editor, element: PsiElement): String? {
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

        return errors.joinToString("\n") {
            "// - $it"
        }
    }

    override fun getReplaceableAction(event: AnActionEvent): (response: String) -> Unit {
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
