package cc.unitmesh.clion.oc.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil.getChildrenOfTypeAsList
import com.jetbrains.cidr.lang.psi.OCDeclaration
import com.jetbrains.cidr.lang.psi.OCFunctionDeclaration
import com.jetbrains.cidr.lang.psi.OCStructLike

class OcClassContextBuilder : ClassContextBuilder {
    override fun getClassContext(psiElement: PsiElement, gatherUsages: Boolean): ClassContext? {

        if (psiElement !is OCStructLike) return null

        val symbol = psiElement.symbol ?: return null
        val locateDefinition = symbol.locateDefinition(psiElement.project) ?: return null

        if (locateDefinition !is OCStructLike) return null

        val definitionSymbol = locateDefinition.symbol ?: return null
        val text = locateDefinition.text
        val name = locateDefinition.name!!

        val functionDecls: List<PsiElement> =
            getChildrenOfTypeAsList(locateDefinition, OCFunctionDeclaration::class.java)
        val decls: List<OCDeclaration> = getChildrenOfTypeAsList(locateDefinition, OCDeclaration::class.java)

        val fields: MutableList<PsiElement> = mutableListOf()
        for (decl in decls) {
            if (decl.declarators.isNotEmpty()) {
                fields.add(decl)
            }
        }

        val superClasses: List<String> = definitionSymbol.getBaseCppClasses(locateDefinition).map { it.name }

        return ClassContext(
            locateDefinition,
            text,
            name,
            functionDecls,
            fields,
            superClasses,
            emptyList()
        )
    }
}