package cc.unitmesh.language.parser

import cc.unitmesh.language.psi.DevInTypes
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.ElementManipulators
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.impl.source.tree.injected.InjectionBackgroundSuppressor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType

class CodeBlockElement(node: ASTNode) : ASTWrapperPsiElement(node), PsiLanguageInjectionHost,
    InjectionBackgroundSuppressor {
    override fun isValidHost(): Boolean {
        return true
    }

    override fun updateText(text: String): PsiLanguageInjectionHost {
        return ElementManipulators.handleContentChange(this, text)
    }

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> {
        return LiteralTextEscaper.createSimple(this)
    }

    fun getLanguageId(): PsiElement? {
        return findChildByType(DevInTypes.LANGUAGE_ID)
    }

    fun getContents(): List<PsiElement> {
        val codeContents = children.filter { it.elementType == DevInTypes.CODE_CONTENTS }.firstOrNull() ?: return emptyList()

        val psiElements = PsiTreeUtil.collectElements(codeContents) {
            it.elementType == DevInTypes.CODE_CONTENT
        }.toMutableList()

        return psiElements.toList()
    }
}