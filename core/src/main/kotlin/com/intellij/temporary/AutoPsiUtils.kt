// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.temporary

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase


object AutoPsiUtils {
    private fun getStartOffset(element: PsiElement): Int = element.textRange.startOffset
    private fun getEndOffset(element: PsiElement): Int = element.textRange.endOffset

    fun getLineStartOffset(psiFile: PsiFile, line: Int): Int? {
        var document = psiFile.viewProvider.document
        if (document == null) {
            document = PsiDocumentManager.getInstance(psiFile.project).getDocument(psiFile)
        }

        if (document == null) return null
        if (line < 0 || line >= document.lineCount) return null

        val startOffset = document.getLineStartOffset(line)
        val element = psiFile.findElementAt(startOffset) ?: return startOffset

        if (element !is PsiWhiteSpace && element !is PsiComment) {
            return startOffset
        }

        val skipSiblingsForward = PsiTreeUtil.skipSiblingsForward(
            element, PsiWhiteSpace::class.java, PsiComment::class.java
        )

        return if (skipSiblingsForward != null) getStartOffset(skipSiblingsForward) else startOffset
    }

    fun getLineNumber(element: PsiElement, start: Boolean): Int {
        var document = element.containingFile.viewProvider.document
        if (document == null) {
            document = PsiDocumentManager.getInstance(element.project).getDocument(element.containingFile)
        }

        if (document == null) return 0

        val index = if (start) getStartOffset(element) else getEndOffset(element)
        if (index > document.textLength) {
            return 0
        }

        return document.getLineNumber(index)
    }

    /**
     * Adds line numbers to each line of the input string.
     *
     * @param string The input string to which line numbers will be added.
     * @return The modified string with line numbers added to each line.
     */
    fun addLineNumbers(string: String): String {
        return string.lines().mapIndexed { index, line -> "${index + 1} $line" }.joinToString("\n")
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