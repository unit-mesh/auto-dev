package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.builder.MethodContextBuilder
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.ml.llm.context.MethodContext
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.NotNull

class MethodContextProvider(private val includeClassContext: Boolean, private val gatherUsages: Boolean) {
    @NotNull
    private val languageExtension: LanguageExtension<MethodContextBuilder> =
        LanguageExtension("cc.unitmesh.devti.methodContextBuilder")

    @NotNull
    private var providers: List<MethodContextBuilder> = emptyList()

    init {
        val registeredLanguages = Language.getRegisteredLanguages()
        providers = registeredLanguages.mapNotNull { languageExtension.forLanguage(it) }
    }

    @NotNull
    fun from(@NotNull psiElement: PsiElement): MethodContext {
        val iterator = providers.iterator()
        while (iterator.hasNext()) {
            val provider = iterator.next()
            val methodContext = provider.getMethodContext(psiElement, includeClassContext, gatherUsages)
            if (methodContext != null) {
                return methodContext
            }
        }

        return MethodContext(
            psiElement,
            psiElement.text,
            null,
            null,
            null,
            psiElement.language.displayName,
            null,
            emptyList(),
            false,
            emptyList(),
        )
    }
}
