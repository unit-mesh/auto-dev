package cc.unitmesh.devti.provider.devins

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

interface DevInsCompletionProvider {

    /**
     * Lookup canonical name for different language
     */
    fun lookupSymbol(project: Project, parameters: CompletionParameters, element: PsiElement): Iterable<LookupElement>

    companion object {
        private val languageExtension: LanguageExtension<DevInsCompletionProvider> =
            LanguageExtension("cc.unitmesh.customDevInsCompletionProvider")

        fun forLanguage(language: Language): DevInsCompletionProvider? {
            return languageExtension.forLanguage(language)
        }
    }
}
