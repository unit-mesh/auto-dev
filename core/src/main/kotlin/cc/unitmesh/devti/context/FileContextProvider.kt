package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.base.LLMCodeContextProvider
import cc.unitmesh.devti.context.builder.FileContextBuilder
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiFile

class FileContextProvider: LLMCodeContextProvider<PsiFile> {
    private val languageExtension: LanguageExtension<FileContextBuilder> =
        LanguageExtension("cc.unitmesh.fileContextBuilder")

    private val providers: List<FileContextBuilder>

    init {
        val registeredLanguages = Language.getRegisteredLanguages()
        providers = registeredLanguages.mapNotNull { languageExtension.forLanguage(it) }
    }

    override fun from(psiElement: PsiFile): FileContext? {
        for (provider in providers) {
            val fileContext = provider.getFileContext(psiElement)
            if (fileContext != null) {
                return fileContext
            }
        }

        return FileContext(psiElement, psiElement.name, psiElement.virtualFile?.path!!)
    }
}
