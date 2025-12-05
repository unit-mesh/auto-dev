package cc.unitmesh.go.util

import com.goide.psi.*
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiElement

object GoPsiUtil {
    fun getDeclarationName(psiElement: PsiElement): String? {
        return singleNamedDescendant(psiElement)?.name
    }

    /**
     * Returns the single named descendant of the given [element].
     *
     * @param element the PsiElement to find the single named descendant from
     * @return the single named descendant of the given [element], or null if there is none
     */
    fun singleNamedDescendant(element: PsiElement): GoNamedElement? {
        return when (element) {
            is GoNamedElement -> element
            is GoTypeDeclaration -> element.typeSpecList.singleOrNull()
            is GoVarOrConstSpec<*> -> element.definitionList.singleOrNull()
            is GoVarOrConstDeclaration<*> -> {
                (element.specList.singleOrNull() as? GoVarOrConstSpec)?.definitionList?.singleOrNull()
            }

            is GoImportDeclaration -> element.importSpecList.singleOrNull()
            else -> null
        }
    }

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