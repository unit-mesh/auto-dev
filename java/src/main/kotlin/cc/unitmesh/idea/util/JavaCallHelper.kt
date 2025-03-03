package cc.unitmesh.idea.util

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.util.*

object JavaCallHelper {
    /**
     * Finds all the methods called by the given method.
     *
     * @param method the method for which callees need to be found
     * @return a list of PsiMethod objects representing the methods called by the given method
     */
    fun findCallees(project: Project, method: PsiMethod): List<PsiMethod> {
        val calledMethods = mutableSetOf<PsiMethod>()
        method.accept(object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                calledMethods.add(expression.resolveMethod() ?: return)
            }
        })

        return calledMethods
            .filter {
                val containingClass = it.containingClass ?: return@filter false
                if (!ProjectScope.getProjectScope(project).contains(containingClass.containingFile.virtualFile)) {
                    return@filter false
                }

                true
            }
    }

    /**
     * Finds all the callers of a given method.
     *
     * @param method the method for which callers need to be found
     * @return a list of PsiMethod objects representing the callers of the given method
     */
    fun findCallers(project: Project, method: PsiMethod): List<PsiMethod> {
        val callers: MutableList<PsiMethod> = ArrayList()

        ProgressManager.getInstance().runProcess(Runnable {
            val references = MethodReferencesSearch.search(method, method.useScope, true).findAll()
            for (reference in references) {
                PsiTreeUtil.getParentOfType(reference.element, PsiMethod::class.java)?.let {
                    callers.add(it)
                }
            }
        }, ProgressIndicatorBase())

        return callers.distinct()
    }
}
