package cc.unitmesh.devti.context.base

import com.intellij.psi.PsiElement

abstract class NamedElementContext(open val root: PsiElement, open val text: String?, open val name: String?) :
    LLMCodeContext {
    override fun format(): String = ""
}
