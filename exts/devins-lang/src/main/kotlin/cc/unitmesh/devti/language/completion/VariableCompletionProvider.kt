package cc.unitmesh.devti.language.completion

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.language.ast.variable.CompositeVariableProvider
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

class VariableCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        CompositeVariableProvider.all().forEach {
            val withTypeText =
                PrioritizedLookupElement.withPriority(
                    LookupElementBuilder.create(it.name)
                        .withIcon(AutoDevIcons.VARIABLE)
                        .withTypeText(it.description, true),
                    it.priority
                )
            result.addElement(withTypeText)
        }
    }
}
