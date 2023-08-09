package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.base.LLMQueryContextProvider
import cc.unitmesh.devti.context.builder.ClassContextBuilder
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement

class ClassContextProvider(private val gatherUsages: Boolean) : LLMQueryContextProvider<PsiElement> {
    private val languageExtension = LanguageExtension<ClassContextBuilder>("cc.unitmesh.classContextBuilder")
    private val providers: List<ClassContextBuilder>

    init {
        val registeredLanguages = Language.getRegisteredLanguages()
        providers = registeredLanguages.mapNotNull(languageExtension::forLanguage)
    }

    companion object {
        val logger = logger<ClassContextProvider>()
    }

    override fun from(psiElement: PsiElement): ClassContext {
        for (provider in providers) {
            provider.getClassContext(psiElement, gatherUsages)?.let {
                return it
            }
        }

        return ClassContext(psiElement, null, null)
    }
}
