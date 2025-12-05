package cc.unitmesh.devti.provider.devins

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

/**
 * The symbol provider for DevIns completion and execution
 * - Completion will be triggered by like `/symbol:`, and the symbol provider will provide the completion for the symbol.
 * - Execution will be triggered by like `/symbol:java.lang.String`, all load children level elements, like `java.lang.String#length()`
 *
 * For execution, see in [DevInsSymbolProvider.resolveSymbol]
 */
interface DevInsSymbolProvider {
    val language: String
    /**
     * Lookup canonical name for different language
     */
    fun lookupSymbol(
        project: Project,
        parameters: CompletionParameters,
        result: CompletionResultSet
    ): List<LookupElement>

    fun resolveElement(project: Project, symbol: String): List<PsiElement>

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
    fun resolveSymbol(project: Project, symbol: String): List<String>

    companion object {
        private val EP_NAME: ExtensionPointName<DevInsSymbolProvider> =
            ExtensionPointName("cc.unitmesh.customDevInsCompletionProvider")

        fun all(): List<DevInsSymbolProvider> {
            return EP_NAME.extensionList
        }
    }
}
