package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement

class ClassContextProvider(private val gatherUsages: Boolean) {
    private val languageExtension = LanguageExtension<ClassContextBuilder>("cc.unitmesh.classContextBuilder")
    private val providers: List<ClassContextBuilder>

    init {
        val registeredLanguages = Language.getRegisteredLanguages()
        providers = registeredLanguages.mapNotNull(languageExtension::forLanguage)
    }

    companion object {
        val logger = Logger.getInstance(ClassContextProvider::class.java)
    }

    fun from(psiElement: PsiElement): ClassContext {
        for (provider in providers) {
            provider.getClassContext(psiElement, gatherUsages)?.let {
                return it
            }
        }

        return ClassContext(psiElement, null, null)
    }
}
