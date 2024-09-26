// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.temporary.error

import com.intellij.temporary.AutoPsiUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

data class ErrorPlace(
    val hyperlinkText: String,
    val lineNumber: Int,
    val isProjectFile: Boolean,
    val virtualFile: VirtualFile,
    val project: Project
) {
    val psiFile: PsiFile?
        get() = PsiManager.getInstance(project).findFile(virtualFile)

    val programText: String
        get() = VfsUtilCore.loadText(virtualFile)

    fun findContainingElement(): PsiElement? {
        val psiFile = psiFile ?: return null
        val lineStartOffset = AutoPsiUtils.getLineStartOffset(psiFile, lineNumber) ?: return null
        val errorPlaceOffset: Int = lineStartOffset
        return this.psiFile?.findElementAt(errorPlaceOffset)
    }

    fun getMarkDownLanguageSlug(): String? {
        val psiFile = psiFile ?: return null
        val language = psiFile.language

        return when (val displayNameLowercase = language.displayName.lowercase()) {
            "c#" -> "csharp"
            "c++" -> "cpp"
            else -> displayNameLowercase
        }
    }

}