package cc.unitmesh.devti.devins.provider

import cc.unitmesh.devti.devins.provider.complex.ComplexitySink
import cc.unitmesh.devti.devins.provider.complex.ComplexityVisitor
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiElement

interface ComplexityProvider {
    fun process(element: PsiElement): Int

    fun visitor(sink: ComplexitySink): ComplexityVisitor

    companion object {
        private val languageExtension = LanguageExtension<ComplexityProvider>("cc.unitmesh.shireComplexityProvider")

        fun provide(language: Language): ComplexityProvider? {
            return languageExtension.forLanguage(language)
        }
    }
}
