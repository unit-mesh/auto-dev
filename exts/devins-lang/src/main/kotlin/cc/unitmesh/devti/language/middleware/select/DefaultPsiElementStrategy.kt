package cc.unitmesh.devti.language.middleware.select

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase

open class DefaultPsiElementStrategy : PsiElementStrategy {
    /**
     * Returns the PsiElement to explain in the given project and editor.
     *
     * @param project the project in which the element resides (nullable)
     * @param editor the editor in which the element is located (nullable)
     * @return the PsiElement to explain, or null if either the project or editor is null, or if no element is found
     */
    override fun getElementToAction(project: Project?, editor: Editor?): PsiElement? {
        if (project == null || editor == null) return null

        val element = PsiUtilBase.getElementAtCaret(editor) ?: return null
        val psiFile = element.containingFile

        val selectionModel = editor.selectionModel
        if (selectionModel.hasSelection()) {
            val startOffset = selectionModel.selectionStart
            val endOffset = selectionModel.selectionEnd

            val startElement = PsiUtilBase.getElementAtOffset(psiFile, startOffset)
            val endElement = PsiUtilBase.getElementAtOffset(psiFile, endOffset)

            if (startElement == endElement) return startElement
        }

        if (InjectedLanguageManager.getInstance(project).isInjectedFragment(psiFile)) return psiFile

        val identifierOwner = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java)
        return identifierOwner ?: element
    }

    /**
     * This method calculates the frontend element to explain based on the given project, PsiFile, and TextRange.
     *
     * @param project the project to which the PsiFile belongs
     * @param psiFile the PsiFile in which the frontend element is located
     * @param range the TextRange specifying the range of the frontend element
     * @return the PsiElement representing the frontend element to explain, or null if the project is null, or the PsiFile is invalid
     */
    override fun getElementToAction(project: Project?, psiFile: PsiFile, range: TextRange): PsiElement? {
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
}