package cc.unitmesh.devti.intentions.action

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase

object ElementSelectionForChat  {
    fun selectElement(elementToExplain: PsiElement, editor: Editor) {
        val startOffset = elementToExplain.textRange.startOffset
        val endOffset = elementToExplain.textRange.endOffset

        editor.selectionModel.setSelection(startOffset, endOffset)
    }

    fun getCurrentSelectionAsRange(editor: Editor): TextRange {
        val currentCaret = editor.caretModel.currentCaret
        return TextRange(currentCaret.selectionStart, currentCaret.selectionEnd)
    }
}

/**
 * Returns the PsiElement to explain in the given project and editor.
 *
 * @param project the project in which the element resides (nullable)
 * @param editor the editor in which the element is located (nullable)
 * @return the PsiElement to explain, or null if either the project or editor is null, or if no element is found
 */
fun getElementToAction(project: Project?, editor: Editor?): PsiElement? {
    if (project == null || editor == null) return null

    val element = PsiUtilBase.getElementAtCaret(editor) ?: return null
    val psiFile = element.containingFile

    if (InjectedLanguageManager.getInstance(project).isInjectedFragment(psiFile)) return psiFile

    val identifierOwner = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java)
    return identifierOwner ?: element
}

fun calculateFrontendElementToExplain(project: Project?, psiFile: PsiFile, range: TextRange): PsiElement? {
    if (project == null || !psiFile.isValid) return null

    val element = PsiUtilBase.getElementAtOffset(psiFile, range.startOffset)
    if (InjectedLanguageManager.getInstance(project).isInjectedFragment(psiFile)) {
        return psiFile
    }

    val injected = InjectedLanguageManager.getInstance(project).findInjectedElementAt(psiFile, range.startOffset)
    if (injected != null) {
        return injected.containingFile
    }

    val psiElement: PsiElement? = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java)
    return psiElement ?: element
}