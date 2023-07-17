package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.builder.FileContextBuilder
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiFile

class FileContextProvider {
    private val languageExtension: LanguageExtension<FileContextBuilder> =
        LanguageExtension("cc.unitmesh.devti.fileContextBuilder")

    private val providers: List<FileContextBuilder>

    init {
        val registeredLanguages = Language.getRegisteredLanguages()
        providers = registeredLanguages.mapNotNull { languageExtension.forLanguage(it) }
    }

    fun from(psiFile: PsiFile): FileContext {
        for (provider in providers) {
            val fileContext = provider.getFileContext(psiFile)
            if (fileContext != null) {
                return fileContext
            }
        }

        return FileContext(psiFile, psiFile.name, psiFile.virtualFile?.path!!, null, listOf(), listOf(), listOf())
    }
}
