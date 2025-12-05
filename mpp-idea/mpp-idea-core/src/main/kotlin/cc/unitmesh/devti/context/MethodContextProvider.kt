package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.base.LLMCodeContextProvider
import cc.unitmesh.devti.context.builder.MethodContextBuilder
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiElement

class MethodContextProvider(private val includeClassContext: Boolean, private val gatherUsages: Boolean):
    LLMCodeContextProvider<PsiElement> {
    private val languageExtension: LanguageExtension<MethodContextBuilder> =
        LanguageExtension("cc.unitmesh.methodContextBuilder")

    private var providers: List<MethodContextBuilder> = emptyList()

    init {
        val registeredLanguages = Language.getRegisteredLanguages()
        providers = registeredLanguages.mapNotNull(languageExtension::forLanguage)
    }

    override fun from(psiElement: PsiElement): MethodContext {
        val iterator = providers.iterator()
        while (iterator.hasNext()) {
            val provider = iterator.next()
            val methodContext = provider.getMethodContext(psiElement, includeClassContext, gatherUsages)
            if (methodContext != null) {
                return methodContext
            }
        }

        return MethodContext(psiElement, psiElement.text, null)
    }
}
