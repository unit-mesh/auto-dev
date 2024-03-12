package cc.unitmesh.language.parser

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.impl.source.tree.injected.InjectionBackgroundSuppressor

class CodeBlockElement(node: ASTNode) : ASTWrapperPsiElement(node), PsiLanguageInjectionHost,
    InjectionBackgroundSuppressor {
    override fun isValidHost(): Boolean {
        TODO("Not yet implemented")
    }

    override fun updateText(text: String): PsiLanguageInjectionHost {
        TODO("Not yet implemented")
    }

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> {
        TODO("Not yet implemented")
    }

}