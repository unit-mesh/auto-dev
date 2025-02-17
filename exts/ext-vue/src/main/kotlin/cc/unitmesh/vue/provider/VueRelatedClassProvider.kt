package cc.unitmesh.vue.provider

import cc.unitmesh.devti.provider.RelatedClassesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.vuejs.lang.html.VueLanguage

/**
 * AutoDev Modular for Vue
 */
class VueRelatedClassProvider : RelatedClassesProvider {
    override fun lookup(element: PsiElement): MutableList<PsiElement> {
        if (element.language !is VueLanguage) return mutableListOf<PsiElement>()

        return mutableListOf<PsiElement>()
    }

    override fun lookup(element: PsiFile): MutableList<PsiElement> {
        return mutableListOf<PsiElement>()
    }
}
