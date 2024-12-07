package cc.unitmesh.devti.actions.completion

import com.intellij.temporary.inlay.codecomplete.presentation.EditorUtilCopy
import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorAction
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.temporary.inlay.codecomplete.LLMInlayManager
import java.awt.event.KeyEvent

class LLMApplyInlaysAction : EditorAction(ApplyInlaysHandler()), DumbAware {
    init {
        setInjectedContext(true)
    }

    override fun update(e: AnActionEvent) {
        if (isIgnoredKeyboardEvent(e)) {
            e.presentation.isEnabled = false
            return
        }

        super.update(e)
    }

    private fun isIgnoredKeyboardEvent(e: AnActionEvent): Boolean {
        if (e.inputEvent !is KeyEvent) return false
        if ((e.inputEvent as KeyEvent).keyChar != '\t') return false

        val project = e.project ?: return false
        val editor = getEditor(e.dataContext) ?: return false

        val document = editor.document
        val blockIndent = CodeStyle.getIndentOptions(project, document).INDENT_SIZE
        val caretOffset = editor.caretModel.offset
        val line = document.getLineNumber(caretOffset)

        val caretOffsetAfterTab = EditorUtilCopy.indentLine(project, editor, line, blockIndent, caretOffset)
//        if (isNonEmptyLinePrefix(document, line, caretOffset) || caretOffsetAfterTab < caretOffset) {
//            return false
//        }

        val instance = LLMInlayManager.getInstance()
        val tabRange = TextRange.create(caretOffset, caretOffsetAfterTab)

        return instance.countCompletionInlays(editor, tabRange) <= 0
    }


    private class ApplyInlaysHandler : EditorActionHandler() {
        override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext): Boolean {
            return isSupported(editor)
        }

        override fun executeInCommand(editor: Editor, dataContext: DataContext): Boolean {
            return false
        }

        override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
            if (editor.isDisposed) return
            val project = editor.project ?: return
            if (project.isDisposed) return


            // todo: update this to use the new API
            logger.info("doExecute for applyInlays")
            LLMInlayManager.getInstance().applyCompletion(project, editor)
        }
    }

    companion object {
        const val ID = "llm.applyInlays"

        private val logger = logger<LLMApplyInlaysAction>()

        private fun isSpaceOrTab(c: Char, withNewline: Boolean): Boolean {
            return c == ' ' || c == '\t' || withNewline && c == '\n'
        }

        private fun isSpacesOrTabs(text: CharSequence, withNewlines: Boolean): Boolean {
            for (element in text) {
                if (!isSpaceOrTab(element, withNewlines)) {
                    return false
                }
            }
            return true
        }

        private fun isNonEmptyLinePrefix(document: Document, lineNumber: Int, caretOffset: Int): Boolean {
            val lineStartOffset = document.getLineStartOffset(lineNumber)
            if (lineStartOffset == caretOffset) {
                return false
            }
            val linePrefix = document.getText(TextRange.create(lineStartOffset, caretOffset))
            return !isSpacesOrTabs(linePrefix, false)
        }

        fun isSupported(editor: Editor): Boolean {
            val project = editor.project
            return project != null && editor
                .caretModel.caretCount == 1 && (LookupManager.getActiveLookup(editor) == null) && TemplateManager.getInstance(
                project
            )
                .getActiveTemplate(editor) == null
        }
    }
}
