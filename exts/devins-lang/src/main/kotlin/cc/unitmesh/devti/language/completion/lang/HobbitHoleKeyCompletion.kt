package cc.unitmesh.devti.language.completion.lang

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.language.ast.HobbitHole
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

class HobbitHoleKeyCompletion : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        HobbitHole.keys().forEach {
            val element = LookupElementBuilder.create(it.key)
                .withIcon(AutoDevIcons.AI_COPILOT)
                .withTypeText(it.value, true)

            result.addElement(PrioritizedLookupElement.withPriority(element, 0.0))
        }
    }

}
