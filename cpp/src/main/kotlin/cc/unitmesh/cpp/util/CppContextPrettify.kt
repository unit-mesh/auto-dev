package cc.unitmesh.cpp.util

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.cidr.lang.psi.*

object CppContextPrettify {
    fun extractStructureText(struct: OCStruct): String {
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

    private fun findDeclAndExtractCommentWithTemplateLists(
        struct: OCStruct,
        structTextBuilder: java.lang.StringBuilder
    ) {
        val parentPsiDecl = findParentOCDeclaration(struct as PsiElement)
        if (parentPsiDecl != null) {
            extractDeclCommentAndTemplateLists(parentPsiDecl, structTextBuilder)
        }
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

}