package cc.unitmesh.devti.language.completion.provider

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

class DatabaseFuncProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val databaseFunctions = listOf("schema", "table", "column", "query")
        databaseFunctions.forEach {
            val element = LookupElementBuilder.create(it)
            result.addElement(element)
        }
    }
}
