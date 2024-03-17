package cc.unitmesh.devti.provider.devins

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface DevInsCompletionProvider {

    /**
     * Lookup canonical name for different language
     */
    fun lookupSymbol(
        project: Project,
        parameters: CompletionParameters,
        result: CompletionResultSet
    ): Iterable<LookupElement>

    companion object {
        private val EP_NAME: ExtensionPointName<DevInsCompletionProvider> =
            ExtensionPointName("cc.unitmesh.customDevInsCompletionProvider")

        fun all(): List<DevInsCompletionProvider> {
            return EP_NAME.extensionList
        }
    }
}
