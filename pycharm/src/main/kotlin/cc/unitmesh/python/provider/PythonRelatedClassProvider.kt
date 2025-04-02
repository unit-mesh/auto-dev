package cc.unitmesh.python.provider

import cc.unitmesh.devti.provider.RelatedClassesProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.jetbrains.python.codeInsight.PyPsiIndexUtil

class PythonRelatedClassProvider : RelatedClassesProvider {
    override fun lookupIO(element: PsiElement): List<PsiElement> {
        return emptyList()
    }

    override fun lookupIO(element: PsiFile): List<PsiElement> {
        return emptyList()
    }

    override fun lookupCaller(
        project: Project,
        element: PsiElement
    ): List<PsiNamedElement> {
        if(element is PsiNamedElement) {
            val findUsages = PyPsiIndexUtil.findUsages(element, false)
            return findUsages.mapNotNull { usageInfo ->
                when (usageInfo.element) {
                    is PsiNamedElement -> {
                        return@mapNotNull usageInfo.element as PsiNamedElement
                    }

                    else -> return@mapNotNull null
                }
            }
        }

        return emptyList()
    }
}
