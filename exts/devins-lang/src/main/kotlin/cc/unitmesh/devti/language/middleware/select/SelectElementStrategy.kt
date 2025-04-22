package cc.unitmesh.devti.language.middleware.select

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace

sealed class SelectedEntry(open val element: Any) {
    class Text(override val element: String) : SelectedEntry(element)
    class Entry(override val element: PsiElement) : SelectedEntry(element)
}

sealed class SelectElementStrategy {
    /**
     * Selection element
     */
    abstract fun select(project: Project, editor: Editor?): Any?

    abstract fun getSelectedElement(project: Project, editor: Editor?): SelectedEntry?

    /**
     * Auto select parent block element, like function, class, etc.
     */
    object Blocked : SelectElementStrategy() {
        override fun select(project: Project, editor: Editor?): PsiElement? {
            if (editor == null) {
                return null
            }

            val elementToAction = DefaultPsiElementStrategy().getElementToAction(project, editor) ?: return null

            runInEdt {
                selectElement(elementToAction, editor)
            }

            return elementToAction
        }

        /**
         * This function selects the specified PsiElement in the editor by setting the selection range from the start offset to the end offset of the element.
         *
         * @param elementToExplain the PsiElement to be selected in the editor
         * @param editor the Editor in which the selection is to be made
         */
        private fun selectElement(elementToExplain: PsiElement, editor: Editor) {
            val startOffset = elementToExplain.textRange.startOffset
            val endOffset = elementToExplain.textRange.endOffset

            editor.selectionModel.setSelection(startOffset, endOffset)
        }

        override fun getSelectedElement(project: Project, editor: Editor?): SelectedEntry? {
            select(project, editor)?.let {
                return SelectedEntry.Entry(it)
            }

            return null
        }
    }

    object SelectedText : SelectElementStrategy() {
        override fun select(project: Project, editor: Editor?): @NlsSafe String? {
            return editor?.selectionModel?.selectedText ?: ""
        }

        override fun getSelectedElement(project: Project, editor: Editor?): SelectedEntry? {
            val selectedText = select(project, editor) ?: return null
            return SelectedEntry.Text(selectedText)
        }
    }

    object Default : SelectElementStrategy() {
        override fun select(project: Project, editor: Editor?): PsiElement? {
            val selectionModel = editor?.selectionModel ?: return null
            if (!selectionModel.hasSelection()) {
                return Blocked.select(project, editor)
            }

            return null
        }

        override fun getSelectedElement(project: Project, editor: Editor?): SelectedEntry? {
            return Blocked.getSelectedElement(project, editor)
        }
    }

    object SelectAll : SelectElementStrategy() {
        override fun select(project: Project, editor: Editor?): String? {
            val selectionModel = editor?.selectionModel ?: return null

            runInEdt {
                selectionModel.setSelection(0, editor.document.textLength)
            }

            return editor.document.text
        }

        override fun getSelectedElement(project: Project, editor: Editor?): SelectedEntry {
            return SelectedEntry.Text(editor?.document?.text ?: "")
        }
    }

    companion object {
        fun fromString(strategy: String): SelectElementStrategy? {
            return when (strategy.lowercase()) {
                "block" -> Blocked
                "select" -> SelectedText
                "selectAll" -> SelectAll
                else -> null
            }
        }

        fun all(): List<String> {
            return SelectElementStrategy::class.sealedSubclasses.map { it.simpleName!! }
        }

        fun getElementAtOffset(psiFile: PsiElement, offset: Int): PsiElement? {
            var element = psiFile.findElementAt(offset) ?: return null

            if (element is PsiWhiteSpace) {
                element = element.getParent()
            }

            return element
        }

        fun resolvePsiElement(myProject: Project, editor: Editor): PsiElement? {
            val elementToAction = DefaultPsiElementStrategy().getElementToAction(myProject, editor)

            if (elementToAction != null) {
                return elementToAction
            }

            return null
        }

    }
}
