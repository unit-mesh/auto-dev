package cc.unitmesh.rust.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.intellij.psi.PsiElement
import org.rust.lang.core.psi.RsEnumItem
import org.rust.lang.core.psi.RsStructItem
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement

class RustClassContextBuilder : ClassContextBuilder {
    override fun getClassContext(psiElement: PsiElement, gatherUsages: Boolean): ClassContext? {
        if (psiElement !is RsStructOrEnumItemElement) return null
        when (psiElement) {
            is RsStructItem -> {
                // TODO: Implement
            }

            is RsEnumItem -> {

            }
        }

        return null
    }
}