package cc.unitmesh.devti.language.completion

import cc.unitmesh.devti.custom.compile.CustomVariable
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

class CustomVariableCompletion: CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        CustomVariable.all().forEach {
            val withTypeText = LookupElementBuilder.create(it.variable).withTypeText(it.description, true)
            result.addElement(withTypeText)
        }
    }
}
