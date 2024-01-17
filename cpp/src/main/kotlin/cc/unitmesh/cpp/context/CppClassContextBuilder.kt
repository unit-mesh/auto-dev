package cc.unitmesh.cpp.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.cidr.lang.psi.OCDeclaration
import com.jetbrains.cidr.lang.psi.OCFunctionDeclaration
import com.jetbrains.cidr.lang.psi.OCStructLike

/**
 * Builds a [ClassContext] for a C++ class.
 *
 * example:
 * ```
 * class Car {
 *   public:
 *
 *     // class data
 *     string brand, model;
 *     int mileage = 0;
 *
 *     // class function
 *     void drive(int distance) {
 *         mileage += distance;
 *     }
 * };
 * ```
 *
 * will be converted to: [ClassContext]
 */
class CppClassContextBuilder : ClassContextBuilder {
    override fun getClassContext(psiElement: PsiElement, gatherUsages: Boolean): ClassContext? {
        if (psiElement !is OCStructLike) {
            return null
        }

        val definition = psiElement.symbol?.locateDefinition(psiElement.project) ?: return null
        if (definition !is OCStructLike) return null

        val definitionSymbol = definition.symbol ?: return null

        val methods = PsiTreeUtil.getChildrenOfTypeAsList(definition, OCFunctionDeclaration::class.java)
        val declarations = PsiTreeUtil.getChildrenOfTypeAsList(definition, OCDeclaration::class.java)
        val fields = declarations.filter { it.declarators.isNotEmpty() }

        val supers = definitionSymbol.getBaseCppClasses(definition).map { it.name }

        return ClassContext(
            definition,
            definition.text,
            definition.name,
            methods,
            fields,
            supers,
            emptyList()
        )
    }
}
