package cc.unitmesh.devti.language.completion.provider

import cc.unitmesh.devti.language.DevInIcons
import cc.unitmesh.devti.devin.dataprovider.ToolHubVariable
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
        ToolHubVariable.all().forEach { toolHub ->
            val elements = LookupElementBuilder.create(toolHub.hubName)
                .withIcon(DevInIcons.DEFAULT)
                .withTypeText("(${toolHub.description})", true)
                .withPresentableText(toolHub.hubName)
                .withTailText(toolHub.type, true)

            result.addElement(PrioritizedLookupElement.withPriority(elements, 0.0))
        }
    }
}
