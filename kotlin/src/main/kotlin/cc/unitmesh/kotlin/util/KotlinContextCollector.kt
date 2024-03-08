package cc.unitmesh.kotlin.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*

object KotlinContextCollector {
    /**
     * Resolves the given reference to a PSI element.
     *
     * @param reference the reference to resolve
     * @return the resolved PSI element, or null if the reference cannot be resolved
     */
    fun resolveReference(reference: PsiReference?): PsiElement? {
        val resolvedElement: PsiElement? = reference?.resolve()

        if (resolvedElement is KtPrimaryConstructor) {
            return PsiTreeUtil.getParentOfType(resolvedElement, KtClass::class.java, false)
        }

        if (resolvedElement == null) return null
        val virtualFile = resolvedElement.containingFile.virtualFile

        if (virtualFile != null && !ProjectFileIndex.getInstance(resolvedElement.project).isInLibrary(virtualFile)) {
            if (resolvedElement is KtClass || resolvedElement is KtFunction) {
                return resolvedElement
            }
        }

        return null
    }

    /**
     * Finds and returns a list of used variables within the given scope.
     *
     * @param scope the scope within which to search for used variables
     * @return a list of KtValVarKeywordOwner objects representing the used variables found within the scope
     */
    fun findUsedVariables(scope: PsiElement): List<KtValVarKeywordOwner> {
        val referenceExpressions = PsiTreeUtil.findChildrenOfType(scope, KtReferenceExpression::class.java)

        val resolvedReferences = referenceExpressions.mapNotNull {
            it.mainReference.resolve() as? KtValVarKeywordOwner
        }

        return resolvedReferences.toList()
    }

    /**
     * Returns the text representation of the return type of a Kotlin function.
     *
     * @param function The Kotlin function for which the return type text is to be retrieved.
     *
     * @return The text representation of the return type of the function, preceded by a colon (":").
     *         If the function does not have a return type, an empty string is returned.
     *
     * When given a class, this method does not return the code representation of the class.
     * Instead, it returns the comment representation of the class, enclosed in /** ... */.
     *
     * Note: This documentation does not include @author and @version tags.
     */
    fun returnTypeText(function: KtFunction?): String {
        val typeReference = function?.typeReference
        val typeText = typeReference?.getTypeText() ?: return ""
        return ": $typeText"
    }

    /**
     * Replaces all occurrences of a reference expression with a new name in the given element.
     *
     * @param element the element in which to replace the reference expressions
     * @param oldName the old name of the reference expression to be replaced
     * @param newName the new name to replace the reference expression with
     * @return a new PsiElement with the replaced reference expressions, or the original element if newName is null
     */
    private fun replaceReferenceExpressions(element: PsiElement, oldName: String, newName: String?): PsiElement {
        if (newName == null) return element

        val copiedElement = element.copy()
        val factory = PsiElementFactory.getInstance(element.project)

        PsiTreeUtil.findChildrenOfAnyType(copiedElement, false, KtReferenceExpression::class.java)
            .filterIsInstance<KtReferenceExpression>()
            .filter { it.text == oldName }
            .forEach { reference ->
                reference.replace(factory.createExpressionFromText(newName, reference.context))
            }

        return copiedElement
    }

    /**
     * Generates a summary for the given Kotlin language PsiElement.
     *
     * @param psiElement The PsiElement to generate the summary for.
     * @return The generated summary as a String, or null if the PsiElement is not applicable.
     *
     * If the PsiElement is a KtFile, it creates a copy of it and removes the function implementations.
     * Then it summarizes each class or object in the file.
     *
     * If the PsiElement is a KtClassOrObject, it creates a copy of it and summarizes the class or object.
     *
     * The generated summary is returned as a String.
     * If the PsiElement is not applicable, null is returned.
     */
    fun generateSummary(psiElement: PsiElement): String? {
        if (psiElement.language != KotlinLanguage.INSTANCE || psiElement is KtDecompiledFile) {
            return null
        }

        return when (psiElement) {
            is KtFile -> {
                val copy = psiElement.copy() as KtFile
                val project = copy.project
                val functions = PsiTreeUtil.getChildrenOfType(copy, KtFunction::class.java) ?: emptyArray()

                functions.forEach { function ->
                    removeFunctionImplementation(project, function)
                }

                val classesOrObjects = PsiTreeUtil.getChildrenOfType(copy, KtClassOrObject::class.java) ?: emptyArray()

                classesOrObjects.forEach { classOrObject ->
                    summarizeClassOrObject(project, classOrObject)
                }

                copy.text
            }
            is KtClassOrObject -> {
                val copy = psiElement.copy() as KtClassOrObject
                val project = copy.project
                summarizeClassOrObject(project, copy)
                copy.text
            }
            else -> null
        }
    }

    private fun summarizeClassOrObject(project: Project, item: KtClassOrObject) {
        for (declaration in item.declarations) {
            when (declaration) {
                is KtFunction -> {
                    removeFunctionImplementation(project, declaration)
                }
                is KtClassOrObject -> {
                    summarizeClassOrObject(project, declaration)
                }
            }
        }
    }

    private const val placeholderMessage = "/* implementation omitted for shortness */"
    /**
     * Removes the implementation of a function.
     *
     * @param project the project context
     * @param function the function to remove the implementation from
     *
     * This method removes the implementation of the given function by deleting the child range of the body expression.
     * If the length of the placeholder message is greater than or equal to the length of the body expression, no changes are made.
     * After removing the implementation, a placeholder is added as the first child of the body expression.
     */
    private fun removeFunctionImplementation(project: Project, function: KtFunction) {
        val bodyExpression = function.bodyExpression ?: return
        if (placeholderMessage.length >= bodyExpression.textLength) return

        bodyExpression.deleteChildRange(bodyExpression.firstChild, bodyExpression.lastChild)
        bodyExpression.addAfter(makePlaceholder(project), bodyExpression.firstChild)
    }

    private fun makePlaceholder(project: Project): PsiElement {
        return KtPsiFactory(project, false).createComment(placeholderMessage)
    }
}

fun KtNamedDeclaration.getReturnTypeReferences(): List<KtTypeReference> {
    return when (this) {
        is KtCallableDeclaration -> listOfNotNull(typeReference)
        is KtClassOrObject -> superTypeListEntries.mapNotNull { it.typeReference }
        is KtScript -> emptyList()
        else -> throw AssertionError("Unexpected declaration kind: $text")
    }
}


fun KtTypeReference?.getTypeText(): String? {
    if (this == null) return null

    val typeElement = this.typeElement
    if (typeElement is KtUserType) {
        val typeElementReference = typeElement.referenceExpression?.mainReference?.resolve()
        if (typeElementReference is KtClass) {
            return typeElementReference.name
        }
    }

    return this.text
}
