package cc.unitmesh.cpp.context

import cc.unitmesh.devti.context.MethodContext
import cc.unitmesh.devti.context.builder.MethodContextBuilder
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.lang.psi.*
import com.jetbrains.cidr.lang.symbols.cpp.OCSymbolWithQualifiedName


class CppMethodContextBuilder : MethodContextBuilder {
    override fun getMethodContext(
        psiElement: PsiElement,
        includeClassContext: Boolean,
        gatherUsages: Boolean
    ): MethodContext? {
        if (psiElement !is OCFunctionDeclaration) {
            return null
        }

        val symbol: OCSymbolWithQualifiedName = psiElement.symbol ?: return null
        val locateDefinition = symbol.locateDefinition(psiElement.project) ?: return null
        val function = (locateDefinition as? OCFunctionDefinition) ?: return null

        val structDeclaration = if (includeClassContext) (psiElement as? OCMethod)?.containingClass else null

        val returnType = function.returnType.name
        val parameterList: List<String> =
            function.parameters?.stream()?.map(OCDeclarator::getName)?.toList() ?: emptyList()

        return MethodContext(
            function,
            function.text,
            function.name!!,
            symbol.getSignature(psiElement.project),
            structDeclaration,
            "Cpp",
            returnType,
            parameterList,
            includeClassContext,
            emptyList()
        )
    }
}
