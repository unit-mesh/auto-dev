package cc.unitmesh.devti.provider.devins

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

/**
 * The symbol provider for DevIns completion and execution
 * - Completion will be triggered by like `/symbol:`, and the symbol provider will provide the completion for the symbol.
 * - Execution will be triggered by like `/symbol:java.lang.String`, all load children level elements, like `java.lang.String#length()`
 *
 * For execution:
 * - If parent is Root, the children will be packages
 * - If parent is Package, the children will be classes
 * - If parent is Class, the children will be methods and fields
 */
interface DevInsSymbolProvider {

    /**
     * Lookup canonical name for different language
     */
    fun lookupSymbol(
        project: Project,
        parameters: CompletionParameters,
        result: CompletionResultSet
    ): Iterable<LookupElement>

    companion object {
        private val EP_NAME: ExtensionPointName<DevInsSymbolProvider> =
            ExtensionPointName("cc.unitmesh.customDevInsCompletionProvider")

        fun all(): List<DevInsSymbolProvider> {
            return EP_NAME.extensionList
        }
    }
}
