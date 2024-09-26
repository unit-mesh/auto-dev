package cc.unitmesh.devti.context.base;

import com.intellij.psi.PsiElement

interface LLMCodeContextProvider<T : PsiElement?> {
    fun from(psiElement: T): LLMCodeContext?
}

