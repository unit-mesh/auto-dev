package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiElement

class ClassContextProvider(private val gatherUsages: Boolean) {
    private val languageExtension = LanguageExtension<ClassContextBuilder>("cc.unitmesh.classContextBuilder")
    private val providers: List<ClassContextBuilder>

    init {
        val registeredLanguages = Language.getRegisteredLanguages()
        providers = registeredLanguages.mapNotNull(languageExtension::forLanguage)
    }

    fun from(psiElement: PsiElement): ClassContext {
        for (provider in providers) {
            val classContext = provider.getClassContext(psiElement, gatherUsages)
            if (classContext != null) {
                return classContext
            }
        }

        return ClassContext(
            psiElement,
            null,
            null,
            listOf(),
            listOf(),
            null,
            listOf(),
        )
    }
}
