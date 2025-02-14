package cc.unitmesh.devti.language.completion.provider

import cc.unitmesh.devti.devin.dataprovider.BuiltinRefactorCommand
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

class RefactoringFuncProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        BuiltinRefactorCommand.all().forEach {
            val element = LookupElementBuilder.create(it.funcName)
                .withTypeText(it.description, true)

            result.addElement(element)
        }
    }
}
