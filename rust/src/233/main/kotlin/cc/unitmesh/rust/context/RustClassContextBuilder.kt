package cc.unitmesh.rust.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.rust.lang.core.psi.RsEnumItem
import org.rust.lang.core.psi.RsFunction
import org.rust.lang.core.psi.RsImplItem
import org.rust.lang.core.psi.RsMembers
import org.rust.lang.core.psi.RsStructItem
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement
import org.rust.lang.core.psi.ext.expandedFields
import org.rust.lang.core.psi.ext.implementingType
import org.rust.lang.core.types.asTy
import org.rust.lang.core.types.implLookup

class RustClassContextBuilder : ClassContextBuilder {
    override fun getClassContext(psiElement: PsiElement, gatherUsages: Boolean): ClassContext? {
        if (psiElement !is RsStructOrEnumItemElement && psiElement !is RsImplItem) return null

        when (psiElement) {
            is RsStructItem -> {
                val fields: List<PsiElement> = psiElement.expandedFields

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

            is RsImplItem -> {
                val structItem = psiElement.implementingType?.item ?: return null
                val functions = PsiTreeUtil.getChildrenOfTypeAsList(psiElement, RsMembers::class.java)
                    .flatMap { PsiTreeUtil.getChildrenOfTypeAsList(it, RsFunction::class.java) }

                val fields = when (structItem) {
                    is RsStructItem -> structItem.expandedFields
                    is RsEnumItem -> structItem.enumBody?.enumVariantList?.map { it } ?: emptyList()
                    else -> emptyList()
                }

                return ClassContext(
                    psiElement,
                    psiElement.text,
                    structItem.name,
                    functions,
                    fields,
                    emptyList(),
                    emptyList(),
                    psiElement.name
                )
            }

            is RsEnumItem -> {
                val fields = psiElement.enumBody?.enumVariantList?.map { it } ?: emptyList()
//                val impls = psiElement.implLookup.findImplsAndTraits(psiElement.asTy()).toList()
//                val methods = impls.flatMap {
//                    it.implementedTrait?.element?.children?.filterIsInstance<RsFunction>() ?: emptyList()
//                }

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
        }

        return null
    }
}