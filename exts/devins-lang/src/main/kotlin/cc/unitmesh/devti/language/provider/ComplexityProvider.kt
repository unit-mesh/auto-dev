package cc.unitmesh.devti.language.provider

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiElement
import cc.unitmesh.devti.language.provider.complex.ComplexitySink
import cc.unitmesh.devti.language.provider.complex.ComplexityVisitor

interface ComplexityProvider {
    fun process(element: PsiElement): Int

    fun visitor(sink: ComplexitySink): ComplexityVisitor

    companion object {
        private val languageExtension = LanguageExtension<ComplexityProvider>("com.phodal.shireComplexityProvider")

        fun provide(language: Language): ComplexityProvider? {
            return languageExtension.forLanguage(language)
        }
    }
}
