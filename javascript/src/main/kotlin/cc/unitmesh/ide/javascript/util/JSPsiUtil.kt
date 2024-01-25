package cc.unitmesh.ide.javascript.util

import cc.unitmesh.devti.util.isInProject
import com.intellij.lang.ecmascript6.psi.ES6ExportDeclaration
import com.intellij.lang.ecmascript6.psi.ES6ExportDefaultAssignment
import com.intellij.lang.javascript.frameworks.commonjs.CommonJSUtil
import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.ecma6.TypeScriptGenericOrMappedTypeParameter
import com.intellij.lang.javascript.psi.ecmal4.JSAttributeList
import com.intellij.lang.javascript.psi.ecmal4.JSAttributeListOwner
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.lang.javascript.psi.ecmal4.JSQualifiedNamedElement
import com.intellij.lang.javascript.psi.resolve.JSResolveResult
import com.intellij.lang.javascript.psi.stubs.JSImplicitElement
import com.intellij.lang.javascript.psi.util.JSDestructuringUtil
import com.intellij.lang.javascript.psi.util.JSStubBasedPsiTreeUtil
import com.intellij.lang.javascript.psi.util.JSUtils
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parents

object JSPsiUtil {
    fun resolveReference(node: JSReferenceExpression, scope: PsiElement): PsiElement? {
        val resolveReference = JSResolveResult.resolveReference(node)
        var resolved = resolveReference.firstOrNull() as? JSImplicitElement

        if (resolved != null) {
            resolved = resolved.parent as? JSImplicitElement
        }

        if (resolved is JSFunction && resolved.isConstructor) {
            resolved = JSUtils.getMemberContainingClass(resolved) as? JSImplicitElement
        }

        if (resolved == null || skipDeclaration(resolved)) {
            return null
        }

        val virtualFile = resolved.containingFile?.virtualFile

        if (virtualFile == null ||
            !node.project.isInProject(virtualFile) ||
            ProjectFileIndex.getInstance(node.project).isInLibrary(virtualFile)
        ) {
            return JSStubBasedPsiTreeUtil.resolveReferenceLocally(node as PsiPolyVariantReference, node.referenceName)
        }

        val jSImplicitElement = resolved

        return if (jSImplicitElement.textLength == 0 || !PsiTreeUtil.isAncestor(scope, jSImplicitElement, true)) {
            jSImplicitElement
        } else {
            null
        }
    }

    private fun skipDeclaration(element: PsiElement): Boolean {
        return when (element) {
            is JSParameter, is TypeScriptGenericOrMappedTypeParameter -> true
            is JSField -> {
                element.initializerOrStub !is JSFunctionExpression
            }

            is JSVariable -> {
                var initializer = JSDestructuringUtil.getNearestDestructuringInitializer(element)
                if (initializer == null) {
                    initializer = element.initializerOrStub ?: return true
                }

                !(initializer is JSCallExpression
                        || initializer is JSFunctionExpression
                        || initializer is JSObjectLiteralExpression
                        )
            }

            else -> false
        }
    }


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
    private fun isPrivateMember(element: PsiElement): Boolean {
        if (element is JSQualifiedNamedElement && element.isPrivateName) {
            return true
        }

        if (element !is JSAttributeListOwner) return false

        val attributeList = element.attributeList
        return attributeList?.accessType == JSAttributeList.AccessType.PRIVATE
    }
}