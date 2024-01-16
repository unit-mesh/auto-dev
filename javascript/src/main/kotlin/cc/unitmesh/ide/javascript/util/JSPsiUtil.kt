package cc.unitmesh.ide.javascript.util

import com.intellij.lang.ecmascript6.psi.ES6ExportDeclaration
import com.intellij.lang.ecmascript6.psi.ES6ExportDefaultAssignment
import com.intellij.lang.javascript.frameworks.commonjs.CommonJSUtil
import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.ecmal4.JSAttributeList
import com.intellij.lang.javascript.psi.ecmal4.JSAttributeListOwner
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.lang.javascript.psi.ecmal4.JSQualifiedNamedElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parents

object JSPsiUtil {
    fun isExportedFileFunction(element: PsiElement): Boolean {
        when (val parent = element.parent) {
            is JSFile, is JSEmbeddedContent -> {
                return when (element) {
                    is JSVarStatement -> {
                        val variables = element.variables
                        val variable = variables.firstOrNull() ?: return false
                        variable.initializerOrStub is JSFunction && exported(variable)
                    }

                    is JSFunction -> exported(element)
                    else -> false
                }
            }

            is JSVariable -> {
                val varStatement = parent.parent as? JSVarStatement ?: return false
                return varStatement.parent is JSFile && exported(parent)
            }

            else -> {
                return parent is ES6ExportDefaultAssignment
            }
        }
    }

    fun isExportedClass(elementForTests: PsiElement?): Boolean {
        return elementForTests is JSClass && elementForTests.isExported
    }

    fun isExportedClassPublicMethod(psiElement: PsiElement): Boolean {
        val jsClass = PsiTreeUtil.getParentOfType(psiElement, JSClass::class.java, true) ?: return false
        if (!exported(jsClass as PsiElement)) return false

        val parentElement = psiElement.parents(true).firstOrNull() ?: return false
        if (isPrivateMember(parentElement)) return false

        return when (parentElement) {
            is JSFunction -> !parentElement.isConstructor
            is JSVarStatement -> {
                val variables = parentElement.variables
                val jSVariable = variables.firstOrNull()
                (jSVariable?.initializerOrStub as? JSFunction) != null
            }

            else -> false
        }
    }

    private fun exported(element: PsiElement): Boolean {
        if (element !is JSElementBase) return false

        if (element.isExported || element.isExportedWithDefault) {
            return true
        }

        if (element is JSPsiElementBase && CommonJSUtil.isExportedWithModuleExports(element)) {
            return true
        }

        val containingFile = element.containingFile ?: return false
        val exportDeclarations =
            PsiTreeUtil.getChildrenOfTypeAsList(containingFile, ES6ExportDeclaration::class.java)

        return exportDeclarations.any { exportDeclaration ->
            exportDeclaration.exportSpecifiers
                .asSequence()
                .any { it.alias?.findAliasedElement() == element }
        }
    }

    fun elementName(psiElement: PsiElement): String? {
        if (psiElement !is JSVarStatement) {
            if (psiElement !is JSNamedElement) return null

            return psiElement.name
        }

        val jSVariable = psiElement.variables.firstOrNull() ?: return null
        return jSVariable.name
    }

    /**
     * Determines whether the given [element] is a private member.
     *
     * @param element the PSI element to check
     * @return `true` if the element is a private member, `false` otherwise
     */
    fun isPrivateMember(element: PsiElement): Boolean {
        if (element is JSQualifiedNamedElement && element.isPrivateName) {
            return true
        }

        if (element !is JSAttributeListOwner) return false

        val attributeList = element.attributeList
        return attributeList?.accessType == JSAttributeList.AccessType.PRIVATE
    }
}