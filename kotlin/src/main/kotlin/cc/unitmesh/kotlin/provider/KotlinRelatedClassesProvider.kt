package cc.unitmesh.kotlin.provider

import cc.unitmesh.devti.provider.RelatedClassesProvider
import cc.unitmesh.kotlin.util.KotlinTypeResolver
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class KotlinRelatedClassProvider : RelatedClassesProvider {
    override fun lookup(element: PsiElement): List<PsiElement> {
        return KotlinTypeResolver.resolveByElement(element).values.filterNotNull().toList()
    }

    override fun lookup(element: PsiFile): List<PsiElement> {
        return KotlinTypeResolver.resolveByElement(element).values.filterNotNull().toList()
    }
}
