package cc.unitmesh.cpp.util

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.cidr.lang.psi.OCFunctionDeclaration
import com.jetbrains.cidr.lang.psi.OCDeclaration
import com.jetbrains.cidr.lang.psi.OCStruct
import com.jetbrains.cidr.lang.psi.OCStructLike
import com.jetbrains.cidr.lang.psi.OCConceptDeclaration
import com.jetbrains.cidr.lang.psi.OCTemplateArgumentList
import com.jetbrains.cidr.lang.psi.OCTemplateParameterList
import com.jetbrains.cidr.lang.symbols.OCResolveContext

object CppContextPrettify {
    /**
     * Extracts the structure text from the given OCStruct object.
     *
     * @param struct the OCStruct object from which to extract the structure text
     * @return the extracted structure text as a string
     */
    fun printStructure(struct: OCStruct): String {
        val builder = StringBuilder()
        findDeclAndExtractCommentWithTemplateLists(struct, builder)
        builder.append("struct").append(' ').append(struct.name).append(" {\n")

        struct.members.forEach { member ->
            val declaration = member as? OCFunctionDeclaration ?: member
            if (declaration is OCFunctionDeclaration) {
                extractDeclCommentAndTemplateLists(declaration, builder)
                val signature = declaration.symbol?.getSignature(struct.project)
                builder.append("$signature;\n")
            } else if (declaration != null) {
                builder.append(declaration.text).append("\n")
            }
        }

        builder.append("}")
        return builder.toString()
    }

    /**
     * Prints the structure of the parent class of the given method.
     *
     * @param potentialMethod the method for which the parent structure needs to be printed
     * @return a string representation of the parent structure, or null if the parent class cannot be resolved
     */
    fun printParentStructure(potentialMethod: OCFunctionDeclaration): String? {
        val symbol = potentialMethod.symbol
        val classSymbol = symbol?.getResolvedOwner(OCResolveContext.forPsi(potentialMethod)) ?: return null

        val locateDefinition = classSymbol.locateDefinition(potentialMethod.project)
        val oCStruct = locateDefinition as? OCStruct ?: return null

        return printStructure(oCStruct)
    }

    private fun findDeclAndExtractCommentWithTemplateLists(
        struct: OCStruct,
        structTextBuilder: java.lang.StringBuilder
    ) {
        val parentPsiDecl = findParentOCDeclaration(struct as PsiElement) ?: return
        extractDeclCommentAndTemplateLists(parentPsiDecl, structTextBuilder)
    }

    private fun findParentOCDeclaration(globalContextElement: PsiElement): OCDeclaration? {
        return PsiTreeUtil.findFirstParent(globalContextElement) { parentElement ->
            parentElement is OCDeclaration
        } as? OCDeclaration
    }

    private fun extractDeclCommentAndTemplateLists(parentPsiDecl: OCDeclaration, builder: StringBuilder) {
        if (parentPsiDecl.firstChild is PsiComment) {
            builder.append(parentPsiDecl.firstChild.text).append("\n")
        }

        parentPsiDecl.children.filterIsInstance<OCTemplateParameterList>().firstOrNull()?.let {
            builder.append(it.text).append("\n")
        }

        parentPsiDecl.children.filterIsInstance<OCTemplateArgumentList>().firstOrNull()?.let {
            builder.append(it.text).append("\n")
        }
    }

    private fun locateTargetDeclarators(element: PsiElement): List<PsiNameIdentifierOwner> {
        val locateTargetDeclaration = PsiTreeUtil.findFirstParent(element) {
            (it is OCStructLike) || (it is OCFunctionDeclaration) || (it is OCConceptDeclaration)
        } ?: return emptyList()

        if (locateTargetDeclaration !is PsiNameIdentifierOwner) return emptyList()

        return listOf(locateTargetDeclaration)
    }
}