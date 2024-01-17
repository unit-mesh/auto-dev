package cc.unitmesh.rust.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.rust.lang.core.psi.RsEnumItem
import org.rust.lang.core.psi.RsFunction
import org.rust.lang.core.psi.RsImplItem
import org.rust.lang.core.psi.RsStructItem
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement
import org.rust.lang.core.psi.ext.fields

class RustClassContextBuilder : ClassContextBuilder {
    override fun getClassContext(psiElement: PsiElement, gatherUsages: Boolean): ClassContext? {
        if (psiElement !is RsStructOrEnumItemElement) return null

        when (psiElement) {
            is RsStructItem -> {
                val fields: List<PsiElement> = psiElement.fields.map {
                    it
                }
                val impls = PsiTreeUtil.getChildrenOfTypeAsList(psiElement.containingFile, RsImplItem::class.java)
                val functions = impls.filter { it.name == psiElement.name }
                    .flatMap { PsiTreeUtil.getChildrenOfTypeAsList(it, RsFunction::class.java) }

                return ClassContext(
                    psiElement,
                    psiElement.text,
                    psiElement.name,
                    functions,
                    fields,
                    emptyList(),
                    emptyList(),
                    psiElement.name
                )
            }

            is RsEnumItem -> {
                // TODO: Implement
            }
        }

        return null
    }
}