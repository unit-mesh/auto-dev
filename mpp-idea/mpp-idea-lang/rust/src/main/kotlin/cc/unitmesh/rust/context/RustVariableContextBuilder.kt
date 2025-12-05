package cc.unitmesh.rust.context

import cc.unitmesh.devti.context.VariableContext
import cc.unitmesh.devti.context.builder.VariableContextBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.rust.lang.core.psi.RsFunction
import org.rust.lang.core.psi.RsImplItem
import org.rust.lang.core.psi.RsLetDecl
import org.rust.lang.core.psi.RsNamedFieldDecl

class RustVariableContextBuilder : VariableContextBuilder {
    override fun getVariableContext(
        psiElement: PsiElement,
        withMethodContext: Boolean,
        withClassContext: Boolean,
        gatherUsages: Boolean
    ): VariableContext? {
        if (psiElement !is RsNamedFieldDecl) return null

        val text = psiElement.text
        val parentOfType = PsiTreeUtil.getParentOfType(psiElement, RsFunction::class.java, true)
        val containingClass = PsiTreeUtil.getParentOfType(psiElement, RsImplItem::class.java, true)

        return VariableContext(
            psiElement,
            text,
            psiElement.name,
            parentOfType,
            containingClass,
            emptyList(),
            withClassContext
        )
    }

}
