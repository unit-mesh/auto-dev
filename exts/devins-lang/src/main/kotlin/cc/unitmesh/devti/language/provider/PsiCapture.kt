package cc.unitmesh.devti.language.provider

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension

interface PsiCapture {
    fun capture(fileContent: String, type: String): List<Any>

    companion object {
        private val languageExtension: LanguageExtension<PsiCapture> =
            LanguageExtension("com.phodal.shirePsiCapture")

        fun provide(language: Language): PsiCapture? {
            return languageExtension.forLanguage(language)
        }
    }
}