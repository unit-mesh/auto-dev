package cc.unitmesh.go.util

import com.goide.psi.GoFunctionOrMethodDeclaration
import com.goide.psi.GoTypeList
import com.goide.psi.GoTypeSpec
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiElement

object GoPsiUtil {
    fun findRelatedTypes(declaration: GoFunctionOrMethodDeclaration): List<GoTypeSpec> {
        val signature = declaration.signature ?: return emptyList()

        val parameterTypes = signature.parameters.parameterDeclarationList
            .mapNotNull { it.type }

        val resultTypes = when (val resultType = signature.resultType) {
            is GoTypeList -> resultType.typeList
            else -> listOf(resultType)
        }

        val mentionedTypes = parameterTypes + resultTypes

        val genericTypes = mentionedTypes
            .flatMap { it.typeArguments?.types ?: emptyList() }

        val relatedTypes = genericTypes + mentionedTypes

        return relatedTypes
            .mapNotNull { it.resolve(declaration) as? GoTypeSpec }
            .filter { isProjectContent(it) }
    }

    private fun isProjectContent(element: PsiElement): Boolean {
        val virtualFile = element.containingFile.virtualFile ?: return true
        return ProjectFileIndex.getInstance(element.project).isInContent(virtualFile)
    }
}