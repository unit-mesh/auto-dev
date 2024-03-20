package cc.unitmesh.devti.language

import cc.unitmesh.devti.language.compiler.model.FileFunc
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

class FileFunctionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        FileFunc.all().forEach {
            result.addElement(
                LookupElementBuilder.create(it.funcName)
                    .withIcon(it.icon)
                    .withTypeText(it.description, true)
            )
        }
    }
}
