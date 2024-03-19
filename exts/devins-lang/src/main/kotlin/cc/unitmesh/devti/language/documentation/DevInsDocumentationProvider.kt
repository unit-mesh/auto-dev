package cc.unitmesh.devti.language.documentation

import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.psi.PsiElement

class DevInsDocumentationProvider : DocumentationProvider {
    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        return super.getQuickNavigateInfo(element, originalElement)
    }
}
