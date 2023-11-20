package cc.unitmesh.devti.provider

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiElement

interface TestDataBuilder {
    fun inBoundData(element: PsiElement): Pair<String, String>? = null

    fun outBoundData(element: PsiElement): Pair<String, String>? = null

    companion object {
        private val languageExtension: LanguageExtension<TestDataBuilder> =
            LanguageExtension("cc.unitmesh.testDataBuilder")

        fun forLanguage(language: Language): TestDataBuilder? {
            return languageExtension.forLanguage(language)
        }
    }
}