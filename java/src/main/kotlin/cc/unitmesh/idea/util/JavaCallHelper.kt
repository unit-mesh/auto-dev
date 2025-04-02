package cc.unitmesh.idea.util

import com.intellij.find.FindManager
import com.intellij.find.findUsages.JavaClassFindUsagesOptions
import com.intellij.find.impl.FindManagerImpl
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import com.intellij.util.CommonProcessors
import com.intellij.util.Function

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
                val resolveMethod = runReadAction { expression.resolveMethod() }
                calledMethods.add(resolveMethod ?: return)
            }
        })

        var resolvedMethods: List<PsiMethod> = calledMethods.mapNotNull { psiMethod ->
            val containingClass = psiMethod.containingClass ?: return@mapNotNull null
            if (!ProjectScope.getProjectScope(project).contains(containingClass.containingFile.virtualFile)) {
                return@mapNotNull null
            }

            if (psiMethod.containingClass?.isInterface == true) {
                val implementations = ClassInheritorsSearch.search(containingClass).findAll()
                return implementations.map { implementation ->
                    implementation.findMethodsBySignature(psiMethod, true).toList()
                }.flatten()
            }

            return@mapNotNull listOf<PsiMethod>(psiMethod)
        }.flatten()

        return resolvedMethods
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

    fun findCallers(project: Project, clazz: PsiClass): List<PsiMethod> {
        val options = JavaClassFindUsagesOptions(project)
        val processor = CommonProcessors.CollectProcessor<UsageInfo?>()
        val findUsagesManager = (FindManager.getInstance(project) as FindManagerImpl).findUsagesManager
        val handler = findUsagesManager.getFindUsagesHandler(clazz, true)

        val callers: MutableList<PsiMethod> = ArrayList()

        ProgressManager.getInstance().runProcess(Runnable {
            handler!!.processElementUsages(clazz, processor, options)

            val usages = processor.getResults()
            val text = StringUtil.join<UsageInfo?>(usages, Function { u: UsageInfo? -> u!!.element!!.text }, ",")

            println("Find usages: $text")
        }, ProgressIndicatorBase())

        return callers
    }
}

