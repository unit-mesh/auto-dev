package cc.unitmesh.rust.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.intellij.psi.PsiElement
import org.rust.lang.core.psi.RsEnumItem
import org.rust.lang.core.psi.RsStructItem
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement
import org.rust.lang.core.psi.ext.fields

class RustClassContextBuilder : ClassContextBuilder {
    override fun getClassContext(psiElement: PsiElement, gatherUsages: Boolean): ClassContext? {
        if (psiElement !is RsStructOrEnumItemElement) return null
        when (psiElement) {
            is RsStructItem -> {
                val fields: List<PsiElement> = psiElement.fields.map {
                    it.typeReference?.reference?.resolve() ?: it
                }
                return ClassContext(
                    psiElement,
                    psiElement.text,
                    psiElement.name,
                    emptyList(),
                    fields,
                    emptyList(),
                    emptyList(),
                    psiElement.name
                )
            }

            is RsEnumItem -> {

            }
        }

        return null
    }
}