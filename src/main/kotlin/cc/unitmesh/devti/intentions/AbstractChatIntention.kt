package cc.unitmesh.devti.intentions

import cc.unitmesh.devti.gui.chat.*
import cc.unitmesh.devti.toolwindow.sendToChat
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase

abstract class AbstractChatIntention : IntentionAction {
    open fun getActionType() = ChatActionType.CODE_COMPLETE

    open val prompt: String = "Code completion"

    override fun startInWriteAction(): Boolean = false

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean =
        editor != null && file != null

    /**
     * Invokes the given method with the specified parameters.
     *
     * @param project The current project.
     * @param editor The editor in which the method is invoked.
     * @param file The file in which the method is invoked.
     */
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return

        var selectedText = editor.selectionModel.selectedText
        val elementToExplain = getElementToAction(project, editor)

        if (selectedText == null) {
            if (elementToExplain == null) {
                return
            }
            selectElement(elementToExplain, editor)
            selectedText = editor.selectionModel.selectedText
        }

        if (selectedText == null) {
            return
        }

        val actionType = getActionType()

        val prompter = ContextPrompter.prompter(file.language.displayName)
        prompter.initContext(actionType, selectedText, file, project, editor.caretModel.offset)
        sendToChat(project, actionType, prompter)
    }

    protected fun selectElement(elementToExplain: PsiElement, editor: Editor) {
        val startOffset = elementToExplain.textRange.startOffset
        val endOffset = elementToExplain.textRange.endOffset

        editor.selectionModel.setSelection(startOffset, endOffset)
    }

    /**
     * Returns the PsiElement to explain in the given project and editor.
     *
     * @param project the project in which the element resides (nullable)
     * @param editor the editor in which the element is located (nullable)
     * @return the PsiElement to explain, or null if either the project or editor is null, or if no element is found
     */
    protected open fun getElementToAction(project: Project?, editor: Editor?): PsiElement? {
        if (project == null || editor == null) return null

        val element = PsiUtilBase.getElementAtCaret(editor) ?: return null
        val psiFile = element.containingFile

        if (InjectedLanguageManager.getInstance(project).isInjectedFragment(psiFile)) return psiFile

        val identifierOwner = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java)
        return identifierOwner ?: element
    }

}

