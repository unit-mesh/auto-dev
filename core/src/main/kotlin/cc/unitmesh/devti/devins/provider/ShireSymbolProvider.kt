package cc.unitmesh.devti.devins.provider

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement

/**
 * The symbol provider for Shire completion and execution
 * - Completion will be triggered by like `/symbol:`, and the symbol provider will provide the completion for the symbol.
 * - Execution will be triggered by like `/symbol:java.lang.String`, all load children level elements, like `java.lang.String#length()`
 *
 * For execution, see in [ShireSymbolProvider.resolveSymbol]
 */
interface ShireSymbolProvider {
    val language: String

    /**
     * Lookup canonical name for different language
     */
    fun lookupSymbol(
        project: Project,
        parameters: CompletionParameters,
        result: CompletionResultSet,
    ): List<LookupElement>

    fun lookupElementByName(project: Project, name: String): List<PsiElement>?

    /**
     * Resolves the symbol for different programming languages.
     * For example, in Java:
     * - If the parent is Root, the children will be packages
     * - If the parent is Package, the children will be classes
     * - If the parent is Class, the children will be methods and fields
     *
     * Format: `java.lang.String#length`, means:
     * - `<package>.<class>#<method>`
     * - `<package>.<class>#<field>`
     * - `<package>.<class>#<constructor>`
     *
     */
    fun resolveSymbol(project: Project, symbol: String): List<PsiNamedElement>

    companion object {
        private val EP_NAME: ExtensionPointName<ShireSymbolProvider> =
            ExtensionPointName("cc.unitmesh.shireSymbolProvider")

        fun all(): List<ShireSymbolProvider> {
            return EP_NAME.extensionList
        }
    }
}
