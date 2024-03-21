package cc.unitmesh.devti.language.completion.provider

import cc.unitmesh.devti.language.DevInIcons
import cc.unitmesh.devti.language.completion.dataprovider.ToolHub
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

class AgentToolOverviewCompletion : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        ToolHub.all().forEach { toolHub ->
            val elements = LookupElementBuilder.create(toolHub.summaryName)
                .withIcon(DevInIcons.DEFAULT)
                .withTypeText(toolHub.type, true)
                .withPresentableText(toolHub.summaryName)
                .withTailText(toolHub.description, true)
            result.addElement(PrioritizedLookupElement.withPriority(elements, 0.0))
        }
    }

}
