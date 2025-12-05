package cc.unitmesh.devti.context.builder

import cc.unitmesh.devti.context.FileContext
import com.intellij.psi.PsiFile

/**
 * The FileContextBuilder interface provides a way to retrieve the file context for a given PsiFile.
 * A file context represents the context in which a file is being used or analyzed.
 */
interface FileContextBuilder {
    /**
     * Retrieves the file context for the given PsiFile.
     *
     * @param psiFile the PsiFile for which to retrieve the file context
     * @return the FileContext associated with the given PsiFile, or null if no file context is found
     */
    fun getFileContext(psiFile: PsiFile): FileContext?
}
