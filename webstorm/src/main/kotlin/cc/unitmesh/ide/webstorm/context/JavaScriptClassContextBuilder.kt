package cc.unitmesh.ide.webstorm.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.psi.PsiElement
import com.intellij.openapi.diagnostic.logger

class JavaScriptClassContextBuilder : ClassContextBuilder {
    companion object {
        val logger = logger<JavaScriptClassContextBuilder>()
    }

    override fun getClassContext(psiElement: PsiElement, gatherUsages: Boolean): ClassContext? {
        if (psiElement !is JSFile) {
            return null
        }

        val text = psiElement.text
        val name = psiElement.name


        return null
    }
}