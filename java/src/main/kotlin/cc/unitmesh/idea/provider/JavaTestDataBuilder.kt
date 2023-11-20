package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.TestDataBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod

class JavaTestDataBuilder : TestDataBuilder {
    override fun inBoundData(element: PsiElement): Pair<String, String>? {
        if (element !is PsiMethod) return null

        return null
    }
}