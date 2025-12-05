package cc.unitmesh.ide.javascript.provider

import cc.unitmesh.devti.provider.RelatedClassesProvider
import cc.unitmesh.ide.javascript.util.JSTypeResolver
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.JSRecursiveWalkingElementVisitor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement

class JavaScriptRelatedClassProvider : RelatedClassesProvider {
    override fun lookupIO(element: PsiElement): List<PsiElement> = JSTypeResolver.resolveByElement(element)
    override fun lookupIO(element: PsiFile): List<PsiElement> = JSTypeResolver.resolveByElement(element)

    override fun lookupCallee(
        project: Project,
        element: PsiElement
    ): List<PsiNamedElement> {
        return when (element) {
            is JSFunction -> findCallees(project, element)
            else -> emptyList()
        }
    }

    fun findCallees(project: Project, method: JSFunction): List<JSFunction> {
        val calledMethods = mutableSetOf<JSFunction>()
        method.accept(object : JSRecursiveWalkingElementVisitor() {
            override fun visitJSCallExpression(node: JSCallExpression) {
//                if (node.isRequireCall) {
//                    val path = CommonJSUtil.getModulePathIfRequireCall(node)
//                    if (!path.isNullOrEmpty() && !JSFileReferencesUtil.isRelative(path)) {
//                       //
//                    }
//                }
                val psiRef = node.stubSafeMethodExpression?.reference ?: return
                val resolve = psiRef?.resolve() ?: return
                if (resolve is JSFunction) {
                    calledMethods.add(resolve)
                }
            }
        })

        return calledMethods.toList()
    }
}
