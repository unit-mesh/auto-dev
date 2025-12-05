package cc.unitmesh.devti.language.completion.provider

import cc.unitmesh.devti.provider.devins.DevInsSymbolProvider
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.util.ProcessingContext

class SymbolReferenceLanguageProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        DevInsSymbolProvider.all().forEach { completionProvider ->
            val elements = completionProvider.lookupSymbol(parameters.editor.project!!, parameters, result)
            elements.forEach {
                result.addElement(it)
            }
        }
    }

}
