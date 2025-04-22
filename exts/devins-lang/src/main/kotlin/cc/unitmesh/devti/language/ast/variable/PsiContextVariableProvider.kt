package cc.unitmesh.devti.language.ast.variable

import cc.unitmesh.devti.language.provider.ComplexityProvider
import cc.unitmesh.devti.language.provider.LanguageToolchainProvider
import cc.unitmesh.devti.language.provider.ToolchainPrepareContext
import cc.unitmesh.devti.provider.RevisionProvider
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture

/**
 * Resolve variables for code struct generation.
 * This is used to provide the variables that are used in the code struct generation.
 */
interface PsiContextVariableProvider : VariableProvider<PsiContextVariable> {
    /**
     * Calculate the value for the given variable based on the provided PsiElement.
     *
     * @param psiElement the PsiElement to use for resolving the variable value
     * @param variable the PsiVariable for which to calculate the value
     * @return the calculated value for the variable as a String
     */
    override fun resolve(variable: PsiContextVariable, project: Project, editor: Editor, psiElement: PsiElement?): Any


    fun collectFrameworkContext(psiElement: PsiElement?, project: Project): String {
        val future = CompletableFuture<String>()
        runBlocking {
            val prepareContext = ToolchainPrepareContext(psiElement?.containingFile, psiElement)
            val contextItems =
                LanguageToolchainProvider.collectToolchainContext(project, prepareContext)

            future.complete(contextItems.joinToString("\n") { it.text })
        }

        return future.get()
    }

    fun calculateChangeCount(psiElement: PsiElement?): String {
        return RevisionProvider.provide()?.countHistoryChange(psiElement?.project!!, psiElement).toString()
    }

    fun calculateLineCount(psiElement: PsiElement?): String {
        return psiElement?.containingFile?.text?.lines()?.size.toString()
    }

    fun calculateComplexityCount(psiElement: PsiElement?): String {
        if (psiElement?.language == null) {
            return "0"
        }

        return ComplexityProvider.provide(psiElement.language)?.process(psiElement).toString()
    }

    companion object {
        private val languageExtension: LanguageExtension<PsiContextVariableProvider> =
            LanguageExtension("com.phodal.shirePsiVariableProvider")

        fun provide(language: Language): PsiContextVariableProvider {
            return languageExtension.forLanguage(language) ?: DefaultPsiContextVariableProvider()
        }
    }
}
